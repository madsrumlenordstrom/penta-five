package scalarcore

import chisel3._
import chisel3.util._

import utility.Constants._

class BranchPredictor extends Module {
    val io = IO(new Bundle{
        val inst = Input(UInt(INST_WIDTH.W))
        val addr = Input(UInt(ADDR_WIDTH.W))
        val branch = Output(Bool())
    })

    val entries = 256

    val notTaken1 :: notTaken0 :: taken0 :: taken1 :: Nil = Enum(4)
    val predictionTable = RegInit(VecInit.fill(entries)(notTaken0))

    val branchOpcode = "b1100011".U
    val isBranchInst = Wire(io.inst(6,0) === branchOpcode)

    val index = io.addr(log2Up(entries - 1) - 1,0)

    // Fill(20,io.inst(31)) ## io.inst(7) ## io.inst(30,25) ## io.inst(11,8) ## 0.U(1.W)
  
}
