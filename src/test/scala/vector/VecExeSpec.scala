/*package vector
import chisel3._
import chisel3.util.BitPat.bitPatToUInt
import chisel3.util.log2Up
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._
import memory._

import scala.collection.immutable.ListMap
import utility.Functions.connect
import utility.VecControlSignals._
class VecExeWMem(memSize: Int, maxDelay: Int, data: String) extends Module{
  val io = IO(new Bundle{
    val preExe = Input(new VecPreExeStage2)
    val en = Input(Bool())
    val done = Output(Bool())
    val addr = Output(UInt(log2Up(memSize).W)) // So stuff isn't optimized away
  })
  val ram = Module(new SyncRAM(memSize, data))
  val ctrl = Module(new MemoryController(1, memSize))
  val vecExe = Module(new VecExe(memSize, maxDelay))
  connect(ctrl.io.ram.elements, ram.io.elements)
  connect(ctrl.io.clients(0).elements, vecExe.io.ram.elements)
  io.addr := vecExe.io.ram.addr
  vecExe.io.preExe <> io.preExe
  vecExe.io.en := io.en
  io.done := vecExe.io.done
}

class VecExeSpec extends AnyFlatSpec with ChiselScalatestTester {
  val data = "data.txt"
  val memSize = 128
  val maxDelay = 53
  "VecExe" should "pass" in {
    test(new VecExeWMem(memSize, maxDelay, data)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      def input(vs1: UInt, vs2: UInt, vd: UInt, opcode: UInt, opa: UInt, opb: UInt, memRE: UInt, memWE: UInt, ew: UInt, regWE: UInt, vl: UInt, xs1: UInt, valid: Bool, lane: UInt, vm: Bool): Unit ={
        dut.io.preExe.vs1.poke(vs1)
        dut.io.preExe.vs2.poke(vs2)
        dut.io.preExe.vd.poke(vd)
        dut.io.preExe.opcode.poke(opcode)
        dut.io.preExe.opa.poke(opa)
        dut.io.preExe.opb.poke(opb)
        dut.io.preExe.memRE.poke(memRE)
        dut.io.preExe.memWE.poke(memWE)
        dut.io.preExe.ew.poke(ew)
        dut.io.preExe.regWE.poke(regWE)
        dut.io.preExe.vl.poke(vl)
        dut.io.preExe.xs1.poke(xs1)
        dut.io.preExe.valid.poke(valid)
        dut.io.preExe.lane.poke(lane)
        dut.io.preExe.vm.poke(vm)
      }
      dut.io.en.poke(true.B)
      dut.clock.setTimeout(100)
      // Do a simple addi (li)
      input(0.U, 0.U, 4.U, bitPatToUInt(OP.ADD), bitPatToUInt(OPA.VRS2), bitPatToUInt(OPB.XRS1), bitPatToUInt(MEMRE.N), bitPatToUInt(MEMWE.N), e32, bitPatToUInt(REGWE.VEC), 16.U, 0x00000003.U, true.B, bitPatToUInt(LANE.LI), false.B)
      dut.clock.step()
      while(!dut.io.done.peekBoolean()){dut.clock.step()}
      // Store the result in v4
      input(0.U, 0.U, 4.U, bitPatToUInt(OP.ADD), bitPatToUInt(OPA.VRS2), bitPatToUInt(OPB.XRS1), bitPatToUInt(MEMRE.N), bitPatToUInt(MEMWE.Y), e32, bitPatToUInt(REGWE.VEC), 16.U, 0.U, true.B, bitPatToUInt(LANE.LI), false.B)
      dut.clock.step()
      while(!dut.io.done.peekBoolean()){dut.clock.step()}
      // Load what was just stored in memory into v0
      input(0.U, 0.U, 0.U, bitPatToUInt(OP.ADD), bitPatToUInt(OPA.VRS2), bitPatToUInt(OPB.XRS1), bitPatToUInt(MEMRE.Y), bitPatToUInt(MEMWE.N), e32, bitPatToUInt(REGWE.VEC), 16.U, 0.U, true.B, bitPatToUInt(LANE.LI), false.B)
      dut.clock.step()
      while(!dut.io.done.peekBoolean()){dut.clock.step()}
      // Do some vector multiplication with v0 and v4 and store it in v8
      input(0.U, 4.U, 8.U, bitPatToUInt(OP.MUL), bitPatToUInt(OPA.VRS2), bitPatToUInt(OPB.VRS1), bitPatToUInt(MEMRE.N), bitPatToUInt(MEMWE.N), e32, bitPatToUInt(REGWE.VEC), 16.U, 0.U, true.B, bitPatToUInt(LANE.LIII), false.B)
      dut.clock.step()
      while(!dut.io.done.peekBoolean()){dut.clock.step()}
      // Do some division with v8 and v4 and store it in v0
      // Div opcode has a don't care bit, so it has to be put in manually
      input(4.U, 8.U, 12.U, "b0100".U, bitPatToUInt(OPA.VRS2), bitPatToUInt(OPB.VRS1), bitPatToUInt(MEMRE.N), bitPatToUInt(MEMWE.N), e32, bitPatToUInt(REGWE.VEC), 16.U, 0.U, true.B, bitPatToUInt(LANE.LII), false.B)
      dut.clock.step()
      while(!dut.io.done.peekBoolean()){dut.clock.step()}
      dut.clock.step()
    }
  }
}*/