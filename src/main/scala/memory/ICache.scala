package memory

import utility.{ICacheCtrlIO, ICacheIO, IDirectCacheIO}
import utility.Functions.connect

import chisel3._
import chisel3.experimental.DataMirror
import chisel3.util._
import scala.collection.immutable

class IDirectCache(bSize : Int, lines : Int, memSize: Int) extends Module{
  val io = IO(new IDirectCacheIO(bSize, memSize))

  val tag = io.addr(log2Up(memSize) - 1, log2Up(lines) + log2Up(bSize))
  val line = io.addr(log2Up(lines) + log2Up(bSize) - 1, log2Up(bSize))
  val offset = io.addr(log2Up(bSize) - 1, 0)
  val dout = WireDefault(0.U.asTypeOf(Vec(bSize, UInt(8.W))))

  val cache = RegInit(0.U.asTypeOf(new Bundle {
    val tag = Vec(lines, UInt((32 - log2Up(lines) - log2Up(bSize)).W))
    val valid = Vec(lines, Bool())
    val data = Vec(lines, Vec(bSize, UInt(8.W)))
  }))
  io.hit := (tag === cache.tag(line)) && cache.valid(line)
  cache.valid(line) := Mux(io.setValid, true.B, cache.valid(line))
  io.dout := cache.data(line)(3.U + offset) ## cache.data(line)(2.U + offset) ## cache.data(line)(1.U + offset) ## cache.data(line)(offset)
  when(io.write) {
    cache.tag(line) := tag
    for(i <- 0 to 3){
      cache.data(line)(i.U + offset) := io.din(8 * i + 7, 8 * i)
    }
  }
  val snoopTag = io.snoopAddr(log2Up(memSize) - 1, log2Up(lines) + log2Up(bSize))
  val snoopLine = io.snoopAddr(log2Up(lines) + log2Up(bSize) - 1, log2Up(bSize))
  io.snoopHit := (cache.tag(snoopLine) === snoopTag) && cache.valid(line)
  when(io.setInvalid){cache.valid(snoopLine) := false.B} // This assignment occurs lastly, so if there is a write to an
  // address which we have just read, it will be marked as invalid
}

class ICacheController(bSize: Int, lines: Int, memSize: Int) extends Module{
  val io = IO(new ICacheCtrlIO(bSize, lines, memSize))
  val idle :: read :: allocate ::  Nil = Enum(3)
  // For debugging
  println("ICacheCtrl states:")
  println("idle: " + idle.litValue.toInt.toBinaryString)
  println("read: " + read.litValue.toInt.toBinaryString)
  println("allocate: " + allocate.litValue.toInt.toBinaryString)
  val state = RegInit(idle)
  val nextState = RegInit(idle)


  val hit = io.cache.hit
  val RAM_WIDTH = 4 // Physical memory bus width in bytes
  val CNT_MAX_WORDS = (bSize >> 2)
  //val CNT_MAX_WORDS = (bSize/RAM_WIDTH)
  val cntWords = RegInit(0.U(log2Up(CNT_MAX_WORDS).W))

  io.cache.addr := io.core.addr + cntWords ## 0.U(2.W)
  io.cache.din := io.ram.din
  io.cache.write := false.B
  io.cache.setValid := false.B
  io.cache.snoopAddr := io.ram.snoop.addr
  io.cache.setInvalid := io.cache.snoopHit && io.ram.snoop.invalidate


  // Memory controller IO
  io.ram.dout := 0.U // Nothing to write
  io.ram.addr := io.core.addr(log2Up(memSize) - 1, 2) ## 0.U(2.W) + cntWords ## 0.U(2.W)
  io.ram.we := false.B
  io.ram.valid := false.B
  io.ram.memWidth := 2.U
  io.ram.burst := true.B
  // Snoop


  io.core.fatal := io.core.addr(1,0) === 0.U // Misaligned address is a fatal exception
  io.core.valid := false.B
  io.core.dout := io.cache.dout

  // Add logic for snooping

  val allocateAddr = io.core.addr(log2Up(memSize) - 1, log2Up(bSize)) ## 0.U(log2Up(bSize).W) + cntWords ## 0.U(2.W)
  switch(state){
    is(idle){
      state := read
    }
    is(read){
      when(hit){
        io.core.valid := true.B
      } . otherwise{
        state := allocate
      }
    }
    is(allocate){
      io.ram.valid := true.B
      io.ram.addr := allocateAddr
      io.cache.addr := allocateAddr
      when(io.ram.ready){
        io.cache.write := true.B
        io.cache.setValid := true.B
        cntWords := cntWords + 1.U
        when(cntWords === (CNT_MAX_WORDS - 1).U){
          state := read
          cntWords := 0.U
        }
      }
    }
  }
}
class ICache(bSize: Int, lines: Int, memSize: Int) extends Module{
  val io = IO(new ICacheIO(memSize))
  val icache = Module(new IDirectCache(bSize, lines, memSize))
  val icachectrl = Module(new ICacheController(bSize, lines, memSize))
  connect(icachectrl.io.cache.elements, icache.io.elements)
  connect(io.core.elements, icachectrl.io.core.elements)
  connect(io.ram.elements, icachectrl.io.ram.elements)
}