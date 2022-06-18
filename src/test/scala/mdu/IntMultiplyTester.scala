package mdu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import MDUConstants._
import utility.Constants._
import utility.Controlsignals._


class IntMultiplyTester extends AnyFlatSpec with ChiselScalatestTester{

  def WIDTH = 32
  def ITERATIONS = 100

  "Bit serial multiply" should "pass" in {
    test(new BitSerialMultiply(WIDTH)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val ran = scala.util.Random

      //println("Testing lower")
      dut.io.upper.poke(false.B)
      val maxNumber: Long = scala.math.pow(2,WIDTH - 1).toLong
      //println("Max possible number: " + maxNumber)
      val maxMultiplier =  ((1 << (WIDTH/2 - 1)) - 1)
      //println("Max possible multipliers: " + maxMultiplier)
      for ( i <- 0 until ITERATIONS){
        val a = ran.nextInt(maxMultiplier).abs
        val b = ran.nextInt(maxMultiplier).abs
        val prod = a*b
        //println(a.toHexString + " * " + b.toHexString + " = " + prod.toHexString)
        dut.io.a.poke(a.U)
        dut.io.b.poke(b.U)
        dut.clock.step(WIDTH)
        dut.io.res.expect(prod.S)
      }

      //println("Testing upper")
      dut.io.upper.poke(true.B)
      for ( i <- 0 until ITERATIONS){
        val a = ran.nextLong(maxNumber).abs
        val b = ran.nextLong(maxNumber).abs
        //val a: Long = 0x60aa
        //val b: Long = 0x4447
        val prod = a*b
        //println(a.toHexString + " * " + b.toHexString + " = 0x" + prod.toHexString)
        //println("Result from multiplier should be: 0x" + ((prod>>WIDTH).toInt).toHexString)
        dut.io.a.poke(a.U)
        dut.io.b.poke(b.U)
        dut.clock.step(WIDTH)
        dut.io.upper.poke(true.B)
        dut.io.res.expect(((prod>>WIDTH).toInt).S)
        dut.io.upper.poke(false.B)
        dut.io.res.expect(((prod&0x00000000FFFFFFFF).toInt).S)
        dut.reset.poke(true.B)
        dut.clock.step(1)
        dut.reset.poke(false.B)
      }

      //println("Testing upper and lower")
      dut.io.upper.poke(false.B)
      for ( i <- 0 until ITERATIONS){
        val a = ran.nextLong(maxNumber).abs
        val b = ran.nextLong(maxNumber).abs
        val prod = a*b
        //println(a.toHexString + " * " + b.toHexString + " = 0x" + prod.toHexString)
        //println("Result from multiplier should be: 0x" + ((prod&0x00000000FFFFFFFF).toInt).toHexString)
        dut.io.a.poke(a.U)
        dut.io.b.poke(b.U)
        dut.clock.step(WIDTH)
        dut.io.res.expect(((prod&0x00000000FFFFFFFF).toInt).S)
        dut.reset.poke(true.B)
        dut.clock.step(1)
        dut.reset.poke(false.B)
      }
    }
  }

  "Pipeline integer Multiply" should "pass" in {
    test(new DigitSerialMultiply32).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>

        val ran = scala.util.Random
        val prod: Array[Int] = new Array[Int](ITERATIONS)

      // Lower test
        for ( i <- 0 until ITERATIONS){
            //println("Iteration: " + i)
            val a = ran.nextInt(32768).abs
            val b = ran.nextInt(32768).abs
            prod(i) = a*b
            //println("0x" + a.toHexString + " * " + "0x" + b.toHexString + " = 0x" + prod(i).toHexString)
            dut.io.upper.poke(false.B)
            dut.io.a.poke(a.U)
            dut.io.b.poke(b.U)
            // Wait for pipeline
            if (i > 2) {
                //println("Result of mul in iteration " + (i - 3) + ": 0x" + dut.io.res.peek().litValue.toInt.toHexString)
                dut.io.res.expect(prod(i - 3))
            }
            Console.out.flush()
            dut.clock.step(1)
        }
    }
  }

  "24 bit two stage multiply" should "pass" in {
    test(new DigitSerialMultiply24).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>

      val ran = scala.util.Random
      val prod: Array[Long] = new Array[Long](ITERATIONS)

      dut.io.en.poke(true.B)

      // Lower test
      for ( i <- 0 until ITERATIONS){
        //println("Iteration 24 bit: " + i)
        val a = ran.nextLong(8388607).abs
        val b = ran.nextLong(8388607).abs
        prod(i) = a*b
        //println("0x" + a.toHexString + " * " + "0x" + b.toHexString + " = 0x" + prod(i).toHexString)
        dut.io.a.poke(a.U)
        dut.io.b.poke(b.U)
        // Wait for pipeline
        if (i > 1) {
            //println("Result of mul in iteration " + (i - 1) + ": 0x" + dut.io.res.peek().litValue.toLong.toHexString)
            dut.io.res.expect(prod(i - 1))
        }
        Console.out.flush()
        dut.clock.step(1)
      }
    }
  }

  "Main integer multiply" should "pass" in{
    test(new IntMultiplier).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        val ran = scala.util.Random
        val maxLong: Long = 4294967295L

        val prodLong: Array[Long] = new Array[Long](ITERATIONS)

        dut.io.en.poke(true.B)
        
        println("Testing MUL")
        dut.io.op.poke(OP.MUL.value)

        for (i <- 0 until ITERATIONS){
          val a = ran.nextInt()
          val b = ran.nextInt()
          dut.io.a.poke(("b" + a.toBinaryString).U)
          dut.io.b.poke(("b" + b.toBinaryString).U)
          dut.clock.step(1)
          prodLong(i) = ((a.toLong)*(b.toLong)) & 0x00000000FFFFFFFFL
          if (i >= 2) {
            dut.io.res.expect(("b" + prodLong(i - 2).toBinaryString).U)
          }
        }
        
        println("Testing MULH")
        dut.io.op.poke(OP.MULH.value)

        for (i <- 0 until ITERATIONS){
          val a = ran.nextInt()
          val b = ran.nextInt()
          dut.io.a.poke(("b" + a.toBinaryString).U)
          dut.io.b.poke(("b" + b.toBinaryString).U)
          dut.clock.step(1)
          prodLong(i) = (a.toLong)*(b.toLong) >>> 32L
          if (i >= 2) {
            dut.io.res.expect(("b" + prodLong(i - 2).toBinaryString).U)
          }
        }

        println("Testing MULHSU")
        dut.io.op.poke(OP.MULHSU.value)

        for (i <- 0 until ITERATIONS){
          val a = ran.nextInt()
          val b = ran.nextLong(maxLong)
          dut.io.a.poke(("b" + a.toBinaryString).U)
          dut.io.b.poke(("b" + b.toBinaryString).U)
          dut.clock.step(1)
          prodLong(i) = (a.toLong)*b >>> 32L
          if (i >= 2) {
            dut.io.res.expect(("b" + prodLong(i - 2).toBinaryString).U)
          }
        }
                
        println("Testing MULHU")
        dut.io.op.poke(OP.MULHU.value)

        for (i <- 0 until ITERATIONS){
          val a = ran.nextLong(maxLong).abs
          val b = ran.nextLong(maxLong).abs
          dut.io.a.poke(("b" + a.toBinaryString).U)
          dut.io.b.poke(("b" + b.toBinaryString).U)
          dut.clock.step(1)
          prodLong(i) = a*b >>> 32L
          if (i >= 2) {
            dut.io.res.expect(("b" + prodLong(i - 2).toBinaryString).U)
          }
        }
        dut.clock.step(5) // Few extra steps to see last results
    }
  }
}
