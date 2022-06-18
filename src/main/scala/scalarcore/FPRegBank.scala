package scalarcore

import chisel3._
import utility.Constants._

class FPRegBank extends Module{
    val io = IO(new Bundle {
        val rs1, rs2, rs3, rd = Input(UInt(5.W))
        val write = Input(Bool())
        val frd = Input(UInt(DATA_WIDTH.W))
        val frs1, frs2, frs3 = Output(UInt(DATA_WIDTH.W))
    })

    val f = Reg(Vec(32,UInt(DATA_WIDTH.W)))

    // Write to registers
    when(io.write){
        f(io.rd) := io.frd
    }

    val forwardRS1 = io.rd === io.rs1 && io.write
    val forwardRS2 = io.rd === io.rs2 && io.write
    val forwardRS3 = io.rd === io.rs3 && io.write

    // Read from registers
    io.frs1 := Mux(forwardRS1, io.frd, f(io.rs1))
    io.frs2 := Mux(forwardRS2, io.frd, f(io.rs2))
    io.frs3 := Mux(forwardRS3, io.frd, f(io.rs3))
}
