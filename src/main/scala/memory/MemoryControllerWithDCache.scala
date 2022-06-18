package memory

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import utility.{MemCtrlWDCacheIO, RAMIO}
class SynchronousRAM(size: Int, program: String = "") extends Module{
  val io = IO(new RAMIO(size))

  val mem = SyncReadMem(size/4, UInt(32.W))
  io.dout := mem.read(io.addr)
  when(io.we){
    mem.write(io.addr, io.din)
  }
  if(program != ""){
    loadMemoryFromFileInline(mem, program)
  }
}
class MemoryControllerWithDCache(channels: Int, memSize: Int, maxBSize: Int) extends Module{
  val io = IO(new MemCtrlWDCacheIO(channels, memSize, maxBSize))

  val idle :: write :: read :: update :: validate :: Nil = Enum(5)
  println("MemCtrl states:")
  println("idle: " + idle.litValue.toInt.toBinaryString)
  println("read: " + read.litValue.toInt.toBinaryString)
  println("write: " + write.litValue.toInt.toBinaryString)
  println("update: " + update.litValue.toInt.toBinaryString)
  println("validate: " + validate.litValue.toInt.toBinaryString + "\n")
  val clientIdxRX = RegInit(0.U(log2Up(channels).W))
  val clientIdxTX = RegInit(0.U(log2Up(channels).W))
  val state = RegInit(idle)
  val nextState = RegInit(idle)
  val foundDirty = WireDefault(false.B)
  val snoopAddr = RegInit(0.U(log2Up(memSize).W))
  val cntWords = RegInit(0.U((maxBSize/4).W))

  // Standard assignments for output signals
  for(i <- 0 until channels){
    io.clients(i).din := io.ram.dout
    io.clients(i).ready := false.B
    io.clients(i).snoop.request := false.B
    io.clients(i).snoop.bSizeIn := io.clients(clientIdxTX).snoop.bSizeOut
    io.clients(i).snoop.invalidate := false.B
    io.clients(i).snoop.update := false.B
    io.clients(i).snoop.addr := snoopAddr

  }
  io.ram.we := false.B
  io.ram.addr := io.clients(clientIdxRX).addr(log2Up(memSize) - 1, 2) // Remove byte offset
  io.ram.din := io.clients(clientIdxRX).dout
  snoopAddr := io.clients(clientIdxRX).addr
  switch(state){
    is(idle) {
      for (i <- 0 until channels) {
        when(io.clients(i).valid) {
          clientIdxRX := i.U
          cntWords := io.clients(i).snoop.bSizeOut >> 2
          snoopAddr := io.clients(i).addr
          when(io.clients(i).we) {
            state := write
          }.otherwise {
            state := validate
          }
        }
      }
    }
    is(validate) {
      io.clients(clientIdxRX).ready := true.B
      when(io.clients(clientIdxRX).snoop.we) {
        state := idle
        for(i <- 0 until channels) {
          when(i.U =/= clientIdxRX) {
            io.clients(i).snoop.invalidate := true.B
          }
        }
      } . otherwise {
        state := read
        for(i <- 0 until channels) {
          when(i.U =/= clientIdxRX && io.clients(i).snoop.hit && io.clients(i).snoop.dirty) {
            clientIdxTX := i.U
            state := update

          }
        }
      }
    }
    is(write){
      io.ram.we := 1.U
      io.clients(clientIdxRX).ready := true.B
      cntWords := cntWords - 1.U
      state := Mux(cntWords === 1.U, idle, write)
    }
    is(read){
      io.clients(clientIdxRX).ready := true.B
      cntWords := cntWords - 1.U
      snoopAddr := io.clients(clientIdxRX).addr + 4.U
      state := Mux(cntWords === 1.U, idle, validate)
    }
    is(update){ // Here we let the two cache's talk together
      // Transmitter cache must send as many words as needed.
      io.clients(clientIdxTX).snoop.request := true.B
      io.clients(clientIdxRX).ready := io.clients(clientIdxTX).valid
      io.clients(clientIdxTX).ready := io.clients(clientIdxRX).valid
      io.clients(clientIdxRX).snoop.update := true.B
      io.clients(clientIdxTX).snoop.addr := snoopAddr
      when(io.clients(clientIdxTX).valid & io.clients(clientIdxRX).valid){ // One word has been transfered
        cntWords := cntWords - 1.U
        snoopAddr := io.clients(clientIdxRX).addr + 4.U
        when(cntWords === 1.U){
          state := idle
        }
      }
      when(io.clients(clientIdxTX).snoop.done){ // Transmitter has nothing more to send. We must revalidate
        state := validate
      }
      io.clients(clientIdxRX).din := io.clients(clientIdxTX).dout
      //io.clients(clientIdxRX).snoop.update := true.B // Tell client block allocated is dirty
    }
  }
}