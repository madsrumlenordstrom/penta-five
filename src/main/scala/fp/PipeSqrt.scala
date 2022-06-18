package fp

import chisel3._
import chisel3.util._
class SqrtStage(N: Int) extends Module{
  val io = IO(new Bundle{
    val en = Input(Bool())
    val in = Input(new Bundle{
      val Q = UInt(47.W)
      val R = UInt(25.W)
      val D = UInt(47.W)
      val e = UInt(8.W)
    })
    val out = Output(new Bundle{
      val Q = UInt(47.W)
      val R = UInt(48.W)
      val D = UInt(47.W)
      val e = UInt(8.W)
    })
  })
  val D_in, D = RegInit(0.U(47.W))
  val e_in, e = RegInit(0.U(8.W))
  val R_in, R = RegInit(0.U(25.W))
  val Q_in, Q = RegInit(0.U(47.W))
  val nextR = (io.in.R << 2).asUInt | ((io.in.D >> (N.U ## 0.U).asUInt).asUInt & 3.U).asUInt
  when(io.en){
    //R_in := (io.in.R << 2).asUInt | ((io.in.D >> (N.U ## 0.U).asUInt).asUInt & 3.U).asUInt
    R_in := Mux(io.in.R(24), nextR + ((io.in.Q << 2).asUInt | 3.U), nextR - ((io.in.Q << 2).asUInt | 1.U))
    R := R_in
    Q_in := io.in.Q
    Q := (Q_in << 1).asUInt | ~R_in(24)
    D_in := io.in.D
    D := D_in
    e_in := io.in.e
    e := e_in
  }
  io.out.Q := Q
  io.out.R := R
  io.out.D := D
  io.out.e := e
}

class PipeSqrt extends Module{
  val io = IO(new Bundle{
    val a = Input(UInt(32.W))
    val en = Input(Bool())
    val y = Output(UInt(36.W))
  })

  val depth = 24
  // First stages
  val D_in, D = RegInit(0.U(47.W))
  val e_in, e = RegInit(0.U(8.W))
  when(io.en){
    D_in := 1.U ## io.a(22, 0) ## 0.U(23.W)
    D := Mux(!e_in(0), D_in, D_in >> 1)
    e_in := io.a(30, 23) - 127.U
    e := Mux(!e_in(0), 0.U ## (e_in >> 1).asUInt, 0.U ## ((e_in + 1.U) >> 1).asUInt) + 127.U
  }
  val stages = for(i <- 0 until depth) yield {
    val stage =  Module(new SqrtStage(depth - 1 - i))
    stage
  }
  stages(0).io.in.Q := 0.U
  stages(0).io.in.R := 0.U
  stages(0).io.in.D := D
  stages(0).io.in.e := e
  stages(0).io.en := io.en
  for(i <- 1 until depth){
    stages(i).io.in.Q := stages(i - 1).io.out.Q
    stages(i).io.in.R := stages(i - 1).io.out.R
    stages(i).io.in.D := stages(i - 1).io.out.D
    stages(i).io.in.e := stages(i - 1).io.out.e
    stages(i).io.en := io.en
  }
  // Final stage
  val R_fin = RegInit(0.U(25.W))
  val Q_fin = RegInit(0.U(25.W))

  when(io.en){
    R_fin := Mux(stages(depth - 1).io.out.R(24),
      stages(depth - 1).io.out.R + ((stages(depth - 1).io.out.Q << 1).asUInt | 1.U), stages(depth - 1).io.out.R)
    Q_fin := stages(depth - 1).io.out.Q(24, 0)
  }

  val distMeas = RegInit(0.U)
  val distExpct = 1.U
  val m_unorm = RegInit(0.U(28.W))
  val m_norm = RegInit(0.U(27.W))
  val e_unorm = RegInit(0.U(8.W))
  val e_norm = RegInit(0.U(8.W))
  val sticky = RegInit(false.B)
  when(io.en){
    m_unorm := Q_fin ## R_fin(24, 22)
    e_unorm := RegNext(stages(depth - 1).io.out.e)
    sticky := R_fin(21, 0) =/= 0.U
    distMeas := PriorityEncoder(Reverse(Q_fin ## R_fin(24, 22)))
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
  io.y := 0.U ## e_norm ## m_norm
}