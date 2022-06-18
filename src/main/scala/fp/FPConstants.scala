package fp

import chisel3._

object FPConstants{

  // Constant for rounding mode
  val RNE = "b000".U(3.W) // Round to nearest ties to even
  val RTZ = "b001".U(3.W) // Round towards zero
  val RDN = "b010".U(3.W) // Round down towards -infinity
  val RUP = "b011".U(3.W) // Round up towards infinity
  val RMM = "b100".U(3.W) // Round to nearest ties to greatest magnitude
  val DYN = "b111".U(3.W) // Dynamic rounding mode selected by instruction

  // Values for 32 bit IEEE float
  val FP_BITS = 32
  val EXP_WIDTH = 8
  val MAN_WIDTH = 23
  val EXP_BIAS = 127

  class Float32 extends Bundle{
    //val isNaN = Bool()
    //val isInf = Bool()
    //val isZero = Bool()
    val sign = Bool()
    val exp = UInt(EXP_WIDTH.W)
    val man = UInt(MAN_WIDTH.W)
  }

  class Float32Unrounded extends Bundle{
    val sign = Bool()
    val exp = UInt(EXP_WIDTH.W)
    val man = UInt((MAN_WIDTH + 4).W) // Hidden fraction bit + mantissa + guard bit + round bit + sticky bit
  }
}