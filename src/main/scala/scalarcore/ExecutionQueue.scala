package scalarcore
import chisel3._
import chisel3.util._
import utility.{EXMEM,MEMWB,IDEX}
class ExecutionQueue(maxDelay : Int) extends Module {
  val io = IO(new Bundle{
    val busy = Output(Bool())   // Is there space to enqueue the instruction
    val enqueue = Input(Bool()) // Insert new instruction in pipeline. We need to AND this with !execution.stall
    val selQueue = Output(Vec(maxDelay, UInt((log2Up(maxDelay) + 1).W)))
    val din = Input(new IDEX)
    val dataQueue = Output(Vec(maxDelay, new IDEX))
    val en = Input(Bool())
  })
  val selQueue = RegInit(VecInit(Seq.fill(maxDelay)(0.U((log2Up(maxDelay) + 1).W)))) // To select between execution lanes
  //val dataQueue = RegInit(Vec(maxDelay, 0.U.asTypeOf(new IDEX))) // Data to be forwarded to memory stage
  val dataQueue = RegInit(VecInit(Seq.fill(maxDelay)(0.U.asTypeOf(new IDEX)))) // Data to be forwarded to memory stage
  io.selQueue <> selQueue
  io.busy := (selQueue(io.din.exCtrl.delay) =/= 0.U) & io.enqueue
  io.dataQueue <> dataQueue
  when(io.en){
    for(i <- 0 until maxDelay - 1) {
      selQueue(i) := selQueue(i + 1)
      dataQueue(i) := dataQueue(i + 1)
    }
    when(io.enqueue & (selQueue(io.din.exCtrl.delay) === 0.U)){ // We can enqueue the instruction
      when(io.din.exCtrl.delay === 0.U){ // This we cannot load into the register, but rather just transfer it to the output directly
        io.selQueue(0) := io.din.exCtrl.lane
        io.dataQueue(0) <> io.din
      } . otherwise {
        selQueue(io.din.exCtrl.delay - 1.U) := io.din.exCtrl.lane
        dataQueue(io.din.exCtrl.delay - 1.U) <> io.din
      }
    }
  }
}
