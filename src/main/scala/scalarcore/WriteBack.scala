package scalarcore

import chisel3._
import chisel3.util._

import utility._
import utility.Controlsignals._

class WriteBack extends Module {
    val io = IO(new WriteBackIO)

    // Send write enable signal to register banks
    io.regWE := io.memwb.wbCtrl.regWE

    io.rd := io.memwb.rd

    // Select data to be written back to register bank
    io.regData := MuxCase(DontCare, Seq(
        (io.memwb.wbCtrl.selWB === WB.ALU) -> (io.memwb.aluData),
        (io.memwb.wbCtrl.selWB === WB.MEM) -> (io.memwb.memData),
        (io.memwb.wbCtrl.selWB === WB.PC4) -> (io.memwb.pcPlusFour),
        (io.memwb.wbCtrl.selWB === WB.CSR) -> (io.memwb.csrData),
    ))

}
