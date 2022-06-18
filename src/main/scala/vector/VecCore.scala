package vector
import chisel3._
import utility.Functions.connect
class VecCore(memSize: Int, maxDelay: Int) extends Module{
  val io = IO(new VecCoreIO(memSize, maxDelay))
  val dec = Module(new VecDecoder)
  val preExe = Module(new VecPreExe)
  val exe = Module(new VecExe(memSize, maxDelay))
  preExe.io.dec <> dec.io.ctrl
  connect(io.scalar.elements, preExe.io.scalar.elements)
  connect(io.ram.elements, exe.io.ram.elements)
  dec.io.inst := io.inst
  val en = io.en & exe.io.done
  dec.io.en := en
  preExe.io.en := en
  exe.io.en := io.en // Execute can stall the rest of the units, but can only be stalled by the scalar core.
  exe.io.preExe <> preExe.io.exe
  io.done := exe.io.done
  io.pending := preExe.io.pending | !exe.io.done | dec.io.pending
  io.haltFetch := !exe.io.done
}
