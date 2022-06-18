package fp

import chisel3._
import chisel3.util._
class Stage1Add extends Bundle{
  val e = UInt(8.W)
  val shifts = UInt(9.W)
  val ltb = Bool() // is a exponent less than b exponent
  val m = new Bundle{
    val a = UInt((27).W)
    val b = UInt((27).W)
  }

  val s = new Bundle{
    val a = Bool()
    val b = Bool()
  }
}
class Stage2Add extends Bundle{
  val e = UInt(8.W)
  val m = new Bundle{
    val a = UInt((28).W)
    val b = UInt((28).W)
  }
  val s = new Bundle{
    val a = Bool()
    val b = Bool()
  }
  val sticky = Bool()
}
class Stage3Add extends Bundle{
  val e = UInt(8.W)
  val m = UInt(28.W)
  val s = Bool()
  val sticky = Bool()
}
class Stage4Add extends Bundle{
  val e = UInt(8.W)
  val m = UInt(27.W)
  val s = Bool()
}
class FPAdd extends Module{
  val io = IO(new Bundle{
    val a, b = Input(UInt(32.W))
    val sub, en = Input(Bool())
    val y = Output(UInt(36.W))
  })
  //-------------------------------------------------------------//
  // Stage 1
  // -> Find exponent difference
  val e_a = io.a(30, 23)
  val m_a = io.a(22, 0)
  val e_b = io.b(30, 23)
  val m_b = io.b(22, 0)
  val s1 = RegInit(0.U.asTypeOf(new Stage1Add))
  val diff = Wire(SInt(9.W))
  diff := (e_a - e_b).asSInt
  when(io.en) {
    s1.shifts := (~(diff)).asUInt + 1.U
    s1.m.a := Mux(e_a === 0.U, 0.U ## m_a, 1.U ## m_a) ## 0.U(3.W) // Check if operand is denormalized and add extra bit for precision
    s1.m.b := Mux(e_b === 0.U, 0.U ## m_b, 1.U ## m_b) ## 0.U(3.W)
    s1.e := Mux(diff(8), e_b, e_a)
    s1.shifts := Mux(diff(8), (~(diff)).asUInt + 1.U, diff.asUInt)
    s1.ltb := diff(8)
    s1.s.a := io.a(31)
    s1.s.b := Mux(io.sub, ~io.b(31), io.b(31))
  }
  //-------------------------------------------------------------//
  // Stage 2
  // -> Shift mantissa
  val s2 = RegInit(0.U.asTypeOf(new Stage2Add))
  val stickyDist = PriorityEncoder(Mux(s1.ltb, s1.m.a, s1.m.b)) // Find distance to first 1 in the lesser mantissa
  // This distance must be less than or equal
  when(io.en) {
    s2.m.a := Mux(s1.ltb, s1.m.a >> s1.shifts, s1.m.a)
    s2.m.b := Mux(s1.ltb, s1.m.b, s1.m.b >> s1.shifts)
    s2.e := s1.e
    s2.s.a := s1.s.a
    s2.s.b := s1.s.b
    s2.sticky := (stickyDist <= s1.shifts) & (s1.shifts >= 3.U)
  }

  //-------------------------------------------------------------//
  // Stage 3
  // -> Add mantissas
  val s3 = RegInit(0.U.asTypeOf(new Stage3Add))
  val m_a_wide = WireDefault(0.S((29).W))
  val m_b_wide = WireDefault(0.S((29).W))
  m_a_wide := Mux(s2.s.a, (-s2.m.a).asSInt, (s2.m.a).asSInt)
  m_b_wide := Mux(s2.s.b, (-s2.m.b).asSInt, (s2.m.b).asSInt)
  val m_sum = WireDefault(0.S((30).W))
  m_sum := m_a_wide + m_b_wide
  when(io.en){
    s3.e := s2.e
    s3.m := Mux(m_sum(29),(-m_sum).asUInt ,m_sum.asUInt)
    s3.s := m_sum(29)
    s3.sticky := s2.sticky
  }

  //-------------------------------------------------------------//
  // Stage 4
  // -> Normalize
  val s4 = RegInit(0.U.asTypeOf(new Stage4Add))
  val m_norm = WireDefault(s3.m)
  val e_norm = WireDefault(s3.e)
  val zero = s3.m === 0.U
  val distUpper = PriorityEncoder(Reverse(s3.m)) // To determine how much we need to shift mantissa
  val distExpct = 1.U//(3-precision).U // First set bit should be 3 away from MSB of input.
  val shiftleft = distUpper > distExpct
  val shiftright = distUpper < distExpct
  val shifts = Mux(shiftleft,(distUpper - distExpct), (distExpct - distUpper)).asUInt
  val distLower = PriorityEncoder(s3.m) // Distance from sticky bit to first set bit of mantissa
  val setS = (distLower <= shifts) | s3.m(0) | s3.sticky
  when(!zero){
    when(shiftleft){
      m_norm := s3.m << shifts
      e_norm := s3.e - shifts
    } . elsewhen(shiftright){
      m_norm := s3.m >> shifts
      e_norm := s3.e + shifts
    }
  }
  when(zero){
    e_norm := 0.U
  }
  when(io.en){
    s4.m := m_norm
    s4.e := e_norm
    s4.s := s3.s
  }
  //-------------------------------------------------------------//
  // Stage 5
  // -> Output non-rounded result
  io.y := s4.s ## s4.e ## s4.m
}


