import scalarcore._

import chisel3._
import chisel3.util._

import utility.Constants._
import utility.Functions.connect
import memory._

class Penta(memSize: Int, bSizeI: Int, linesI: Int, maxDelay: Int, program: String = "") extends Module{
  val io = IO(new Bundle{
   //val csr = Output(Vec(NUM_OF_CSR,UInt(DATA_WIDTH.W)))
   val csr = Output(UInt((DATA_WIDTH/2).W))
   val led = Output(Bool())
  })
  val channels = 2
  val scalarCore = Module(new ScalarCore(memSize, bSizeI, linesI, maxDelay))
  val syncRam = Module(new SyncRAM(memSize, program))
  val memCtrl = Module(new MemoryController(channels, memSize))
  connect(memCtrl.io.clients(1).elements, scalarCore.io.clients(0).elements)
  connect(memCtrl.io.clients(0).elements, scalarCore.io.clients(1).elements)
  connect(memCtrl.io.ram.elements, syncRam.io.elements)
  io.csr := scalarCore.io.csr(0)(15,0)
  io.led := scalarCore.io.led
  scalarCore.io.vec.busyIn := false.B
  scalarCore.io.vec.haltFetch := false.B
  scalarCore.io.vec.rs1 := 0.U
  scalarCore.io.vec.rs2 := 0.U
  scalarCore.io.vec.rd := 0.U
  scalarCore.io.vec.we := 0.U
  scalarCore.io.vec.xrd := 0.U
}

object Penta extends App {
  println("Generating hardware")
  val memSize = 512
  val bSizeI = 64
  val linesI = 4
  val maxDelay = 56
  val program = "program.txt"
  emitVerilog(new Penta(memSize, bSizeI, linesI, maxDelay, program), args)
  println("Hardware successfully generated")
}
