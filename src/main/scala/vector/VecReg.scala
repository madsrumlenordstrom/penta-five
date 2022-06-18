package vector

import chisel3._
import chisel3.util._
import utility.Constants._
import utility.Functions.connect
class VecRegBankIO extends Bundle{
  val ew = Input(UInt(3.W))
  val vs1Addr = Input(UInt(log2Up(4*VLEN).W))
  val vs2Addr = Input(UInt(log2Up(4*VLEN).W))
  val vdAddr = Input(UInt(log2Up(4*VLEN).W))
  val mask = Output(UInt(VLEN.W))
  val we = Input(Bool())
  val din = Input(UInt(32.W))
  val vec1 = Output(UInt(32.W))
  val vec2 = Output(UInt(32.W))
}
class VecRegCoreIO extends Bundle{
  val en = Input(Bool())
  val mask = Output(UInt(VLEN.W))
  val write = new Bundle{
    val we = Input(Bool()) // Works as a "valid" signal too
    val ready = Output(Bool())
    val vd = Input(UInt(5.W))
    val din = Input(UInt(32.W))
    val ew = Input(UInt(3.W))
    val vl = Input(UInt(log2Up(VLEN + 1).W))
    val vstart = Input(UInt(log2Up(VLEN).W))
    val vm = Input(Bool())
    val done = Output(Bool())
  }
  val read = new Bundle{
    val re = Input(Bool()) // Works as a "valid" signal too
    val ready = Output(Bool())
    val vs = Input(Vec(2,UInt(5.W)))
    val ew = Input(UInt(3.W))
    val vl = Input(UInt(log2Up(VLEN + 1).W))
    val vstart = Input(UInt(log2Up(VLEN).W))
    val vec = Output(Vec(2, UInt(32.W)))
    val done = Output(Bool())
  }
}
class VecRegCtrlIO extends Bundle{
  val bank = Flipped(new VecRegBankIO)
  val core = new VecRegCoreIO
}
class VecRegBank extends Module {
  val io = IO(new VecRegBankIO)
  val reg = RegInit(0.U.asTypeOf(Vec(4*VLEN, UInt(8.W))))

  io.vec1 := (reg(io.vs1Addr + 3.U) ## reg(io.vs1Addr + 2.U) ## reg(io.vs1Addr + 1.U) ## reg(io.vs1Addr)).asUInt
  io.vec2 := (reg(io.vs2Addr + 3.U) ## reg(io.vs2Addr + 2.U) ## reg(io.vs2Addr + 1.U) ## reg(io.vs2Addr)).asUInt
  io.mask := (reg(7) ## reg(6) ## reg(5) ## reg(4) ## reg(3) ## reg(2) ## reg(1) ## reg(0)).asUInt
  when(io.we){
    switch(io.ew){
      is(e8){ // Only write 1 element
        reg(io.vdAddr) := io.din(7, 0)
      }
      is(e16){
        reg(io.vdAddr) := io.din(7, 0)
        reg(io.vdAddr + 1.U) := io.din(15, 8)
      }
      is(e32){
        reg(io.vdAddr) := io.din(7, 0)
        reg(io.vdAddr + 1.U) := io.din(15, 8)
        reg(io.vdAddr + 2.U) := io.din(23, 16)
        reg(io.vdAddr + 3.U) := io.din(31, 24)
      }
    }
  }
}
class VecRegCtrl extends Module{
  val io = IO(new VecRegCtrlIO)

  // TODO maybe sample ew, vl and vstart?
  val cntWrites = RegInit(0.U(log2Up(VLEN).W))
  val cntMaxWrites = RegInit(0.U(log2Up(VLEN).W))
  val vdAddr = RegInit(0.U(log2Up(4*VLEN).W))
  val cntReads = RegInit(0.U(log2Up(VLEN).W))
  val cntMaxReads = RegInit(0.U(log2Up(VLEN).W))
  val vs1Addr = RegInit(0.U(log2Up(4*VLEN).W))
  val vs2Addr = RegInit(0.U(log2Up(4*VLEN).W))

  // Core output
  io.core.write.ready := false.B
  io.core.write.done := false.B
  io.core.read.ready := false.B
  io.core.read.vec(0) := io.bank.vec1
  io.core.read.vec(1) := io.bank.vec2
  when(io.core.read.ew === e8){
    io.core.read.vec(0) := io.bank.vec1(7, 0)
    io.core.read.vec(1) := io.bank.vec2(7, 0)
  } .elsewhen(io.core.read.ew === e16){
    io.core.read.vec(0) := io.bank.vec1(15, 0)
    io.core.read.vec(1) := io.bank.vec2(15, 0)
  }
  io.core.read.done := false.B
  io.core.mask := io.bank.mask
  // Bank output
  io.bank.vdAddr := vdAddr
  io.bank.we := false.B
  io.bank.ew := io.core.write.ew
  io.bank.din := io.core.write.din
  io.bank.vs1Addr := vs1Addr
  io.bank.vs2Addr := vs2Addr

  val write = RegInit(false.B)
  val read = RegInit(false.B)
  when(io.core.en) {
    // Writing logic
    when(!write & io.core.write.we) {
      cntMaxWrites := io.core.write.vl
      vdAddr := io.core.write.vd ## 0.U(log2Up(VLEN/8).W)
      cntWrites := 0.U
      write := ~write
    }.elsewhen(write & io.core.write.we) {
      io.core.write.ready := true.B
      cntWrites := cntWrites + 1.U
      io.bank.we := (!io.core.write.vm | (io.core.write.vm & io.bank.mask(cntWrites)))
      vdAddr := vdAddr + (1.U << io.core.write.ew).asUInt
      when(cntWrites === (cntMaxWrites - 1.U)) {
        cntWrites := 0.U
        write := ~write
        io.core.write.done := true.B
      }
    }
    // Reading logic
    when(!read &  io.core.read.re) {
      cntMaxReads := io.core.read.vl
      vs1Addr := io.core.read.vs(0) ## 0.U(log2Up(VLEN/8).W)
      vs2Addr := io.core.read.vs(1) ## 0.U(log2Up(VLEN/8).W)
      cntReads := 0.U
      read := ~read
    }.elsewhen(read & io.core.read.re) {
      io.core.read.ready := true.B
      vs1Addr := vs1Addr + (1.U << io.core.read.ew).asUInt
      vs2Addr := vs2Addr + (1.U << io.core.read.ew).asUInt
      cntReads := cntReads + 1.U
      when(cntReads === (cntMaxReads - 1.U)) {
        cntReads := 0.U
        read := ~read
        io.core.read.done := true.B
      }
    }
  }
}
class VecReg extends Module{
  val io = IO(new VecRegCoreIO)

  val vecRegCtrl = Module(new VecRegCtrl)
  val vecRegBank = Module(new VecRegBank)
  connect(vecRegCtrl.io.bank.elements, vecRegBank.io.elements)
  connect(io.elements, vecRegCtrl.io.core.elements)
}


