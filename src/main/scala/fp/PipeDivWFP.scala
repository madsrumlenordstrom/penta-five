package fp

import chisel3._
import chisel3.util._
import fp.FPRound
class DivideStage extends Module{
  val io = IO(new Bundle{
    val en = Input(Bool())
    val in = Input(new Bundle{
      val B = SInt(49.W) // Divisor
      val Q = SInt(49.W) // Quotient
      val R = SInt(49.W) // Remainder
      val e = UInt(8.W)
      val a_sign, b_sign, signed, rem = Bool()

    })
    val out = Output(new Bundle{
      val B = Input(SInt(49.W)) // Divisor
      val Q = Output(SInt(49.W)) // Quotient
      val R = Output(SInt(49.W)) // Remainder
      val a_sign, b_sign, signed, rem = Bool()
      val e = UInt(8.W)
    })
  })
  val nextR = Mux(io.in.R(48),(io.in.R(47, 0) ## io.in.Q(48)) + io.in.B.asUInt, (io.in.R(47, 0) ## io.in.Q(48)) - io.in.B.asUInt)
  val nextQ = io.in.Q(47, 0) ## Mux(nextR(48), 0.U, 1.U)

  val B, Q, R = RegInit(0.S(49.W))
  val a_sign, b_sign, signed, rem = RegInit(false.B)
  val e = RegInit(0.U(8.W))
  when(io.en){
    B := io.in.B
    Q := nextQ.asSInt
    R := nextR.asSInt
    a_sign := io.in.a_sign
    b_sign := io.in.b_sign
    signed := io.in.signed
    rem := io.in.rem
    e := io.in.e
  }
  io.out.B := B
  io.out.Q := Q
  io.out.R := R
  io.out.a_sign := a_sign
  io.out.b_sign := b_sign
  io.out.signed := signed
  io.out.rem := rem
  io.out.e := e

}
class PipeDivWFP extends Module {
  val io = IO(new Bundle{
    val a,b = Input(UInt(32.W))
    val en = Input(Bool())
    val op = Input(UInt(4.W))
    val y = Output(new Bundle{
      val float = UInt(36.W)
      val int = UInt(32.W)
    })
  })
  val floatOP = io.op(3)
  val signedOP = io.op(2)
  val remOP = io.op(1)
  val depth = 49

  // First stage -----------------------------------------------------------------------------------------------------//
  val B, Q, R = RegInit(0.S(49.W))
  val a_sign, b_sign, signed, rem = RegInit(false.B)
  val e = RegInit(0.U(8.W))
  val initQ = Mux(floatOP, 0.U ## (( 1.U ## io.a(22, 0)) << 23).asUInt, Mux(signedOP & io.a(31), (~io.a).asUInt + 1.U, io.a.asUInt))
  val initB = Mux(floatOP, 1.U ## io.b(22,0), Mux(signedOP & io.b(31), (~io.b).asUInt + 1.U, io.b.asUInt))

  R := 0.S
  when(io.en){
    B := initB.asSInt
    Q := initQ.asSInt
    e := (io.a(30, 23) - io.b(30, 23)) + 127.U
    a_sign := io.a(31)
    b_sign := io.b(31)
    signed := signedOP
    rem := remOP
  }

  // Middle stage ----------------------------------------------------------------------------------------------------//
  val stages = for(i <- 0 until depth) yield {
    val stage =  Module(new DivideStage)
    stage
  }
  stages(0).io.in.B := B
  stages(0).io.en := io.en
  stages(0).io.in.Q := Q
  stages(0).io.in.R := R
  stages(0).io.in.e := e
  stages(0).io.in.a_sign := a_sign
  stages(0).io.in.b_sign := b_sign
  stages(0).io.in.signed := signed
  stages(0).io.in.rem := rem
  for(i <- 1 until depth){
    stages(i).io.in := stages(i - 1).io.out
    stages(i).io.en := io.en
  }

  // Adjusting integer divide / remainder output ---------------------------------------------------------------------//
  val Q_unadjusted, R_unadjusted, Q_out, R_out = RegInit(0.U(32.W))
  val rem_unadjusted, rem_out = RegInit(false.B)
  val R_sign = RegInit(false.B)
  val Q_sign = RegInit(false.B)
  when(io.en){
    rem_unadjusted := stages(depth - 1).io.out.rem
    R_sign := stages(depth - 1).io.out.signed & stages(depth - 1).io.out.a_sign
    Q_sign := stages(depth - 1).io.out.signed & (stages(depth - 1).io.out.a_sign ^ stages(depth - 1).io.out.b_sign)
    Q_unadjusted := stages(depth - 1).io.out.Q.asUInt
    R_unadjusted := stages(depth - 1).io.out.R.asUInt + Mux(stages(depth - 1).io.out.R(48), stages(depth - 1).io.out.B.asUInt, 0.U)
  }
  when(io.en){
    rem_out := rem_unadjusted
    Q_out := Mux(Q_sign,
      (~Q_unadjusted).asUInt + 1.U, Q_unadjusted.asUInt)
    R_out := Mux(R_sign,
      (~R_unadjusted).asUInt + 1.U, R_unadjusted.asUInt)
  }
  io.y.int := Mux(rem_out, R_out, Q_out)

  // Floating point normalization ------------------------------------------------------------------------------------//
  val distMeas = RegInit(0.U)
  val distExpct = 1.U
  val m_unorm = RegInit(0.U(28.W))
  val m_norm = RegInit(0.U(27.W))
  val e_unorm = RegInit(0.U(8.W))
  val e_norm = RegInit(0.U(8.W))
  val sign_unorm = RegInit(false.B)
  val sign_norm = RegInit(false.B)
  val sticky = RegInit(false.B)
  when(io.en){
    m_unorm := Q_unadjusted(24, 0) ## R_unadjusted(24, 22)
    e_unorm := RegNext(stages(depth-1).io.out.e)
    sticky := R_unadjusted(21, 0) =/= 0.U
    sign_unorm := RegNext(stages(depth - 1).io.out.a_sign ^ stages(depth - 1).io.out.b_sign)
    sign_norm := sign_unorm
    distMeas := PriorityEncoder(Reverse(Q_unadjusted(24, 0) ## R_unadjusted(24, 22)))
    m_norm := m_unorm
    e_norm := e_unorm
    when(distMeas > distExpct){
      m_norm := (m_unorm << (distMeas - distExpct)).asUInt | sticky
      e_norm := e_unorm - (distMeas - distExpct)
    } . elsewhen(distMeas < distExpct){
      m_norm := (m_unorm >> (distExpct - distMeas)).asUInt | sticky
      e_norm := e_unorm + (distExpct - distMeas)
    }
  }
  io.y.float := (sign_norm ## e_norm ## m_norm).asUInt
}