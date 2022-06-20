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
class VecCoreWMem(memSize: Int, maxDelay: Int, data: String) extends Module{
  val io = IO(new Bundle{
    val scalar = new Bundle{
      val rs1, rs2, rd = Output(UInt(5.W))
      val xs1, xs2 = Input(UInt(32.W))
      val xd1 = Output(UInt(32.W))
      val we = Output(Bool())
    }
    val inst = Input(UInt(32.W))
    val en = Input(Bool())
    val done = Output(Bool())
    val addr = Output(UInt(log2Up(memSize).W)) // So stuff isn't optimized away
    val pending = Output(Bool())
  })
  val ram = Module(new SyncRAM(memSize, data))
  val ctrl = Module(new MemoryController(1, memSize))
  val vecCore = Module(new VecCore(memSize, maxDelay))

  connect(ctrl.io.ram.elements, ram.io.elements)
  connect(ctrl.io.clients(0).elements, vecCore.io.ram.elements)
  connect(io.scalar.elements, vecCore.io.scalar.elements)
  io.addr := vecCore.io.ram.addr
  vecCore.io.en := io.en
  vecCore.io.inst := io.inst
  io.done := vecCore.io.done
  io.pending := vecCore.io.pending
}

class VecCoreSpec extends AnyFlatSpec with ChiselScalatestTester {
  val data = "data.txt"
  val memSize = 128
  val maxDelay = 53
  "VecCore" should "pass" in {
    test(new VecCoreWMem(memSize, maxDelay, data)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val x = Array.fill[Int](32)(0)
      def step(): Unit ={
        val we = dut.io.scalar.we.peekBoolean()
        val rd = dut.io.scalar.rd.peekInt().toInt
        val rs1 = dut.io.scalar.rs1.peekInt().toInt
        val rs2 = dut.io.scalar.rs2.peekInt().toInt
        val xd1 = dut.io.scalar.xs1.peekInt().toInt
        dut.io.scalar.xs1.poke(x(rs1).U)
        dut.io.scalar.xs2.poke(x(rs2).U)
        if(we){
          x(rd) = xs1
        }
        dut.clock.step()
      }
      def printX(): Unit ={
        for(i <- 0 to 7){
          for(j <- 0 to 3){
            print("x("+(4*i+j)+") : " + x(4*i+j).toHexString + "    ")
          }
          println()
        }
      }
      def inputInst(inst: UInt): Unit ={
        while(!dut.io.done.peekBoolean()){step()} // Wait until the core can take an instruction
        dut.io.inst.poke(inst)
        step()
      }
      dut.clock.setTimeout(50)
      dut.io.en.poke(true.B)
      // First instruction should set the vtype register. We do a vsetvli. Lets aim for lmul = 1, and sew = 8 and enforce
      // vl to be vmax (rd!= 0 and rs1 === 0).
      inputInst("b0_00000_000_000_00000_111_00001_1010111".U)
      // Then lets load the mask, so that v0.mask[i] = 1 for i = 0, 4, 8,... (v0 = v0 + 0x11)
      inputInst("b000000_0_00000_10001_011_00000_1010111".U)
      // Do a masked load immediate for v1 (v1 = v1 + 0x0A)
      inputInst("b000000_1_00001_01010_011_00001_1010111".U)
      // Do a load immediate for v2 (v2 = v2 + 0x02)
      inputInst("b000000_0_00010_00010_011_00010_1010111".U)
      // Do a masked v3 = v2 + v1
      inputInst("b000000_1_00010_00001_000_00011_1010111".U)
      // Not lets store v2 as a masked e8 store, with x0 as base address
      inputInst("b000_0_00_1_00000_00000_000_00010_0100111".U)
      //000_0_00_1_00000_00100_000_00010_0100111
      // Then lets load v4 as an unmasked e8 load, with x0 as base address
      inputInst("b000_0_00_0_00000_00000_000_00100_0000111".U)
      //000_0_00_0_00000_00100_000_00100_0000111
      // Then lets load v5 as masked e8 load, with x0 as base address
      inputInst("b000_0_00_1_00000_00000_000_00101_0000111".U)
      //000_0_00_1_00000_00100_000_00101_0000111
      // Input an unknown instruction, so testbench knows when to stop
      inputInst("b0".U)
      while(dut.io.pending.peekBoolean()){step()} // Wait until the core has no more instructions to do
      step()
      printX()
    }
  }
}
*/