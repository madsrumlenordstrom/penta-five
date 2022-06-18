package fp

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import FPTestUtility._

class FPMultiplyTester extends AnyFlatSpec with ChiselScalatestTester{

  def ITERATIONS = 10
  def MIN: Float = -1000000.0F
  def MAX: Float = 1000000.0F

  "Float multiply" should "pass" in{
    test(new FPMultiply).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      val ran = scala.util.Random

      dut.io.en.poke(true.B)

      for (i <- 0 until ITERATIONS){
        val a = ran.between(MIN,MAX)
        val b = ran.between(MIN,MAX)
        println("Printing a:")
        printFPAsBin(a)
        println()
        println("Printing b:")
        printFPAsBin(b)
        println()
        println("Printing a*b")
        printFPAsBin(a*b)
        println()
        dut.io.a.sign.poke(FPGetSign(a).U)
        dut.io.a.exp.poke(FPGetExp(a).U)
        dut.io.a.man.poke(FPGetMan(a).U)
        dut.io.b.sign.poke(FPGetSign(b).U)
        dut.io.b.exp.poke(FPGetExp(b).U)
        dut.io.b.man.poke(FPGetMan(b).U)
        dut.clock.step(3) // TODO check if pipeline works
        println("Printing product from multiplier")
        println("As decimal " + a*b + " is " + generateFP(dut.io.res.sign.peek().litValue.toInt,dut.io.res.exp.peek().litValue.toInt,dut.io.res.man.peek().litValue.toInt))
        println("Sign of " + a*b + " is " + dut.io.res.sign.peek().litValue.toInt.toBinaryString)
        println("Exp of " + a*b + " is " + dut.io.res.exp.peek().litValue.toInt.toBinaryString)
        println("Man of " + a*b + " is " + dut.io.res.man.peek().litValue.toInt.toBinaryString)
        println()
        //dut.io.res.sign.expect(FPGetSign(a*b))
        //dut.io.res.exp.expect(FPGetExp(a*b))
        //dut.io.res.man.expect(FPGetMan(a*b))
      }
    }
  }
}
