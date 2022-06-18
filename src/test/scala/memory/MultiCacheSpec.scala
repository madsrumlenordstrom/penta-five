package memory

import chiseltest._
import chisel3._
import chisel3.experimental.DataMirror
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.util._
import utility.{DCacheCoreIO, ICacheCoreIO}
import utility.Functions.connect

import scala.collection.immutable
import scala.math

// Commented out
/*
class MultiCacheSim(bSizeD0: Int, bSizeD1: Int, bSizeI : Int, linesD0 : Int, linesD1: Int, linesI : Int, memSize: Int, maxBSize: Int) extends Module{
  val io = IO(new Bundle{
    val data = Vec(2, new DCacheCoreIO(memSize))
    val instr = new ICacheCoreIO(memSize)
  })
  val dcache0 = Module(new DCache(bSizeD0, linesD0, memSize, maxBSize))
  val dcache1 = Module(new DCache(bSizeD1, linesD1, memSize, maxBSize))
  val icache = Module(new ICache(bSizeI, linesI, memSize, maxBSize))
  val ram = Module(new SynchronousRAM(memSize))
  val ramctrl = Module(new MemoryControllerWithDCache(3, memSize, maxBSize))
  connect(ramctrl.io.ram.elements, ram.io.elements)
  connect(ramctrl.io.clients(2).elements, icache.io.ram.elements)
  connect(ramctrl.io.clients(0).elements, dcache0.io.ram.elements)
  connect(ramctrl.io.clients(1).elements, dcache1.io.ram.elements)
  connect(io.data(0).elements, dcache0.io.core.elements)
  connect(io.data(1).elements, dcache1.io.core.elements)
  connect(io.instr.elements, icache.io.core.elements)
}

class MultiCacheSpec extends AnyFlatSpec with ChiselScalatestTester{
  val bSizeD0 = 16
  val linesD0 = 8
  val bSizeD1 = 16
  val linesD1 = 8
  val bSizeI = 16
  val linesI = 8
  val memSize = 512
  val maxBSize = bSizeD1.max(bSizeD0.max(bSizeI))
  "Multi cache should pass" should "pass" in {
    test(new MultiCacheSim(bSizeD0, bSizeD1, bSizeI, linesD0, linesD1, linesI, memSize, maxBSize)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      def writeD(addr: Int, data: Int, memWidth: Int, i: Int): Unit ={
        dut.io.data(i).addr.poke(addr.U)
        dut.io.data(i).dout.poke(data.U)
        dut.io.data(i).memWidth.poke(memWidth.U)
        dut.io.data(i).valid.poke(true.B)
        dut.io.data(i).we.poke(true.B)
        while(!dut.io.data(i).ready.peek.litToBoolean){dut.clock.step()}
        dut.io.data(i).valid.poke(false.B)
        dut.io.data(i).we.poke(false.B)
        dut.clock.step()
      }
      def readD(addr: Int, data: Int, memWidth: Int, i: Int): Unit ={
        dut.io.data(i).addr.poke(addr.U)
        dut.io.data(i).valid.poke(true.B)
        dut.io.data(i).we.poke(false.B)
        dut.io.data(i).valid.poke(true.B)
        while(!dut.io.data(i).ready.peek.litToBoolean){dut.clock.step()}
        dut.io.data(i).valid.poke(false.B)
        val s = memWidth match{
          case 0 => 6*4
          case 1 => 4*4
          case _ => 0
        }
        val res = (dut.io.data(i).din.peek().litValue.toInt << s) >>> s
        println("Output is:" + dut.io.data(i).din.peek().litValue.toInt.toHexString)
        if(res != data){
          println("Expected: " + data.toHexString)
          println("Got: "+ dut.io.data(i).din.peek().litValue.toInt.toHexString)
        }
        assert(res == data)
        dut.clock.step(5)
      }
      dut.clock.setTimeout(100)
      writeD(0, 0x69, 0, 0)
      readD(0, 0x69, 0, 0)
      readD(0, 0x69, 0, 1)
      writeD(1, 0xAA, 0, 1)
      readD(1, 0xAA, 0, 1)
      writeD(8, 0x1337, 1, 0)
      writeD(12, 0x1312, 1, 0)
      readD(8, 0x1337, 1, 1)
      readD(12, 0x1312, 1, 0)
      dut.clock.step(40)
      dut.io.instr.addr.poke(8.U)
      dut.clock.step(2)
      while(!dut.io.instr.valid.peek.litToBoolean){dut.clock.step()}
      assert((dut.io.instr.dout.peek.litValue.toInt & 0x0000FFFF) == 0x1337)
      dut.io.instr.addr.poke(12.U)
      dut.clock.step(2)
      while(!dut.io.instr.valid.peek.litToBoolean){dut.clock.step()}
      assert((dut.io.instr.dout.peek.litValue.toInt & 0x0000FFFF) == 0x1312)
      readD(8, 0x1337, 1, 1)
      readD(12, 0x1312, 1, 1)
      readD(8, 0x1337, 1, 0)
      readD(12, 0x1312, 1, 0)
      writeD(8, 0x1111, 1, 1)
      readD(8, 0x1111, 1, 0)
    }
  }
}

 */