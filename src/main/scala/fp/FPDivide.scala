package fp
import chisel3._
import chisel3.util._

class Stg1 extends Bundle {
  val e = UInt(8.W)
  val m = new Bundle {
    val a = UInt(24.W)
    val b = UInt(24.W)
  }
  val s = Bool()
}
class Stg2 extends Bundle{
  val e = UInt(8.W)
  val m = new Bundle {
    val a = UInt(48.W)
    val b = UInt(48.W)
  }
  val s = Bool()
}
class Stg6 extends Bundle{
  val e = UInt(8.W)
  val m = UInt(23.W)
  val s = Bool()
}

class FPDivide(depth: Int = 4, width: Int = 5) extends Module{
  val io = IO(new Bundle {
    val a, b = Input(SInt(32.W))
    val y = Output(SInt(32.W))
    val m_a, m_b = Output(UInt(23.W))
    val e_a, e_b = Output(UInt(8.W))
  })
  io.m_a := io.a(22, 0)
  io.m_b := io.b(22, 0)
  io.e_a := io.a(30, 23)
  io.e_b := io.b(30, 23)
  //------------------------------------------------------------//
  // Stage 1
  // Calculate sign and exponent
  val s1 = RegInit(0.U.asTypeOf(new Stg1))
  s1.s := io.a(31) ^ io.b(31)
  s1.e := (io.a(30, 23) - io.b(30, 23)) + 127.U
  s1.m.a := 1.U ## io.a(22, 0)
  s1.m.b := 1.U ## io.b(22, 0)

  //-----------------------------------------------------------//
  // Stage 2
  // Multiply with approximate 1/b:
  val s2 = RegInit(0.U.asTypeOf(new Stg2))
  val invLUT = Module(new InvLookUp(width))
  invLUT.io.mant := s1.m.b(22, 22 -(width-1))
  val bApprInv = invLUT.io.invMant
  s2.e := s1.e
  s2.s := s1.s
  s2.m.a := (s1.m.a * bApprInv)(47,23)
  s2.m.b := (s1.m.b * bApprInv)(47,23)

  val stages = RegInit(VecInit(Seq.fill(depth)(0.S.asTypeOf(new Stg2))))
  val end = depth - 1

  stages(0).e := s2.e
  stages(0).s := s2.s
  stages(0).m.a := (s2.m.a * (0x1000000.U - s2.m.b))(47,23)
  stages(0).m.b := (s2.m.b * (0x1000000.U - s2.m.b))(47,23)
  for(i <- 1 to end){
    stages(i).e := stages(i-1).e
    stages(i).s := stages(i-1).s
    stages(i).m.a := (stages(i-1).m.a * (0x1000000.U - stages(i-1).m.b))(47,23)
    stages(i).m.b := (stages(i-1).m.b * (0x1000000.U - stages(i-1).m.b))(47,23)
  }


  //-----------------------------------------------------------//
  // Stage 6
  // Normalize
  val s6 = RegInit(0.U.asTypeOf(new Stg6))
  s6.m := stages(end).m.a
  s6.s:= stages(end).s
  s6.e := stages(end).e
  val dist = PriorityEncoder(Reverse(stages(end).m.a))
  when(dist > 24.U){
    s6.m := stages(end).m.a << (dist - 24.U)
    s6.e := stages(end).e - (dist - 24.U)
  } . elsewhen(dist < 24.U){
    s6.m := stages(end).m.a >> (24.U - dist)
    s6.e := stages(end).e + (24.U - dist)
  }
  io.y := (s6.s ## s6.e ## s6.m).asSInt
}
