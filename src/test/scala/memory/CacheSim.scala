/*
package memory

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Functions._

import java.util.concurrent.ThreadLocalRandom
import scala.util.Random

class MemControllerSim(bSize : Int, memSize: Int) extends Module{
  val io = IO(new RAMIO)
  val mem = RegInit(0.U.asTypeOf(Vec(memSize/4, UInt(32.W))))
  val cnt = RegInit(0.U(2.W))
  val idle :: delay :: Nil = Enum(2)
  val stateReg = RegInit(idle)
  io.dout := mem(io.addr)
  io.ready := stateReg === idle
  switch(stateReg){
    is(idle){
      when(io.valid){
        stateReg := delay
        when(io.write){
          mem(io.addr) := io.din
        }
      }
    }
    is(delay){
      cnt := cnt + 1.U
      when(cnt === 3.U){
        stateReg := idle
      }
    }
  }
}
class CacheWithMem(bSize : Int, lines : Int, memSize: Int) extends Module{
  val io = IO(Flipped(new CPU2CIO(bSize)))

  val mem = Module(new MemControllerSim(bSize, memSize))
  val cache = Module(new DCache(bSize, lines))
  connect(cache.io.cpu.elements, io.elements)
  connect(cache.io.mem.elements, mem.io.elements)
}


class CacheSim extends AnyFlatSpec with ChiselScalatestTester {
  val bSize = 8
  val lines = 32
  val memSize = 256
  val maxAddr = memSize - 1
  val iterations = 30
  val maxData = (math.pow(2,8*8) - 1).toLong
  "Cache simulation" should "pass" in {
    test(new CacheWithMem(bSize, lines, memSize)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      def step(steps: Int = 1): Unit = {
        dut.clock.step(steps)
      }
      def wait(): Unit={
        while(dut.io.peek.ready.litValue != 1){
          step()
        }
      }
      def write(addr: Int, data: Long, width: Int): Unit = {
        dut.io.valid.poke(true.B)
        dut.io.addr.poke(addr.U)
        dut.io.dout.poke(data.U)
        dut.io.memWidth.poke(width.U)
        dut.io.write.poke(true.B)
        step()
        wait()
        dut.io.valid.poke(false.B)
        dut.io.write.poke(false.B)
        step()
      }
      def read(addr: Int, data: Long, width: Int): Unit = {
        dut.io.valid.poke(true.B)
        dut.io.addr.poke(addr.U)
        dut.io.memWidth.poke(width.U)
        dut.io.write.poke(false.B)
        step() // Force one step for nicer waveforms
        wait()
        dut.io.din.expect(data.U)
        dut.io.valid.poke(false.B)
        step()
      }
      val r = new Random(0x1337BABE)
      val addr = Seq.fill(30)(r.nextInt(maxAddr + 1))
      val data = Seq.fill(30)(ThreadLocalRandom.current().nextLong(0, maxData))
      val memWidth = Seq.fill(30)(1 + r.nextInt(8 - 1 + 1 ))
      val memory = Array.ofDim[Byte](memSize)
      for(i <- 0 until iterations){
        write(addr(i), data(i) , memWidth(i))
        read(addr(i), ((data(i) << ((bSize - memWidth(i))*8)) >>> ((bSize - memWidth(i))*8)), memWidth(i))
      }
    }
  }
}

 */

