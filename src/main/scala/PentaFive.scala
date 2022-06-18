import chisel3._
import utility.Constants.DATA_WIDTH
import utility.CoreIO
import scalarcore._
import scalarcore.ScalarCore
import memory.{MemoryController, SyncRAM}
import vector.VecCore
import utility.Functions.connect


class PentaFive(memSize: Int, bSizeI: Int, linesI: Int, maxDelay: Int, program: String = "") extends Module{
  val io = IO(new Bundle{
    val csr = Output(UInt((DATA_WIDTH/2).W))
    val led = Output(Bool())
  })
  val vecCore = Module(new VecCore(memSize, maxDelay))
  val scalarCore = Module(new ScalarCore(memSize, bSizeI, linesI, maxDelay))
  val ctrl = Module(new MemoryController(3, memSize))
  val ram = Module(new SyncRAM(memSize, program))
  connect(ctrl.io.ram.elements, ram.io.elements)
  connect(ctrl.io.clients(2).elements, scalarCore.io.clients(0).elements) // Fetch gets highest priority
  connect(ctrl.io.clients(1).elements, vecCore.io.ram.elements) // Vector LS gets second highest priority
  connect(ctrl.io.clients(0).elements, scalarCore.io.clients(1).elements) // Scalar LS gets least highest priority
  scalarCore.io.vec.busyIn := vecCore.io.pending
  scalarCore.io.vec.haltFetch := vecCore.io.haltFetch
  scalarCore.io.vec.rs1 := vecCore.io.scalar.rs1
  scalarCore.io.vec.rs2 := vecCore.io.scalar.rs2
  scalarCore.io.vec.xrd := vecCore.io.scalar.xrd
  scalarCore.io.vec.rd := vecCore.io.scalar.rd
  scalarCore.io.vec.we := vecCore.io.scalar.we
  vecCore.io.scalar.xs1 := scalarCore.io.vec.xs1
  vecCore.io.scalar.xs2 := scalarCore.io.vec.xs2
  vecCore.io.en := !scalarCore.io.vec.busyOut
  vecCore.io.inst := scalarCore.io.vec.inst
  io.csr := scalarCore.io.csr(0)(15,0)
  io.led := scalarCore.io.led
}
object PentaFive extends App {
  println("Generating hardware")
  val memSize = 512
  val bSizeI = 64
  val linesI = 4
  val maxDelay = 56
  val program = "program.txt"
  emitVerilog(new PentaFive(memSize, bSizeI, linesI, maxDelay, program), args)
  println("Hardware successfully generated")
}