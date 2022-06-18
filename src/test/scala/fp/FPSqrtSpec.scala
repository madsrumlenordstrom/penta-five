package fp
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import fp.{FPSqrt,FPRound}
import fp.FPTestUtility._


import java.lang.Float.{floatToIntBits, intBitsToFloat}
class FPSqrtWRound extends Module{
  val io = IO(new Bundle{
    val a  = Input(SInt(32.W))
    val y = Output(SInt(32.W))
    val valid, en = Input(Bool())
    val ready = Output(Bool())
  })
  val squareRoot = Module(new FPSqrt)
  val rounder = Module(new FPRounder)
  squareRoot.io.a := io.a
  squareRoot.io.valid := io.valid
  squareRoot.io.en := io.en
  io.ready := squareRoot.io.ready
  rounder.io.en := io.en
  rounder.io.a := squareRoot.io.y
  rounder.io.rm := 0.U
  io.y := rounder.io.y.asSInt

}

class FPSqrtSpec extends AnyFlatSpec with ChiselScalatestTester {
  val rounds = 30
  "FPSqrt" should "pass" in {
    test(new FPSqrtWRound).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      def equalBits(a: Int, b: Int):Boolean={
        for(i<-0 to 31){
          if(((a >>> i) & 0x00000001) != ((b >>> i) & 0x00000001)){
            return false
          }
        }
        true
      }
      def printFields(x: Int): Unit={
        val s = getBits(x, 31, 31).toBinaryString
        val e = getBits(x, 30, 23).toBinaryString
        val m = getBits(x, 22, 0).toBinaryString
        println("s = " + s + ", e = " + e + ", m = " + m)
      }
      dut.io.en.poke(true.B)
      val r = new scala.util.Random()
      var cnt = 0
      for(i <- 0 until rounds){
        val a = r.nextFloat()*100
        val res = math.sqrt(a.toDouble).toFloat
        dut.io.valid.poke(true.B)
        dut.io.a.poke(floatToIntBits(a).S)
        dut.clock.step()
        while(!dut.io.ready.peek.litToBoolean){
          dut.clock.step()
        }
        dut.io.valid.poke(false.B)
        dut.clock.step() // Round result
        //println("Res: " + intBitsToFloat(dut.io.y.peek().litValue.toInt), " Exp: " + res )
        if((intBitsToFloat(dut.io.y.peek().litValue.toInt) - res) == 0F){cnt += 1}
      }
      println("Hit: " + cnt +  " / rounds")

    }
  }
}