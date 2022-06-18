

package scalarcore
import chisel3._
import utility.{MEMWB, MemoryIO}
import utility.Functions.connect
import chisel3.util._
import memory.DCache
import utility.Controlsignals._

class Memory(memSize: Int) extends Module{
  val io = IO(new MemoryIO(memSize))

  val memwb = RegInit(0.U.asTypeOf(new MEMWB))
  val memOP = io.exmem.meCtrl.memData
  val memSigned = !memOP(0)
  val memWidth = Mux(memOP(2), "b10".U,Mux(memOP(1), "b01".U, "b00".U))

  io.ram.valid := io.exmem.meCtrl.memWE.asBool | io.exmem.meCtrl.memRE.asBool
  io.ram.we := io.exmem.meCtrl.memWE.asBool
  io.ram.addr := io.exmem.addr
  io.ram.dout := io.exmem.data
  io.ram.memWidth := memWidth
  io.ram.burst := false.B

  io.stall := !io.ram.ready & io.ram.valid

  val memOut = Mux(memOP(2), io.ram.din,
               Mux(memOP(1), Fill(16, Mux(memSigned, io.ram.din(15), 0.U)) ## io.ram.din(15, 0),
               Fill(24, Mux(memSigned, io.ram.din(7), 0.U)) ## io.ram.din(7, 0)))

  val csrRegBank = Module(new CSRRegBank)
  csrRegBank.io.csrSrcDest := io.exmem.csrSrcDest
  csrRegBank.io.dataIn := io.exmem.data
  csrRegBank.io.write := io.exmem.wbCtrl.regWE === RegWE.CSR
  io.csr := csrRegBank.io.csr
  val led = RegInit(false.B)


  when(io.en & !io.stall){
    memwb.wbCtrl := io.exmem.wbCtrl
    memwb.memData := memOut
    memwb.aluData := io.exmem.data
    memwb.rd := io.exmem.rd
    memwb.pcPlusFour := io.exmem.pcPlusFour
    memwb.csrData := csrRegBank.io.dataOut
    when(io.exmem.addr === 0x69.U && io.exmem.meCtrl.memWE.asBool){
      led := io.exmem.data(0).asBool
    }
  }
  io.memwb <> memwb
  io.led := led
}
