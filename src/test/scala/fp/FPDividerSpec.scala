package fp

import FPTestUtility._
import chiseltest._
import chisel3._
import fp.FPDivide
import org.scalatest.flatspec.AnyFlatSpec
import org.scalactic.TimesOnInt.convertIntToRepeater

import scala.math.{abs, pow}
import java.lang.Float.{floatToIntBits, intBitsToFloat}



class FPDividerSpec extends AnyFlatSpec with ChiselScalatestTester {
  "FPDivider" should "pass" in {
    test(new FPDivide(4)).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      def input(a: Float, b: Float):Unit={
        dut.io.a.poke(floatToIntBits(a).S)
        dut.io.b.poke(floatToIntBits(b).S)
      }
      def output():Unit={
        println("Output = " + intBitsToFloat(dut.io.y.peek().litValue.toInt))
      }
      def step(x: Int = 1):Unit={
        dut.clock.step(x)
      }
      val r = new scala.util.Random(69)
      5 times{
        val a = r.nextFloat()
        val b = r.nextFloat()
        val expct = a/b
        println("-> Test ")
        println("a = " + a)
        println("b = " + b)
        println("a/b = " + expct)
        print("Fields expected:")
        printFields(floatToIntBits(a/b))
        input(a, b)
        step(20)
        val res = intBitsToFloat(dut.io.y.peek().litValue.toInt)
        println("Output = " + res)
        println("Error = " +  (res - expct))
        println("WITHIN MARGIN: " + ((res - expct) < 1E-5))
        print("Fields result:")
        printFields(floatToIntBits(res))
      }
    }
  }
}
