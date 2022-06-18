package scalarcore

import chisel3._
import chisel3.util._
import utility.Controlsignals.Imm
import utility.Constants._

class ImmGen extends Module {
    val io = IO(new Bundle{
        val inst = Input(UInt(INST_WIDTH.W))
        val immType = Input(UInt(Imm.WIDTH.W))
        val immRes = Output(UInt(DATA_WIDTH.W))
    })

    io.immRes := MuxCase(DontCare, Seq(
            (io.immType === Imm.I) -> Fill(21,io.inst(31)) ## io.inst(30,25) ## io.inst(24,21) ## io.inst(20),
            (io.immType === Imm.S) -> Fill(21,io.inst(31)) ## io.inst(30,25) ## io.inst(11,8)  ## io.inst(7),
            (io.immType === Imm.B) -> Fill(20,io.inst(31)) ## io.inst(7)     ## io.inst(30,25) ## io.inst(11,8)  ## 0.U(1.W),
            (io.immType === Imm.U) -> io.inst(31)          ## io.inst(30,20) ## io.inst(19,12) ## 0.U(12.W),
            (io.immType === Imm.J) -> Fill(12,io.inst(31)) ## io.inst(19,12) ## io.inst(20)    ## io.inst(30,25) ## io.inst(24,21) ## 0.U(1.W), // TODO maybe remove?
        )
    )
}