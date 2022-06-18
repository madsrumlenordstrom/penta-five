package mdu

import chisel3._

object MDUConstants {

    // Instruction constants for MDU
    val MUL    = "b000".U(3.W)
    val MULH   = "b001".U(3.W)
    val MULHSU = "b010".U(3.W)
    val MULHU  = "b011".U(3.W)
    val DIV    = "b100".U(3.W)
    val DIVU   = "b101".U(3.W)
    val REM    = "b110".U(3.W)
    val REMU   = "b111".U(3.W)

    val FUNCT3_WIDTH = 3
}
