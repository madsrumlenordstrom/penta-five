package fp

import java.lang.Float.floatToIntBits
import java.lang.Float.intBitsToFloat
import FPConstants._

object FPTestUtility{
  def FPGetSign(n: Float): Int = {
    // Returns sign of a float
    val temp = floatToIntBits(n)
    (temp >> (FP_BITS - 1)) & 1
  }

  def FPGetExp(n: Float): Int = {
    // Returns exponent of float
    val temp = floatToIntBits(n)
    (temp >> (MAN_WIDTH)) & 0xFF // TODO make dependent on constant EXP_WIDTH
  }

  def FPGetMan(n: Float): Int = {
    // Returns mantissa of float
    val temp = floatToIntBits(n)
    temp & 0x7FFFFF // TODO make dependent on constants
  }

  def generateFP(s: Int, exp: Int, man: Int): Float ={
    var res = s << (MAN_WIDTH + EXP_WIDTH)
    res += exp << MAN_WIDTH
    res += man
    intBitsToFloat(res)
  }

  def printFPAsBin(n: Float): Unit ={
    println("Sign of " + n + " is " + FPGetSign(n).toBinaryString)
    println("Exp of " + n + " is " + FPGetExp(n).toBinaryString)
    println("Man of " + n + " is " + FPGetMan(n).toBinaryString)
  }

  def getBits(x: Int, end: Int, start: Int): Int ={
    // Returns a bit field from an integer
    ((x << (32-(end+1))) >>> (32-(end+1))) >>> start
  }
  def printFields(x: Int): Unit={
    // Print's the field
    val s = getBits(x, 31, 31).toBinaryString
    val e = getBits(x, 30, 23).toBinaryString
    val m = getBits(x, 22, 0).toBinaryString
    println("s = " + s + ", e = " + e + ", m = " + m)
  }
}