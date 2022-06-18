package memory
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import utility.{ByteRAMIO, MemCtrlIO}
import utility.Constants._
import scala.io.Source
import java.io._

class SyncRAM(memSize: Int, program: String = "") extends Module{
  val io = IO(new ByteRAMIO(memSize))
  val bankA = SyncReadMem(memSize/4, UInt(8.W))
  val bankB = SyncReadMem(memSize/4, UInt(8.W))
  val bankC = SyncReadMem(memSize/4, UInt(8.W))
  val bankD = SyncReadMem(memSize/4, UInt(8.W))
  val rowUpper = io.addr(log2Up(memSize) - 1,2) + 1.U
  val rowLower = io.addr(log2Up(memSize) - 1,2)
  val dout = WireInit(VecInit(Seq.fill(4)(0.U(8.W))))
  val rowA = WireDefault(rowLower)
  val rowB = WireDefault(rowLower)
  val rowC = WireDefault(rowLower)
  val rowD = WireDefault(rowLower)


  val write2Bytes = io.memWidth(1) | io.memWidth(0)
  val write4Bytes = io.memWidth(1)
  val addr = RegInit(io.addr)
  val writeBankB = io.memWidth
  when(io.addr(1) || io.addr(0)){ rowA := rowUpper }
  when(io.addr(1)){ rowB := rowUpper }
  when(io.addr(1) && io.addr(0)){ rowC := rowUpper }

  addr := io.addr
  dout(0) := bankA.read(rowA)
  dout(1) := bankB.read(rowB)
  dout(2) := bankC.read(rowC)
  dout(3) := bankD.read(rowD)
  switch(io.addr(1,0)){
    is(0.U){
      when(io.we){
        bankA.write(rowA, io.din(7, 0))
        //bankB.write(rowB, io.din(15, 8))
        //bankC.write(rowC, io.din(23, 16))
        //bankD.write(rowD, io.din(31, 24))
        when(write2Bytes) {
          bankB.write(rowB, io.din(15, 8))
        }
        when(write4Bytes) {
          bankC.write(rowC, io.din(23, 16))
          bankD.write(rowD, io.din(31, 24))
        }
      }
    }
    is(1.U) {
      when(io.we) {
        bankB.write(rowB, io.din(7, 0))
        when(write2Bytes) {
          bankC.write(rowC, io.din(15, 8))
        }
        when(write4Bytes) {
          bankD.write(rowD, io.din(23, 16))
          bankA.write(rowA, io.din(31, 24))
        }
      }
    }
    is(2.U){
      when(io.we){
        bankC.write(rowC, io.din(7, 0))
        when(write2Bytes) {
          bankD.write(rowD, io.din(15, 8))
        }
        when(write4Bytes) {
          bankA.write(rowA, io.din(23, 16))
          bankB.write(rowB, io.din(31, 24))
        }
      }
    }
    is(3.U){
      when(io.we){
        bankD.write(rowD, io.din(7, 0))
        when(write2Bytes) {
          bankA.write(rowA, io.din(15, 8))
        }
        when(write4Bytes) {
          bankB.write(rowB, io.din(23, 16))
          bankC.write(rowC, io.din(31, 24))
        }
      }
    }
  }
  io.dout := (dout(3) ## dout(2) ## dout(1) ## dout(0)).asUInt
  switch(addr(1,0)){
    is(1.U){
      io.dout := (dout(0) ## dout(3) ## dout(2) ## dout(1)).asUInt
    }
    is(2.U){
      io.dout := (dout(1) ## dout(0) ## dout(3) ## dout(2)).asUInt
    }
    is(3.U){
      io.dout := (dout(2) ## dout(1) ## dout(0) ## dout(3)).asUInt
    }
  }
  def hexToInt(s: String): Int = {
    s.toList.map("0123456789abcdef".indexOf(_)).reduceLeft(_ * 16 + _)
  }
  /*
  val array = new Array[Int](14)
  var i = 0
  val source = Source.fromFile(program)
  for(line <- source.getLines()){
    println("Input: " + line)
    array(i) = hexToInt(line)
    i+=1
    println("@" + (4*i) + " : " + array(i).toHexString)
  }
  val rom = VecInit(array.map(_.S(32.W)))
  when(addr <= 48.U){
    io.dout := rom(addr(log2Up(memSize)-1,2)).asUInt
  }

   */



  if(program != ""){
    val mainFile = new BufferedReader(new FileReader(program))
    val initA = "bankA.txt"
    val initB = "bankB.txt"
    val initC = "bankC.txt"
    val initD = "bankD.txt"
    val writeA = new PrintWriter(new File(initA))
    val writeB = new PrintWriter(new File(initB))
    val writeC = new PrintWriter(new File(initC))
    val writeD = new PrintWriter(new File(initD))
    var line : String = mainFile.readLine()
    while(line != null){
      writeD.write(line.slice(0,2) + "\n")
      writeC.write(line.slice(2,4) + "\n")
      writeB.write(line.slice(4,6) + "\n")
      writeA.write(line.slice(6,8) + "\n")
      line = mainFile.readLine()
    }
    writeA.close()
    writeB.close()
    writeC.close()
    writeD.close()
    loadMemoryFromFileInline(bankA, initA)
    loadMemoryFromFileInline(bankB, initB)
    loadMemoryFromFileInline(bankC, initC)
    loadMemoryFromFileInline(bankD, initD)

  }
}


class MemoryController(channels: Int, memSize: Int) extends Module {
  val io = IO(new MemCtrlIO(channels, memSize))

  val idle :: read :: write :: Nil = Enum(3)
  val state = RegInit(idle)
  val clientIdx = RegInit(0.U)
  val snoopAddr = RegInit(0.U(log2Up(memSize).W))
  val snoopInvalidate = RegInit(false.B)
  val memWidth = RegInit(0.U(2.W))
  for (i <- 0 until channels) {
    io.clients(i).ready := (clientIdx === i.U) & (state =/= idle)
    io.clients(i).din := io.ram.dout
    io.clients(i).snoop.addr := snoopAddr
    io.clients(i).snoop.invalidate := snoopInvalidate
  }
  io.ram.we := (state === write) & (io.clients(clientIdx).we)
  io.ram.memWidth := memWidth
  io.ram.din := io.clients(clientIdx).dout
  io.ram.addr := io.clients(clientIdx).addr
  memWidth := io.clients(clientIdx).memWidth
  snoopAddr := io.ram.addr
  snoopInvalidate := io.ram.we


  // TODO add burst r/w mode
  switch(state) {
    is(idle) {
      for (i <- 0 until channels) {
        when(io.clients(i).valid) {
          state := Mux(io.clients(i).we, write, read)
          clientIdx := i.U
          io.ram.din := io.clients(i).dout
          io.ram.addr := io.clients(i).addr
          memWidth := io.clients(i).memWidth
        }
      }
    }
    is(read) {
      io.clients(clientIdx).ready := true.B
      io.ram.addr := io.clients(clientIdx).addr + (1.U << memWidth )
      state := Mux(io.clients(clientIdx).valid & !io.clients(clientIdx).we & io.clients(clientIdx).burst, read, idle)
    }
    is(write) {
      io.clients(clientIdx).ready := true.B
      state := Mux(io.clients(clientIdx).valid & io.clients(clientIdx).burst, write, idle) // Not checking for we to allow masked writes
    }
  }
}
