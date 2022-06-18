/*package scalarcore

import chisel3._
import chisel3.util._

import utility._
import utility.Controlsignals._

class AltFetchIO(memSize: Int, maxBSize: Int) extends Bundle{
    val branchPC = Input(UInt(DATA_WIDTH.W))
    val setPC = Input(Bool())
    val ifid = Output(new IFID)
    val flush = Input(Bool())
    val en = Input(Bool())
}

class AltFetch extends Module{
    val io = IO(new AltFetchIO)

    // Main pipeline register
    val ifidReg = RegInit(0.U.asTypeOf(new IFID))
    val pcNext = WireDefault(0.U)

    val instMem = Module(new InstructionMemory(INST_MEM_ENTRIES, INST_WIDTH, "program.txt"))

    val pc = RegInit(0.U(ADDR_WIDTH.W))

    val pcPlusFour = pc + 4.U

    instMem.io.address := pc >> 2.U

    //val inst = instMem.io.instOut
    val inst = iCache.io.core.dout

    // Specific for JAL instruction
    val isJal = inst === JAL
    val immU = Fill(12,inst(31)) ## inst(19,12) ## inst(20) ## inst(30,25) ## inst(24,21) ## 0.U(1.W)
    
    // Specific for branch (TODO maybe move most of this to branch predictor)
    val isBranch = inst === BEQ || inst === BNE || inst === BLT || inst === BGE || inst === BLTU || inst === BGEU 
    val immB = Fill(20,inst(31)) ## inst(7) ## inst(30,25) ## inst(11,8) ## 0.U(1.W)
    val branch = false.B
    
    // Determine next PC
    pcNext := MuxCase( /* Default pc */ pcPlusFour, Seq(
      (io.setPC)            -> (io.branchPC(DATA_WIDTH - 1, 1) ## 0.U(1.W)), // Set LSB to 0
      (isJal)               -> (pc.asSInt + immU.asSInt).asUInt,
      (isBranch && branch)  -> (pc.asSInt + immB.asSInt).asUInt,
    ))
    
    // Put into pipeline register
    ifidReg.pc := pc
    ifidReg.pcPlusFour := pcPlusFour
    ifidReg.inst := inst
    ifidReg.branched := branch

    // Output
    io.ifid := ifidReg
}

class AltExecuteIO(maxDelay: Int) extends Bundle{
    val idex = Input(new IDEX)
    val exmem = Output(new EXMEM)
    val branchPC = Output(UInt(DATA_WIDTH.W))
    val setPC = Output(Bool())
    val stall = Output(Bool())
    val en = Input(Bool())
}

class AltExecute extends Module {
  val io = IO(new AltExecuteIO(0))

  val exmeReg = RegInit(0.U.asTypeOf(new EXMEM))

  val alu32i = Module(new ALU32I)

  alu32i.io.op := io.idex.exCtrl.opcode
  alu32i.io.a := io.idex.opa
  alu32i.io.b := io.idex.opb

  // Branching
  val eq = alu32i.io.eq
  val ne = !eq
  val lt = alu32i.io.lt
  val ge = !lt | eq
  val ltu = alu32i.io.ltu
  val geu = !ltu | eq

  val jumpType = io.idex.exCtrl.jumpType
  val branched = io.idex.branched

  // Check if branch was wrong or for JALR
  io.setPC := MuxCase(false.B, Seq(
    (jumpType === Jump.JALR)                      -> (true.B),
    (jumpType === Jump.BEQ  &&  eq  && !branched) -> (true.B),
    (jumpType === Jump.BEQ  && !eq  &&  branched) -> (true.B),
    (jumpType === Jump.BNE  &&  ne  && !branched) -> (true.B),
    (jumpType === Jump.BNE  && !ne  &&  branched) -> (true.B),
    (jumpType === Jump.BLT  &&  lt  && !branched) -> (true.B),
    (jumpType === Jump.BLT  && !lt  &&  branched) -> (true.B),
    (jumpType === Jump.BGE  &&  ge  && !branched) -> (true.B),
    (jumpType === Jump.BGE  && !ge  &&  branched) -> (true.B),
    (jumpType === Jump.BLTU &&  ltu && !branched) -> (true.B),
    (jumpType === Jump.BLTU && !ltu &&  branched) -> (true.B),
    (jumpType === Jump.BGEU &&  geu && !branched) -> (true.B),
    (jumpType === Jump.BGEU && !geu &&  branched) -> (true.B),
  ))

  // Set next PC
  io.branchPC := MuxCase(DontCare, Seq(
    (jumpType === Jump.JALR)                      -> (alu32i.io.y),
    (jumpType === Jump.BEQ  &&  eq  && !branched) -> (alu32i.io.y),
    (jumpType === Jump.BEQ  && !eq  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BNE  &&  ne  && !branched) -> (alu32i.io.y),
    (jumpType === Jump.BNE  && !ne  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BLT  &&  lt  && !branched) -> (alu32i.io.y),
    (jumpType === Jump.BLT  && !lt  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BGE  &&  ge  && !branched) -> (alu32i.io.y),
    (jumpType === Jump.BGE  && !ge  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BLTU &&  ltu && !branched) -> (alu32i.io.y),
    (jumpType === Jump.BLTU && !ltu &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BGEU &&  geu && !branched) -> (alu32i.io.y),
    (jumpType === Jump.BGEU && !geu &&  branched) -> (io.idex.pcPlusFour),
  ))
  
  exmeReg.data := alu32i.io.y
  exmeReg.addr := io.idex.opc
  exmeReg.pcPlusFour := io.idex.pcPlusFour
  
  exmeReg.rs1 := io.idex.rs1
  exmeReg.rs2 := io.idex.rs2
  exmeReg.rs3 := io.idex.rs3
  exmeReg.rd := io.idex.rd
  
  exmeReg.meCtrl := io.idex.meCtrl
  exmeReg.wbCtrl := io.idex.wbCtrl

  //temp
  io.stall := false.B

  io.exmem := exmeReg
}

class AltMemoryIO extends Bundle{
    val exmem = Input(new EXMEM)
    val memwb = Output(new MEMWB)
    val ram = new ClientIO(memSize, maxBSize)
    val stall = Output(Bool())
    val en = Input(Bool())
    val fatal = Output(Bool())
}

class AltMemory extends Module{
  val io = IO(new AltMemoryIO)

  val mem = Mem(512, UInt(DATA_WIDTH.W))


}

class Top extends Module {
  val io = IO(new Bundle{
    val done = Output(Bool())
  })

  val fetch = Module(new AltFetch(16,8,512,16))
  val decode = Module(new Decode)
  val execute = Module(new AltExecute)
  val memory = Module(new AltMemory)
  val writeBack = Module(new WriteBack)

  // Dont optimize away
  dontTouch(fetch.io)
  dontTouch(decode.io)
  dontTouch(execute.io)
  //dontTouch(memory.io)
  //dontTouch(writeBack.io)

  execute.io.idex := decode.io.idex
  
  // Fetch connections
  fetch.io.branchPC := execute.io.branchPC
  fetch.io.setPC := execute.io.setPC
  fetch.io.flush := execute.io.setPC
  fetch.io.en := true.B
  
  // Decode connections
  decode.io.regData := writeBack.io.regData
  decode.io.regWE := writeBack.io.regWE
  decode.io.rd := writeBack.io.rd
  decode.io.ifid := fetch.io.ifid
  decode.io.en := true.B
  decode.io.flush := execute.io.setPC

  // Execution connections
  execute.io.idex := decode.io.idex
  execute.io.en := true.B
  execute.io.forward := 0.U

  
  io.done := fetch.io.ifid.inst(0) && decode.io.idex.pc(31)
}

object Top extends App {
    println("Generating hardware")
    emitVerilog(new Top, args)
    println("Hardware successfully generated")
}
*/