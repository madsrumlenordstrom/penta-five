package mdu
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import java.util.concurrent.ThreadLocalRandom
import utility.Constants._
import fp.FPTestUtility._

import java.lang.Float.{floatToIntBits, intBitsToFloat}
class DivideWRound extends Module{
  val io = IO(new Bundle{
    val a,b = Input(SInt(32.W))
    val en, valid = Input(Bool())
    val op = Input(UInt(5.W))
    val div = Output(UInt(32.W))
    val rem = Output(UInt(32.W))
    val y = Output(SInt(32.W))
    val ready = Output(Bool())
  })

}


class DivideSpec extends AnyFlatSpec with ChiselScalatestTester {
  "Divide" should "pass" in {
    test(new Divide).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Functions to be used
      def input(a: Long, b: Long): Unit = {
        dut.io.valid.poke(true.B)
        dut.io.a.poke(a.S)
        dut.io.b.poke(b.S)
      }
      def stepDelay(op: UInt):Unit={
        val delay = op match {
          case DIVOP.INT.DIVUBYTE  => 10
          case DIVOP.INT.DIVBYTE   => 11
          case DIVOP.INT.DIVUHWORD => 18
          case DIVOP.INT.DIVHWORD  => 19
          case DIVOP.INT.DIVUWORD  => 34
          case DIVOP.INT.DIVWORD   => 35
          case DIVOP.INT.REMUBYTE  => 10
          case DIVOP.INT.REMBYTE   => 11
          case DIVOP.INT.REMUHWORD => 18
          case DIVOP.INT.REMHWORD  => 19
          case DIVOP.INT.REMUWORD  => 34
          case DIVOP.INT.REMWORD   => 35
          case DIVOP.FLOAT.DIV     => 50
          case _                   => 0
        }
        dut.clock.step() // Step once to load
        dut.io.valid.poke(false.B)
        while(!dut.io.ready.peek.litToBoolean){dut.clock.step()}
      }
      def equalBits(a: Int, b: Int):Boolean={
        for(i<-0 to 31){
          if(((a >>> i) & 0x00000001) != ((b >>> i) & 0x00000001)){
            return false
          }
        }
        true
      }
      def limits(j: Int, k: Int): Tuple2[Long, Long]={
        var lim : Tuple2[Long, Long] = (0, 0)
        lim = k match{
          case 0 => if(j == 1){ (-128L, 127L) }else{ (0L, 255L) }
          case 1 => if(j == 1){ (-32768L, 32767L) }else{ (0L, 65535L) }
          case 2 => if(j == 1){ (-2147483648L, 2147483647L) }else{ (0L, 4294967295L) }
        }
        lim
      }
      def printAttributes(a: Long, b: Long, op: UInt): Unit ={
        println("Op was: " + op.litValue.toInt.toBinaryString)
        println("a = " + a.toHexString + ", b = " + b.toHexString +  ", a/b = " + (a/b).toHexString + ", a%b = " + (a%b).toHexString)
        println("y = " + dut.io.y.peek.litValue.toInt.toHexString)
        println("---------------------------------------------------")
      }
      def printFields(x: Int): Unit={
        val s = getBits(x, 31, 31).toBinaryString
        val e = getBits(x, 30, 23).toBinaryString
        val m = getBits(x, 22, 0).toBinaryString
        println("s = " + s + ", e = " + e + ", m = " + m)
      }
      val seed = 0xDEADBEEF
      val rounds = 30
      val rFloat = new scala.util.Random(seed)
      val rInt = ThreadLocalRandom.current()
      //rInt.setSeed(seed)
      // Testing integer operations
      dut.io.en.poke(true.B)
      var op :UInt = 0.U(5.W)
      for(i <- 0 to 0){ // Divide or remainder
        for(j <- 0 to 1){ // Signed or unsigned
          for(k <- 0 to 2){ // Input width
            val (min, max) = limits(j, k)
            op = ((i << 1) | (k << 3) | j).U(5.W)
            println("Div: " + i, " Sign: " + j, " Width: " + k)
            dut.io.op.poke(op)
            for(l <- 0 until rounds){ // Test operation
              val a = rInt.nextLong(min, max)
              var b = rInt.nextLong(min, max)
              while(b == 0){
                b = rInt.nextLong(min, max)
              }
              input(a, b)
              stepDelay(op)
              val expected_res = if(i == 0){ (a / b).toInt }else{ (a % b).toInt }
              val actual_res = dut.io.y.peek().litValue.toInt
              val success = equalBits(expected_res, actual_res)
              if(!success){
                printAttributes(a, b, op)
                dut.clock.step(5)
                assert(success)
              }
            }
          }
        }
      }
      // Testing floating point operation
      op = DIVOP.FLOAT.DIV
      dut.io.op.poke(op)
      for(i <- 0 until rounds){
        val a = rFloat.nextFloat()
        var b = rFloat.nextFloat()
        while(b == 0){
          b = rFloat.nextFloat()
        }
        input(floatToIntBits(a).toLong,floatToIntBits(b).toLong)
        stepDelay(op)
        dut.clock.step(2)
        val expected_res = a/b
        val actual_res = dut.io.y.peek().litValue.toInt
        val success = equalBits(floatToIntBits(expected_res), actual_res)
        if(!success){
          println("Expected:")
          printFields(floatToIntBits(expected_res))
          println("Acutal:")
          printFields(actual_res)
        }
        assert(success)
      }
    }
  }
}
