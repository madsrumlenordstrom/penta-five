package memory


import chiseltest._
import chisel3._
import chisel3.experimental.DataMirror
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.util._
import utility.Functions.connect

import scala.collection.immutable
/*
class ICacheSim(bSize: Int, lines: Int, memSize: Int, maxBSize: Int) extends Module{
  val io = IO(new Bundle{
    val fatal, valid = Output(Bool())
    val addr = Input(UInt(log2Up(memSize).W))
    val dout = Output(UInt(32.W))
  })

  val icache = Module(new ICache(bSize, lines, memSize, maxBSize))
  val ram = Module(new SynchronousRAM(memSize))
  val ramctrl = Module(new MemoryControllerWithDCache(1, memSize, maxBSize))
  connect(ramctrl.io.ram.elements, ram.io.elements)
  connect(ramctrl.io.clients(0).elements, icache.io.ram.elements)
  connect(io.elements, icache.io.core.elements)
}

class ICacheSpec extends AnyFlatSpec with ChiselScalatestTester{
  val bSize = 8
  val lines = 2
  val memSize = 64
  val maxBSize = bSize
  "Instruction cache should pass" should "pass" in {
    test(new ICacheSim(bSize, lines, memSize, maxBSize)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      var pc = 0
      dut.clock.setTimeout(1000)
      dut.io.addr.poke(pc.U)
      dut.clock.step(10)
      for(i <- 0 to 10){
        if(dut.io.peek.valid.litToBoolean){
          pc+=4
          dut.io.addr.poke(pc.U)
        }
        dut.clock.step(1)
      }
    }
  }
}

 */