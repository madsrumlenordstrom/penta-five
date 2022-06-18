package scalarcore
import chisel3._
import utility.CoreIO
import utility.Functions.connect

class ScalarCore(memSize: Int, bSizeI: Int, linesI: Int, maxDelay: Int) extends Module{
  val io = IO(new CoreIO(memSize))

  val ftch = Module(new Fetch(memSize, bSizeI, linesI))
  val dec = Module(new Decode)
  val exe = Module(new Execute(maxDelay))
  val mem = Module(new Memory(memSize))
  val wb = Module(new WriteBack)
  val hazrd = Module(new HazardUnit(maxDelay))
  val fowrd = Module(new ForwardUnit(maxDelay))
  val activeInst = WireDefault(false.B)
  io.vec.busyOut := activeInst
  // Check if scalar pipeline has active instructions pending
  for(i <- 0 until maxDelay){
    when(exe.io.idexQueue(i).wbCtrl.active){
      activeInst := true.B
    }
  }
  when(exe.io.exmem.wbCtrl.active){
    activeInst := true.B
  }
  when(dec.io.idex.wbCtrl.active){
    activeInst := true.B
  }
  when(mem.io.memwb.wbCtrl.active){
    activeInst := true.B
  }

  // Connect fetch stage
  connect(io.clients(0).elements, ftch.io.ram.elements)
  ftch.io.branchPC := exe.io.branchPC
  ftch.io.setPC := exe.io.setPC
  ftch.io.flush := exe.io.setPC
  ftch.io.en := !(exe.io.stall | hazrd.io.stall | mem.io.stall | io.vec.haltFetch) & !(io.vec.busyIn & (dec.io.active | activeInst)) // If the vectorcore is processing instructions and the scalar is aswell, we must halt fetch
  io.vec.inst := ftch.io.vecInst
  // Connect decode stage
  dec.io.regData := wb.io.regData
  dec.io.regWE := wb.io.regWE
  dec.io.ifid := ftch.io.ifid
  dec.io.en := !(exe.io.stall | mem.io.stall)
  dec.io.flush := exe.io.setPC | io.vec.busyIn | (hazrd.io.stall & !exe.io.stall) // Send non active operations when the vetor core is busy
  dec.io.rd := wb.io.rd
  dec.io.vec.rs1 := io.vec.rs1
  dec.io.vec.rs2 := io.vec.rs2
  dec.io.vec.rd := io.vec.rd
  dec.io.vec.xrd := io.vec.xrd
  dec.io.vec.we := io.vec.we
  io.vec.xs1 := dec.io.vec.xs1
  io.vec.xs2 := dec.io.vec.xs2
  dec.io.vec.bankAccess := !io.vec.busyOut & io.vec.busyIn

  // Connect execution stage
  exe.io.idex := dec.io.idex
  exe.io.en := !(mem.io.stall)
  exe.io.forward.EX := fowrd.io.forward.EX
  exe.io.forward.MEMData := exe.io.exmem.data
  exe.io.forward.WBData := wb.io.regData

  // Connect memory stage
  connect(io.clients(1).elements, mem.io.ram.elements)
  mem.io.exmem := exe.io.exmem
  mem.io.en := true.B
  io.led := mem.io.led

  // Connect writeback stage
  wb.io.memwb := mem.io.memwb

  // Connect forwarding unit
  fowrd.io.idex := dec.io.idex
  fowrd.io.exmem := exe.io.exmem
  fowrd.io.memwb := mem.io.memwb

  // Connect hazard unit
  hazrd.io.ifid := dec.io.idexNext
  hazrd.io.idexQueue := exe.io.idexQueue
  hazrd.io.idex := dec.io.idex

  // CSR output
  io.csr := mem.io.csr

  // Memory misalignment
  io.fatal := ftch.io.fatal


}
