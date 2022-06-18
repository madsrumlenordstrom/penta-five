/*

package mdu
import chisel3._
import chiseltest._
import org.scalactic.TimesOnInt.convertIntToRepeater
import org.scalatest.flatspec.AnyFlatSpec
import java.util.concurrent.ThreadLocalRandom

class DividerSpec extends AnyFlatSpec with ChiselScalatestTester {
  val iterations = 30
  "Divider" should "pass" in {
    test(new Divider).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      def equalBits(a: Long, b: Long):Boolean={
        // Check's if first 32 bits of two long integer's are the same
        for(i<-0 to 31){
          if(((a >>> i) & 0x0000000000000001) != ((b >>> i) & 0x0000000000000001)){
            return false
          }
        }
        true
      }
      // Testing DIVU and REMU
      println("Testing unsigned division and unsigned modulus")
      println("...")
      dut.io.signed.poke(false.B)
      iterations times{
        var a = ThreadLocalRandom.current().nextLong(0,4294967295L)
        var b = ThreadLocalRandom.current().nextLong(0,4294967295L)
        dut.io.a.poke(a.S)
        dut.io.b.poke(b.S)
        dut.io.en.poke(true.B)
        dut.clock.step(1)
        var i = 0
        while(dut.io.ready.peek().litValue == 0){
          dut.clock.step()
          i = i + 1
        }
        dut.io.en.poke(false.B)
        val Q = dut.io.Q.peek().litValue.toInt.toLong
        val R = dut.io.R.peek().litValue.toInt.toLong
        val success = equalBits(a%b,R) && equalBits(a/b,Q)
        if(!success){
          println("Failed @")
          println(a + " / " + b + " = " + (a/b).toInt.toString + ", % = " + (a%b).toInt.toString)
          println("Q = " + Q.toString + ", R = " + R.toString)
        }
        assert(success)
      }
      println("Success")
      // Testing DIV and REM
      dut.io.signed.poke(true.B)
      println("Testing signed division and signed modulus")
      println("...")
      iterations times{
        var a = ThreadLocalRandom.current().nextLong(-2147483648, 2147483647)
        var b = ThreadLocalRandom.current().nextLong(-2147483648, 2147483647)
        dut.io.a.poke(a.S)
        dut.io.b.poke(b.S)
        dut.io.en.poke(true.B)
        dut.clock.step(1)
        var i = 0
        while(dut.io.ready.peek().litValue == 0){
          dut.clock.step()
          i = i + 1
        }
        dut.io.en.poke(false.B)
        val Q = dut.io.Q.peek().litValue.toInt.toLong
        val R = dut.io.R.peek().litValue.toInt.toLong
        val success = equalBits(a%b,R) && equalBits(a/b,Q)
        if(!success){
          println("Failed @")
          println(a + " / " + b + " = " + (a/b).toInt.toString + ", % = " + (a%b).toInt.toString)
          println("Q = " + Q.toString + ", R = " + R.toString)
        }
        assert(success)
      }
      println("Success")
    }
  }

}

 */
