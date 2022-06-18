package memory
import chisel3._
import chisel3.util._
import utility.{DCacheCtrlIO, DCacheIO, DDirectCacheIO}
import utility.Functions.connect
class DDirectCache(bSize : Int, lines : Int, memSize: Int) extends Module{
  val io = IO(new DDirectCacheIO(bSize, lines, memSize))

  val tag = io.addr(log2Up(memSize) - 1, log2Up(lines) + log2Up(bSize))
  val line = io.addr(log2Up(lines) + log2Up(bSize) - 1, log2Up(bSize))
  val offset = io.addr(log2Up(bSize) - 1, 0)
  val BYTE = "b00".U
  val HWORD = "b01".U
  val WORD = "b10".U

  val cache = RegInit(0.U.asTypeOf(new Bundle {
    val tag = Vec(lines, UInt((32 - log2Up(lines) - log2Up(bSize)).W))
    val valid = Vec(lines, Bool())
    val data = Vec(lines, Vec(bSize, UInt(8.W)))
    val dirty = Vec(lines, Bool())
  }))
  io.valid := cache.valid(line)
  io.hit := tag === cache.tag(line)
  io.tagOut := cache.tag(line)
  io.dirty := cache.dirty(line)
  io.dout := 0.U
  when(io.setDirty){
    cache.dirty(line) := true.B
  }
  when(io.write){
    cache.valid(line) := io.setValid
    cache.tag(line) := tag
    switch(io.memWidth){
      is(BYTE){
        cache.data(line)(0.U + offset) := io.din(7, 0)
      }
      is(HWORD){
        cache.data(line)(0.U + offset) := io.din(7, 0)
        cache.data(line)(1.U + offset) := io.din(15, 8)

      }
      is(WORD){
        cache.data(line)(0.U + offset) := io.din(7, 0)
        cache.data(line)(1.U + offset) := io.din(15, 8)
        cache.data(line)(2.U + offset) := io.din(23, 16)
        cache.data(line)(3.U + offset) := io.din(31, 24)
      }
    }
  } . otherwise{
    io.dout := cache.data(line)(3.U + offset) ## cache.data(line)(2.U + offset) ## cache.data(line)(1.U + offset) ## cache.data(line)(0.U + offset)
  }
  val snoopTag = io.snoopAddr(log2Up(memSize) - 1, log2Up(lines) + log2Up(bSize))
  val snoopLine = io.snoopAddr(log2Up(lines) + log2Up(bSize) - 1, log2Up(bSize))
  io.snoopHit := (cache.tag(snoopLine) === snoopTag) && cache.valid(line)
  io.snoopDirty := cache.dirty(snoopLine) | (snoopLine === line & io.setDirty)
  when(io.setInvalid){
    cache.valid(snoopLine) := false.B
    cache.dirty(snoopLine) := false.B

  }
}
class DCacheController(bSize : Int, lines: Int, memSize: Int, maxBSize: Int) extends Module {
  val io = IO(new DCacheCtrlIO(bSize, lines, memSize, maxBSize))

  val idle :: allocate :: writeback :: write :: read :: snoop :: update :: Nil = Enum(7)
  val state = RegInit(idle)
  val nextState = RegInit(idle)
  // For debugging
  println("DCacheCtrl states:")
  println("idle: " + idle.litValue.toInt.toBinaryString)
  println("read: " + read.litValue.toInt.toBinaryString)
  println("write: " + write.litValue.toInt.toBinaryString)
  println("allocate: " + allocate.litValue.toInt.toBinaryString)
  println("snoop: " + snoop.litValue.toInt.toBinaryString)
  println("writeback: " + writeback.litValue.toInt.toBinaryString + "\n")
  println("update: " + update.litValue.toInt.toBinaryString + "\n")
  val dirtyMiss = !io.cache.hit & io.cache.valid & io.cache.dirty
  val hit = io.cache.hit & io.cache.valid
  val cleanMiss = !hit & !dirtyMiss
  val RAM_WIDTH = 4 // Physical memory bus width in bytes
  val CNT_MAX_WORDS = (Mux(io.ram.snoop.request && io.ram.snoop.bSizeOut > io.ram.snoop.bSizeOut, io.ram.snoop.bSizeIn, io.ram.snoop.bSizeOut) >> 2).asUInt
  val cntWords = RegInit(0.U((bSize/RAM_WIDTH).W))

  val offset = io.core.addr(log2Up(bSize) - 1, 0)

  val aligned = offset + io.core.memWidth ## 0.U(1.W) <= (bSize - 1).U
  io.core.fatal := !aligned


  // Cache IO
  io.cache.addr := io.core.addr
  io.cache.din := io.core.dout
  io.cache.memWidth := io.core.memWidth
  io.cache.write := false.B
  io.cache.setDirty := false.B
  io.cache.setValid := false.B
  io.cache.setInvalid := io.cache.snoopHit && io.ram.snoop.invalidate
  io.cache.snoopAddr := io.ram.snoop.addr
  io.cache.setClean := false.B

  // Memory controller IO
  io.ram.dout := io.cache.dout
  io.ram.addr := io.core.addr(log2Up(memSize) - 1, 2) ## 0.U(2.W) + cntWords ## 0.U(2.W) // Physical memory is not byte addressable
  io.ram.valid := false.B
  io.ram.we := false.B

  // Snooping
  io.ram.snoop.we := false.B
  io.ram.snoop.bSizeOut := bSize.U
  io.ram.snoop.dirty := io.cache.snoopDirty
  io.ram.snoop.done := false.B
  io.ram.snoop.hit := io.cache.snoopHit


  // CPU IO
  io.core.ready := false.B
  io.core.din := io.cache.dout
  val allocateAddr = io.core.addr(log2Up(memSize) - 1, log2Up(bSize)) ## 0.U(log2Up(bSize).W) + cntWords ## 0.U(2.W)//Mux(io.ram.snoop.update, io.ram.snoop.addr(log2Up(memSize) - 1, 2), io.core.addr(log2Up(memSize) - 1, log2Up(lines) + log2Up(bSize)) ## 0.U((log2Up(lines) + log2Up(bSize)).W)) + (cntWords ## 0.U(2.W))
  val writebackAddr = io.core.addr(log2Up(memSize) - 1, log2Up(bSize)) ## 0.U(log2Up(bSize).W) + cntWords ## 0.U(2.W)  //Mux(io.ram.snoop.request, io.ram.snoop.addr(log2Up(memSize) - 1, 2), io.core.addr(log2Up(memSize) - 1, log2Up(lines) + log2Up(bSize)) ## 0.U((log2Up(lines) + log2Up(bSize)).W)) + (cntWords ## 0.U(2.W))

  switch(state) {
    is(idle) {
      when(io.ram.snoop.request){
        state := update
        cntWords := io.ram.snoop.addr(log2Up(bSize) - 1, 2) // Word offset
      } . elsewhen(io.core.valid){ // Incoming request
        when(hit){
          when(io.core.we){
            nextState := write
            state := Mux(io.cache.dirty, write, snoop)
          } . otherwise{
            state := read
          }
        } .elsewhen(dirtyMiss) { // Dirty miss on old block. New block might be dirty in other caches, so snoop afterwards
          state := writeback
          nextState := snoop
        }. otherwise{ // Clean miss
          state := snoop
          nextState := allocate
        }
      }
    }
    is(snoop){ // Include extra state so there is time to snoop
      io.ram.valid := true.B
      when(io.ram.ready){
        state := nextState
      }
      when(nextState === allocate){
        io.ram.addr := allocateAddr
      }
      io.ram.snoop.we := nextState === write
    }
    is(write){
      io.cache.write := true.B
      io.cache.setDirty := true.B
      io.cache.setValid := true.B
      io.core.ready := true.B
      state := idle
    }
    is(read){
      io.core.ready := true.B
      state := idle
    }
    is(allocate){
      io.ram.valid := true.B
      io.cache.setDirty := io.ram.snoop.update
      io.cache.addr := allocateAddr
      io.ram.addr := allocateAddr
      io.cache.din := io.ram.din
      when(cntWords === CNT_MAX_WORDS) {
        cntWords := 0.U
        io.ram.valid := false.B
        when(io.core.we){
          state := snoop
          nextState := write
        } . otherwise{
          state := read
        }
      }. elsewhen(io.ram.ready) { // We need to transfer multiple words
        cntWords := cntWords + 1.U
        // Write data from memory to cache
        io.cache.write := true.B
        io.cache.setDirty := false.B
        io.cache.setValid := true.B
        io.cache.memWidth := "b10".U
        when(!io.ram.snoop.update & cntWords =/= (CNT_MAX_WORDS - 1.U)){state := snoop} // Snoop next address
      }
    }
    is(writeback){
      io.ram.valid := true.B
      io.cache.addr := writebackAddr
      //val snoopLine = io.snoopAddr(log2Up(lines) + log2Up(bSize) - 1, log2Up(bSize))
      io.ram.addr := io.cache.tagOut ## io.core.addr(log2Up(lines) + log2Up(bSize) - 1, log2Up(bSize)) + (cntWords ## 0.U(2.W))
      io.ram.we := true.B
      io.ram.dout := io.cache.dout
      io.cache.memWidth := "b10".U
      // io.cache.setInvalid := true.B
      when(cntWords === CNT_MAX_WORDS){
        cntWords := 0.U
        io.ram.valid := false.B
        io.ram.snoop.done := true.B
        state := nextState
      } .elsewhen(io.ram.ready) {
        cntWords := cntWords + 1.U
      }
    }
    is(update){ // Transfer dirty data to other cache
      io.ram.valid := true.B
      io.cache.addr := io.ram.snoop.addr
      io.ram.dout := io.cache.dout
      io.cache.memWidth := "b10".U
      when(cntWords === (CNT_MAX_WORDS) | !io.ram.snoop.request){
        cntWords := 0.U
        io.ram.valid := false.B
        io.ram.snoop.done := true.B
        state := idle
      } .elsewhen(io.ram.ready) {
        cntWords := cntWords + 1.U
        io.cache.setInvalid := true.B
      }
    }

  }
}
class DCache(bSize : Int, lines : Int, memSize: Int, maxBSize: Int) extends Module {
  val io = IO(new DCacheIO(memSize, maxBSize))
  val dcachectrl = Module(new DCacheController(bSize, lines, memSize, maxBSize))
  val cache = Module(new DDirectCache(bSize, lines, memSize))
  connect(io.core.elements, dcachectrl.io.core.elements)
  connect(dcachectrl.io.cache.elements, cache.io.elements)
  connect(io.ram.elements, dcachectrl.io.ram.elements)
}
