package vector
import chisel3._
import memory._
import chisel3.util._
import utility.ClientIO
import utility.Functions.connect
import utility.Constants._
class VecLSUnitCoreIO(memSize: Int) extends Bundle{
  val addr = Input(UInt(log2Up(memSize).W))
  val dout = Input(UInt(32.W))
  val we, valid = Input(Bool())
  val ew = Input(UInt(2.W))
  val vl = Input(UInt(log2Up(VLEN + 1).W))
  val vstart = Input(UInt(log2Up(VLEN).W))
  val vm = Input(Bool())
  val mask = Input(UInt(VLEN.W))
  val din = Output(UInt(32.W))
  val ready = Output(Bool())
  val done = Output(Bool())
}
class VecLSUnitIO(memSize: Int) extends Bundle{
  val ram = new ClientIO(memSize)
  val core = new VecLSUnitCoreIO(memSize)
  val en = Input(Bool())
}
class VecLSUnit(memSize: Int) extends Module {
  val io = IO(new VecLSUnitIO(memSize))

  val addr = RegInit(0.U(log2Up(memSize).W))
  val write = RegInit(false.B)
  val cntOps = RegInit(0.U(log2Up(VLEN).W))
  val cntMaxOps = RegInit(0.U(log2Up(VLEN).W))
  val active = RegInit(false.B)

  io.ram.addr := addr
  io.ram.we := false.B
  io.ram.valid := io.en & active & io.core.valid //& (!io.core.we | !io.core.vm |  io.core.mask(cntOps))
  io.ram.dout := io.core.dout
  io.ram.memWidth := io.core.ew
  io.ram.burst := true.B
  io.core.din := io.ram.din
  io.core.ready := io.ram.ready
  io.core.done := false.B
  when(io.en){
    when(!active & io.core.valid){
      cntMaxOps := io.core.vl
      cntOps := 0.U
      addr := io.core.addr
      active := true.B
    } . elsewhen(active & io.core.valid){
      io.ram.we := io.core.we & (!io.core.vm | io.core.mask(cntOps))
      when(io.ram.ready){
        cntOps := cntOps + 1.U
        addr := addr + (1.U << io.core.ew).asUInt
        when(cntOps === (cntMaxOps - 1.U)){
          active := false.B
          io.core.done := true.B
        }
      }
    }
  }






}
