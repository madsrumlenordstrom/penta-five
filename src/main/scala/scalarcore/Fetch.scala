package scalarcore

import chisel3._
import chisel3.util._

import utility._
import utility.Constants._
import utility.Instructions._
import memory._
import utility.Functions.connect

import chisel3.util.experimental.loadMemoryFromFile

class InstructionMemory(entries : Int, width : Int, program : String) extends Module{
  val io = IO(new Bundle{
    val address = Input(UInt(log2Ceil(entries + 1).W))
    val instOut = Output(UInt(width.W))
  })

  val instMem = SyncReadMem(entries, UInt(width.W))
  io.instOut := instMem.read(io.address)

  // Program is loaded to instruction memory
  loadMemoryFromFile(instMem, program)
}

class Fetch(memSize: Int, bSizeI: Int, linesI: Int) extends Module{
    val io = IO(new FetchIO(memSize))



    // Main pipeline register
    val ifidReg = RegInit(0.U.asTypeOf(new IFID))
    val ifidNext = WireInit(0.U.asTypeOf(new IFID))
    val pcNext = WireDefault(0.U)

    //val instMem = Module(new InstructionMemory(INST_MEM_ENTRIES, INST_WIDTH, "program.txt"))
    val iCache = Module(new ICache(bSizeI, linesI, memSize))

    val pc = RegInit(0.U(ADDR_WIDTH.W))

    val pcPlusFour = pc + 4.U

    //instMem.io.address := pc >> 2.U

    //val inst = instMem.io.instOut
    val inst = iCache.io.core.dout

    // Specific for JAL instruction
    val isJal = inst === JAL
    val immU = Fill(12,inst(31)) ## inst(19,12) ## inst(20) ## inst(30,25) ## inst(24,21) ## 0.U(1.W)
    
    // Specific for branch (TODO maybe move most of this to branch predictor)
    val isBranch = inst === BEQ || inst === BNE || inst === BLT || inst === BGE || inst === BLTU || inst === BGEU 
    val immB = Fill(20,inst(31)) ## inst(7) ## inst(30,25) ## inst(11,8) ## 0.U(1.W)
    val branch = false.B
    
    val stall = false.B // TEMP
    
    // Determine next PC
    pcNext := MuxCase( /* Default pc */ pcPlusFour, Seq(
      (io.setPC)            -> (io.branchPC(DATA_WIDTH - 1, 1) ## 0.U(1.W)), // Set LSB to 0
      (stall)               -> (pc),
      (isJal)               -> (pc.asSInt + immU.asSInt).asUInt,
      (isBranch && branch)  -> (pc.asSInt + immB.asSInt).asUInt,
    ))
    
    // Put into pipeline register
    /*
    ifidReg.pc := pc
    ifidReg.pcPlusFour := pcPlusFour
    ifidReg.inst := inst
    ifidReg.branched := branch

     */
    // Put into pipeline register
    ifidNext.pc := pc
    ifidNext.pcPlusFour := pcPlusFour
    ifidNext.inst := inst
    ifidNext.branched := branch

    // Connect cache
    connect(io.ram.elements, iCache.io.ram.elements)
    iCache.io.core.addr := pc
    io.fatal := iCache.io.core.fatal
    val nop = WireDefault(0.U.asTypeOf(new IFID))
    nop.inst :=  0x10000000.U
    val vecInst = RegInit(0.U(32.W))
    val nopVec = 0x00000000.U

    when(io.en){ //
      ifidReg := Mux(io.flush | !iCache.io.core.valid, nop, ifidNext) // Send nops if we flush or instruction isn't in cache
      vecInst := Mux(io.flush | !iCache.io.core.valid, nopVec, ifidNext.inst)
      pc := Mux(io.setPC, pcNext, Mux(iCache.io.core.valid, pcNext, pc)) // Update pc when pipeline isn't stalling and cache has correct data
    }
    // Output
    io.ifid := ifidReg
    io.vecInst := vecInst
}