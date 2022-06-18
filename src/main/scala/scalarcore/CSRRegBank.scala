package scalarcore

import chisel3._
import chisel3.util._

import utility.Constants._

class CSRRegBank extends Module {
    val io = IO(new Bundle {
        val csrSrcDest = Input(UInt(log2Ceil(NUM_OF_CSR).W))
        val write = Input(Bool())
        val dataIn = Input(UInt(DATA_WIDTH.W))
        val dataOut = Output(UInt(DATA_WIDTH.W))
        val csr = Output(Vec(NUM_OF_CSR,UInt(DATA_WIDTH.W)))
    })

    val csr = Reg(Vec(NUM_OF_CSR,UInt(DATA_WIDTH.W)))

    // Write to registers
    when(io.write){
        csr(io.csrSrcDest) := io.dataIn
    }

    // Read from registers
    io.dataOut := csr(io.csrSrcDest)

    // Output register data
    io.csr := csr
}