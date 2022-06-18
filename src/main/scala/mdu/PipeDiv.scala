package mdu
import chisel3._
import chisel3.util._
import fp.FPRound
class DivideStage(width: Int) extends Module{
  val io = IO(new Bundle{
    val en = Input(Bool())
    val in = Input(new Bundle{
      val B = SInt((width + 1).W) // Divisor
      val Q = SInt((width + 1).W) // Quotient
      val R = SInt((width + 1).W) // Remainder
      val a_sign, b_sign, signed, rem = Bool()
    })
    val out = Output(new Bundle{
      val B = Input(SInt((width + 1).W)) // Divisor
      val Q = Output(SInt((width + 1).W)) // Quotient
      val R = Output(SInt((width + 1).W)) // Remainder
      val a_sign, b_sign, signed, rem = Bool()
    })
  })
  val nextR = Mux(io.in.R(width),(io.in.R(width - 1, 0) ## io.in.Q(width)) + io.in.B.asUInt, (io.in.R(width - 1, 0) ## io.in.Q(width)) - io.in.B.asUInt)
  val nextQ = io.in.Q(width - 1, 0) ## Mux(nextR(width), 0.U, 1.U)

  val B, Q, R = RegInit(0.S((width + 1).W))
  val a_sign, b_sign, signed, rem = RegInit(false.B)
  when(io.en){
    B := io.in.B
    Q := nextQ.asSInt
    R := nextR.asSInt
    a_sign := io.in.a_sign
    b_sign := io.in.b_sign
    signed := io.in.signed
    rem := io.in.rem
  }
  io.out.B := B
  io.out.Q := Q
  io.out.R := R
  io.out.a_sign := a_sign
  io.out.b_sign := b_sign
  io.out.signed := signed
  io.out.rem := rem

}
class PipeDiv(width: Int = 48) extends Module {
  val io = IO(new Bundle{
    val a,b = Input(UInt(32.W))
    val en = Input(Bool())
    val op = Input(UInt(4.W))
    val y = Output(UInt(32.W))
  })
  val signedOP = io.op(2)
  val remOP = io.op(1)
  val depth = width + 1

  // First stage -----------------------------------------------------------------------------------------------------//
  val B, Q, R = RegInit(0.S((width + 1).W))
  val a_sign, b_sign, signed, rem = RegInit(false.B)
  val initQ = Mux(signedOP & io.a(31), (~io.a).asUInt + 1.U, io.a.asUInt)
  val initB = Mux(signedOP & io.b(31), (~io.b).asUInt + 1.U, io.b.asUInt)

  R := 0.S
  when(io.en){
    B := initB.asSInt
    Q := initQ.asSInt
    a_sign := io.a(31)
    b_sign := io.b(31)
    signed := signedOP
    rem := remOP
  }

  // Middle stage ----------------------------------------------------------------------------------------------------//
  val stages = for(i <- 0 until depth) yield {
    val stage =  Module(new DivideStage(width))
    stage
  }
  stages(0).io.in.B := B
  stages(0).io.en := io.en
  stages(0).io.in.Q := Q
  stages(0).io.in.R := R
  stages(0).io.in.a_sign := a_sign
  stages(0).io.in.b_sign := b_sign
  stages(0).io.in.signed := signed
  stages(0).io.in.rem := rem
  for(i <- 1 until depth){
    stages(i).io.in := stages(i - 1).io.out
    stages(i).io.en := io.en
  }

  // Adjusting integer divide / remainder output ---------------------------------------------------------------------//
  val Q_unadjusted, R_unadjusted, Q_out, R_out = RegInit(0.U(width.W))
  val rem_unadjusted, rem_out = RegInit(false.B)
  val R_sign = RegInit(false.B)
  val Q_sign = RegInit(false.B)
  when(io.en){
    rem_unadjusted := stages(depth - 1).io.out.rem
    R_sign := stages(depth - 1).io.out.signed & stages(depth - 1).io.out.a_sign
    Q_sign := stages(depth - 1).io.out.signed & (stages(depth - 1).io.out.a_sign ^ stages(depth - 1).io.out.b_sign)
    Q_unadjusted := stages(depth - 1).io.out.Q.asUInt
    R_unadjusted := stages(depth - 1).io.out.R.asUInt + Mux(stages(depth - 1).io.out.R(width), stages(depth - 1).io.out.B.asUInt, 0.U)
  }
  when(io.en){
    rem_out := rem_unadjusted
    Q_out := Mux(Q_sign,
      (~Q_unadjusted).asUInt + 1.U, Q_unadjusted.asUInt)
    R_out := Mux(R_sign,
      (~R_unadjusted).asUInt + 1.U, R_unadjusted.asUInt)
  }
  io.y := Mux(rem_out, R_out, Q_out)
}
