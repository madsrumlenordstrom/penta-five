package utility
import chisel3._
import chisel3.util._

object Constants {
    val DATA_WIDTH = 32
    val ADDR_WIDTH = 32
    val INST_WIDTH = 32
    val INST_MEM_ENTRIES = 1024

    val NUM_OF_CSR = 2

    val XLEN             = 32
    val MXLEN            = 32
    val MXL              = 1
    val VLEN             = 128 // Bits in one vector register.
    val ELEN             = 32 //

    val mf8              = "b101".U
    val mf4              = "b110".U
    val mf2              = "b111".U
    val m1               = "b000".U
    val m2               = "b001".U
    val m4               = "b010".U
    val m8               = "b011".U
    val e8               = "b000".U
    val e16              = "b001".U
    val e32              = "b010".U

    val POSINF           = "b01111111100000000000000000000000".U
    val NEGINF           = "b11111111100000000000000000000000".U
    val INF              = BitPat("b?11111111000000000000000000000000")
    val SNAN             = BitPat("b?111111111???????????????????????")
    val QNAN             = BitPat("b?111111110???????????????????????")
    val NAN              = BitPat("b?11111111????????????????????????")
    val CNAN             = "b01111111110000000000000000000000".U
    val POSZR            = "b00000000000000000000000000000000".U
    val NEGZR            = "b10000000000000000000000000000000".U
    val POSSUBNORM       = BitPat("b100000000????????????????????????")
    val NEGSUBNORM       = BitPat("b000000000????????????????????????")
    val SUBNORM          = BitPat("b?00000000????????????????????????")

    val FORWARD_NO  = "b00".U
    val FORWARD_MEM = "b01".U
    val FORWARD_WB  = "b10".U

    val BYTE = "b00".U
    val HWORD = "b01".U
    val WORD = "b10".U


    val DIVOP = new Bundle {
        val INT = new Bundle {
            val DIVUBYTE     = "b00000".U(5.W)
            val DIVBYTE      = "b00001".U(5.W)
            val DIVUHWORD    = "b01000".U(5.W)
            val DIVHWORD     = "b01001".U(5.W)
            val DIVUWORD     = "b10000".U(5.W)
            val DIVWORD      = "b10001".U(5.W)
            val REMUBYTE     = "b00010".U(5.W)
            val REMBYTE      = "b00011".U(5.W)
            val REMUHWORD    = "b01010".U(5.W)
            val REMHWORD     = "b01011".U(5.W)
            val REMUWORD     = "b10010".U(5.W)
            val REMWORD      = "b10011".U(5.W)
        }
        val FLOAT = new Bundle {
            val DIV          = "b00100".U
        }
    }
    val CONVOP = new Bundle {
        val FCVTWS         = "b00".U // Float to signed int
        val FCVTSW         = "b01".U // Signed int to float
        val FCVTWUS        = "b10".U // Float to unsigned int
        val FCVTSWU        = "b11".U // Unsigned int to float
    }
    val OP32I = new Bundle{
        val ADD            = "b0000".U
        val SUB            = "b0001".U
        val SLL            = "b0010".U
        val XOR            = "b0011".U
        val SRL            = "b0100".U
        val SRA            = "b0101".U
        val OR             = "b0110".U
        val AND            = "b0111".U
        val SLT            = "b1000".U
        val SLTU           = "b1001".U
    }
    val EXE_DELAY = new Bundle{
        val LUI            = 0.U
        val AUIPC          = 0.U
        val JAL            = 0.U
        val JALR           = 0.U
        val BEQ            = 0.U
        val BNE            = 0.U
        val BLT            = 0.U
        val BGE            = 0.U
        val BLTU           = 0.U
        val BGEU           = 0.U
        val LB             = 0.U
        val LH             = 0.U
        val LW             = 0.U
        val LBU            = 0.U
        val LHU            = 0.U
        val SB             = 0.U
        val SH             = 0.U
        val SW             = 0.U
        val ADDI           = 0.U
        val SLTI           = 0.U
        val SLTIU          = 0.U
        val XORI           = 0.U
        val ORI            = 0.U
        val ANDI           = 0.U
        val SLLI           = 0.U
        val SRLI           = 0.U
        val SRAI           = 0.U
        val ADD            = 0.U
        val SUB            = 0.U
        val SLL            = 0.U
        val SLT            = 0.U
        val SLTU           = 0.U
        val XOR            = 0.U
        val SRL            = 0.U
        val SRA            = 0.U
        val OR             = 0.U
        val AND            = 0.U
        val FENCE          = 0.U
        val ECALL          = 0.U
        val EBREAK         = 0.U
        val CSRRW          = 0.U
        val CSRRS          = 0.U
        val CSRRC          = 0.U
        val CSRRWI         = 0.U
        val CSRRSI         = 0.U
        val CSRRCI         = 0.U
        val MUL            = 4.U // Uncertain
        val MULH           = 4.U // Uncertain
        val MULHSU         = 4.U // Uncertain
        val MULHU          = 4.U // Uncertain
        val DIV            = 33.U // Uncertain
        val DIVU           = 33.U // Uncertain
        val REM            = 33.U // Uncertain
        val REMU           = 33.U // Uncertain
        val FLW            = 0.U
        val FSW            = 0.U
        val FMADD_S        = 10.U // Uncertain
        val FMSUB_S        = 10.U // Uncertain
        val FNMSUB_S       = 10.U // Uncertain
        val FNMADD_S       = 10.U // Uncertain
        val FADD_S         = 10.U // Uncertain
        val FSUB_S         = 10.U // Uncertain
        val FMUL_S         = 10.U // Uncertain
        val FDIV_S         = 50.U // Uncertain
        val FSQRT_S        = 24.U // Uncertain
        val FSGNJ_S        = 0.U
        val FSGNJN_S       = 0.U
        val FSGNJX_S       = 0.U
        val FMIN_S         = 0.U
        val FMAX_S         = 0.U
        val FCVT_W_S       = 1.U
        val FCVT_WU_S      = 1.U
        val FMV_X_W        = 0.U
        val FEQ_S          = 0.U
        val FLT_S          = 0.U
        val FLE_S          = 0.U
        val FCLASS_S       = 0.U
        val FCVT_S_W       = 1.U
        val FCVT_S_WU      = 1.U
        val FMV_W_X        = 0.U
    }
    val INST_MEM_SIZE = 4096
}

