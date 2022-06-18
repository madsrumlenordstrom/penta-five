package fp

import chisel3._
import chisel3.util._
import utility.Constants._

class FPConvert extends Module{
  val io = IO(new Bundle{
    val a = Input(UInt(32.W))
    val y = Output(UInt(36.W))
    val op = Input(UInt(2.W))
    val en = Input(Bool())
  })

  /*
    Behaviour and limits
    /---------------------------------------\------------\------------\
    |                                       |   FCVT.W.S |  FCVT.WU.S |
    |-----------------------------------------------------------------|
    |Minimum valid input (after rounding)   |    -2^{31} |          0 |
    |Maximum valid input (after rounding)   | 2^{31} - 1 | 2^{32} - 1 |
    |-----------------------------------------------------------------|
    |Output for out-of-range negative input |    -2^{31} | 0          |
    |Output for -∞                          |    -2^{31} | 0          |
    |Output for out-of-range positive input | 2^{31} - 1 | 2^{32} - 1 |
    |Output for +∞ or NaN                   | 2^{31} - 1 | 2^{32} - 1 |
    \---------------------------------------/------------/------------/
   */
  val e_in, m_in, intToFloatShifts, floatToIntShifts, m_out,e_out, op, y = RegInit(0.U)
  val s_in, s_out, outOfRangeUnsigned, outOfRangeSigned, isNaN, isInf = RegInit(false.B)
  val x = RegInit(0.U(32.W))
  val yNext = WireDefault(0.S)


  when(io.en){
    op := io.op
    e_in := io.a(30,23)
    m_in := 1.U ## io.a(22, 0)
    s_in := io.a(31).asBool
    x := Mux(!io.op(1) & s_in, (~io.a).asUInt + 1.U, io.a.asUInt)
    intToFloatShifts := PriorityEncoder(Reverse(x))
    floatToIntShifts := e_in - 127.U
    m_out := (x << intToFloatShifts)(30,0)
    e_out := (158.U - intToFloatShifts)
    s_out := !io.op(1) & s_in
    isNaN := io.a.asUInt === NAN
    isInf := io.a.asUInt === INF // Ignore sign
    outOfRangeUnsigned := floatToIntShifts > 31.U // Shifting too much
    outOfRangeSigned := floatToIntShifts > 30.U // No space for sign
    y := yNext.asUInt
  }
  when(!op(0)){ // Float to int
    // Default assignment, just convert
    yNext := Mux(!op(1) & s_in.asBool, ((~(((m_in ## 0.U(8.W)) >> (31.U - floatToIntShifts)))).asUInt + 1.U).asSInt, (((m_in ## 0.U(8.W)) >> (31.U - floatToIntShifts))).asSInt)
    when(!op(1)){ // Float to signed int
      when((outOfRangeSigned | isInf).asBool){
        yNext := Mux(s_in.asBool, (-2147483648).S, 2147483647.S)
      } .elsewhen(isNaN.asBool){
        yNext := 2147483647.S
      }
    } . otherwise{ // Float to unsigned int
      when((outOfRangeUnsigned | isInf).asBool){
        yNext := Mux(s_in.asBool, 0.S, (4294967295L.U).asSInt)
      } .elsewhen(isNaN.asBool){
        yNext := (4294967295L.U).asSInt
      }
    }
  } .otherwise{ // Int to float
    yNext := (s_out ## e_out ## 1.U ## m_out(30, 6) ## (m_out(5,0) =/= 0.U)).asSInt
  }
  io.y := y
}