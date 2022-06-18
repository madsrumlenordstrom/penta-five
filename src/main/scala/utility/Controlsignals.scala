package utility

import chisel3._
import chisel3.util._
import firrtl.getWidth

object Controlsignals {

    // Generic true false signals
    def Y:      BitPat = BitPat("b1") // True
    def N:      BitPat = BitPat("b0") // False
    def X:      BitPat = BitPat("b?") // Don't care
    
    object OP {
        // Specify what operation execute should perform

        // Operations for first lane
        def ADD:    BitPat = BitPat("b0000")
        def SUB:    BitPat = BitPat("b0001")
        def AND:    BitPat = BitPat("b0010")
        def OR:     BitPat = BitPat("b0011")
        def XOR:    BitPat = BitPat("b0100")
        def SLT:    BitPat = BitPat("b0101")
        def SLL:    BitPat = BitPat("b0110")
        def SLTU:   BitPat = BitPat("b0111")
        def SRL:    BitPat = BitPat("b1000")
        def SRA:    BitPat = BitPat("b1001")
        //def B:      BitPat = BitPat("b1010") // WTF is this?
        def SEQ:    BitPat = BitPat("b1010")
        def SNE:    BitPat = BitPat("b1011")
        def SLE:    BitPat = BitPat("b1100")
        def SLEU:   BitPat = BitPat("b1101")
        def SGT:    BitPat = BitPat("b1110")
        def SGTU:   BitPat = BitPat("b1111")

        
        // Operations for second lane
        def MUL:    BitPat = BitPat("b0000")
        def MULH:   BitPat = BitPat("b0001")
        def MULHSU: BitPat = BitPat("b0010")
        def MULHU:  BitPat = BitPat("b0011")
        def FMUL:   BitPat = BitPat("b0100")

        def FMADD:  BitPat = BitPat("b0011") // TODO
        def FMSUB:  BitPat = BitPat("b0011") // TODO
        def FNMADD: BitPat = BitPat("b0011") // TODO
        def FNMSUB: BitPat = BitPat("b0011") // TODO

        def FADD_S: BitPat = BitPat("b0000") // Can probably be mostly dont cares
        def FSUB_S: BitPat = BitPat("b0001") // Can probably be mostly dont cares

        // Operations for third lane
        def DIV:    BitPat = BitPat("b010?")
        def DIVU:   BitPat = BitPat("b000?")
        def REM:    BitPat = BitPat("b011?")
        def REMU:   BitPat = BitPat("b001?")

        def FDIV:   BitPat = BitPat("b1???")

        def FSGNJ_S:BitPat = BitPat("b??00")
        def FSGNJN_S:BitPat = BitPat("b??01")
        def FSGNJX_S:BitPat = BitPat("b??10")

        def FMAX:   BitPat = BitPat("b?000")
        def FMIN:   BitPat = BitPat("b?001")
        def FEQ:    BitPat = BitPat("b?010")
        def FLT:    BitPat = BitPat("b?011")
        def FLE:    BitPat = BitPat("b?100")

        def FCVT_W_S:BitPat = BitPat("b??00")
        def FCVT_WU_S:BitPat = BitPat("b??10")
        def FCVT_S_W:BitPat = BitPat("b??01")
        def FCVT_S_WU:BitPat = BitPat("b??11")

        def X:      BitPat = BitPat("b????")
        def WIDTH:  Int = X.getWidth
    }

    object OPA {
        // Specifies what goes into execute
        def ZERO:   BitPat = BitPat("b00")
        def XRS1:   BitPat = BitPat("b01")
        def PC:     BitPat = BitPat("b10")
        def FRS1:   BitPat = BitPat("b11")

        def X:      BitPat = BitPat("b??")
        def WIDTH:  Int = X.getWidth
    }

    object OPB {
        // Specifies what goes into execute
        def XRS2:   BitPat = BitPat("b00")
        def IMM:    BitPat = BitPat("b01")
        def FRS2:   BitPat = BitPat("b10")
        def ZERO:   BitPat = BitPat("b11")

        def X:      BitPat = BitPat("b??")
        def WIDTH:  Int = X.getWidth
    }

    object OPC {
        // Specifies what goes into execute
        def XRS2:   BitPat = BitPat("b00")
        def FRS3:   BitPat = BitPat("b01")
        def FRS2:   BitPat = BitPat("b10")
        def IMM:    BitPat = BitPat("b11")

        def X:      BitPat = BitPat("b??")
        def WIDTH:  Int = X.getWidth
    }

    object Imm {
        // Immediate formats
        def I:      BitPat = BitPat("b000")
        def S:      BitPat = BitPat("b001")
        def B:      BitPat = BitPat("b010")
        def U:      BitPat = BitPat("b011")
        def J:      BitPat = BitPat("b100")

        def X:      BitPat = BitPat("b???")
        def WIDTH:  Int = X.getWidth
    }

    object Delay {
        // Data delay from execution stage to memory stage

        val maxWidth = log2Up(54 + 1)
        def WIDTH:      Int = maxWidth

        def ZERO:       BitPat = BitPat(0.U(maxWidth.W))
        def ONE:        BitPat = BitPat(1.U(maxWidth.W))
        def FOUR:       BitPat = BitPat(4.U(maxWidth.W))


        def LUI:        BitPat = BitPat(0.U(maxWidth.W))
        def AUIPC:      BitPat = BitPat(0.U(maxWidth.W))
        def JAL:        BitPat = BitPat(0.U(maxWidth.W))
        def JALR:       BitPat = BitPat(0.U(maxWidth.W))
        def BEQ:        BitPat = BitPat(0.U(maxWidth.W))
        def BNE:        BitPat = BitPat(0.U(maxWidth.W))
        def BLT:        BitPat = BitPat(0.U(maxWidth.W))
        def BGE:        BitPat = BitPat(0.U(maxWidth.W))
        def BLTU:       BitPat = BitPat(0.U(maxWidth.W))
        def BGEU:       BitPat = BitPat(0.U(maxWidth.W))
        def LB:         BitPat = BitPat(0.U(maxWidth.W))
        def LH:         BitPat = BitPat(0.U(maxWidth.W))
        def LW:         BitPat = BitPat(0.U(maxWidth.W))
        def LBU:        BitPat = BitPat(0.U(maxWidth.W))
        def LHU:        BitPat = BitPat(0.U(maxWidth.W))
        def SB:         BitPat = BitPat(0.U(maxWidth.W))
        def SH:         BitPat = BitPat(0.U(maxWidth.W))
        def SW:         BitPat = BitPat(0.U(maxWidth.W))
        def ADDI:       BitPat = BitPat(0.U(maxWidth.W))
        def SLTI:       BitPat = BitPat(0.U(maxWidth.W))
        def SLTIU:      BitPat = BitPat(0.U(maxWidth.W))
        def XORI:       BitPat = BitPat(0.U(maxWidth.W))
        def ORI:        BitPat = BitPat(0.U(maxWidth.W))
        def ANDI:       BitPat = BitPat(0.U(maxWidth.W))
        def SLLI:       BitPat = BitPat(0.U(maxWidth.W))
        def SRLI:       BitPat = BitPat(0.U(maxWidth.W))
        def SRAI:       BitPat = BitPat(0.U(maxWidth.W))
        def ADD:        BitPat = BitPat(0.U(maxWidth.W))
        def SUB:        BitPat = BitPat(0.U(maxWidth.W))
        def SLL:        BitPat = BitPat(0.U(maxWidth.W))
        def SLT:        BitPat = BitPat(0.U(maxWidth.W))
        def SLTU:       BitPat = BitPat(0.U(maxWidth.W))
        def XOR:        BitPat = BitPat(0.U(maxWidth.W))
        def SRL:        BitPat = BitPat(0.U(maxWidth.W))
        def SRA:        BitPat = BitPat(0.U(maxWidth.W))
        def OR:         BitPat = BitPat(0.U(maxWidth.W))
        def AND:        BitPat = BitPat(0.U(maxWidth.W))
        def FENCE:      BitPat = BitPat(0.U(maxWidth.W))
        def ECALL:      BitPat = BitPat(0.U(maxWidth.W))
        def EBREAK:     BitPat = BitPat(0.U(maxWidth.W))
        def CSRRW:      BitPat = BitPat(0.U(maxWidth.W))
        def CSRRS:      BitPat = BitPat(0.U(maxWidth.W))
        def CSRRC:      BitPat = BitPat(0.U(maxWidth.W))
        def CSRRWI:     BitPat = BitPat(0.U(maxWidth.W))
        def CSRRSI:     BitPat = BitPat(0.U(maxWidth.W))
        def CSRRCI:     BitPat = BitPat(0.U(maxWidth.W))
        def MUL:        BitPat = BitPat(3.U(maxWidth.W)) // Uncertain
        def MULH:       BitPat = BitPat(3.U(maxWidth.W)) // Uncertain
        def MULHSU:     BitPat = BitPat(3.U(maxWidth.W)) // Uncertain
        def MULHU:      BitPat = BitPat(3.U(maxWidth.W)) // Uncertain
        def DIV:        BitPat = BitPat(52.U(maxWidth.W))
        def DIVU:       BitPat = BitPat(52.U(maxWidth.W))
        def REM:        BitPat = BitPat(52.U(maxWidth.W))
        def REMU:       BitPat = BitPat(52.U(maxWidth.W))
        def FLW:        BitPat = BitPat(0.U(maxWidth.W))
        def FSW:        BitPat = BitPat(0.U(maxWidth.W))
        def FMADD_S:    BitPat = BitPat(10.U(maxWidth.W)) // Uncertain
        def FMSUB_S:    BitPat = BitPat(10.U(maxWidth.W)) // Uncertain
        def FNMSUB_S:   BitPat = BitPat(10.U(maxWidth.W)) // Uncertain
        def FNMADD_S:   BitPat = BitPat(10.U(maxWidth.W)) // Uncertain
        def FADD_S:     BitPat = BitPat(5.U(maxWidth.W))
        def FSUB_S:     BitPat = BitPat(5.U(maxWidth.W))
        def FMUL_S:     BitPat = BitPat(10.U(maxWidth.W)) // Uncertain
        def FDIV_S:     BitPat = BitPat(54.U(maxWidth.W))
        def FSQRT_S:    BitPat = BitPat(54.U(maxWidth.W))
        def FSGNJ_S:    BitPat = BitPat(0.U(maxWidth.W))
        def FSGNJN_S:   BitPat = BitPat(0.U(maxWidth.W))
        def FSGNJX_S:   BitPat = BitPat(0.U(maxWidth.W))
        def FMIN_S:     BitPat = BitPat(0.U(maxWidth.W))
        def FMAX_S:     BitPat = BitPat(0.U(maxWidth.W))
        def FCVT_W_S:   BitPat = BitPat(2.U(maxWidth.W))
        def FCVT_WU_S:  BitPat = BitPat(2.U(maxWidth.W))
        def FMV_X_W:    BitPat = BitPat(0.U(maxWidth.W))
        def FEQ_S:      BitPat = BitPat(0.U(maxWidth.W))
        def FLT_S:      BitPat = BitPat(0.U(maxWidth.W))
        def FLE_S:      BitPat = BitPat(0.U(maxWidth.W))
        def FCLASS_S:   BitPat = BitPat(0.U(maxWidth.W))
        def FCVT_S_W:   BitPat = BitPat(3.U(maxWidth.W))
        def FCVT_S_WU:  BitPat = BitPat(3.U(maxWidth.W))
        def FMV_W_X:    BitPat = BitPat(0.U(maxWidth.W))

    }

    object Lane {
        // Specifies the lane used for execution
        // Note that all instructions which use the rounding unit has the fourth bit set.
        def LI:      BitPat = BitPat("b0000") // integer alu
        def LII:     BitPat = BitPat("b0001") // integer multiply
        def LIII:    BitPat = BitPat("b0010") // integer divide
        def LIV:     BitPat = BitPat("b0011") // float convert to integer
        def LV:      BitPat = BitPat("b0100") // float classify
        def LVI:     BitPat = BitPat("b0101") // float compare and min/max
        def LVII:    BitPat = BitPat("b0110") // float sign injection
        def LVIII:   BitPat = BitPat("b0111") // Not used.
        def LIX:     BitPat = BitPat("b1000") // float fused multiply and add/sub (uses rounding unit)
        def LX:      BitPat = BitPat("b1001") // integer convert to float
        def LXI:     BitPat = BitPat("b1010") // float multiply
        def LXII:    BitPat = BitPat("b1011") // float divide
        def LXIII:   BitPat = BitPat("b1100") // float add
        def LXIV:    BitPat = BitPat("b1101") // float square root
        def LXV:     BitPat = BitPat("b1110")
        def LXVI:    BitPat = BitPat("b1111")
        // Lanes to select from (final select)
        // integer alu
        // integer divide
        // integer multiply
        // float convert to integer
        // float class
        // float compare
        // float sign injection
        // float fused multiply and add (fpround)
        // integer convert to float (fpround)
        // float multiply (fpround)
        // float divide (fpround)
        // float add (fpround)
        // float multiply (fpround)
        // float square root (fpround)



        def X:      BitPat = BitPat("b????")
        def WIDTH:  Int = X.getWidth
    }

    object MemData {
        // Signals for memory stores
        def B:     BitPat = BitPat("b000")
        def BU:    BitPat = BitPat("b001")
        def H:     BitPat = BitPat("b010")
        def HU:    BitPat = BitPat("b011")
        def W:     BitPat = BitPat("b100")

        def X:     BitPat = BitPat("b???")
        def WIDTH:  Int = X.getWidth
    }

    object MemWE {
        // Write enable for memory
        def N:      BitPat = BitPat("b0")
        def Y:      BitPat = BitPat("b1")

        def X:      BitPat = BitPat("b?")
        def WIDTH:  Int = X.getWidth
    }

    object MemRE {
        // Write enable for memory
        def N:      BitPat = BitPat("b0")
        def Y:      BitPat = BitPat("b1")

        def X:      BitPat = BitPat("b?")
        def WIDTH:  Int = X.getWidth
    }

    // Specify what data should be written to register bank
    object WB {
        def ALU:    BitPat = BitPat("b00")
        def MEM:    BitPat = BitPat("b01")
        def PC4:    BitPat = BitPat("b10")
        def CSR:    BitPat = BitPat("b11")

        def X:      BitPat = BitPat("b??")
        def WIDTH:  Int = X.getWidth
    }

    // Specify which bank to write to
    object RegWE {
        // Write back signals for register banks
        def N:      BitPat = BitPat("b00") // No writeback
        def INT:    BitPat = BitPat("b01") // Write to integer registers
        def FP:     BitPat = BitPat("b10") // Write to float registers
        def CSR:    BitPat = BitPat("b11") // Write to CSR registers

        def X:      BitPat = BitPat("b??")
        def WIDTH:  Int = X.getWidth
    }

    object Jump {
        def N:      BitPat = BitPat("b000")
        def BEQ:    BitPat = BitPat("b011")
        def BNE:    BitPat = BitPat("b001")
        def BLT:    BitPat = BitPat("b100")
        def BGE:    BitPat = BitPat("b101")
        def BLTU:   BitPat = BitPat("b110")
        def BGEU:   BitPat = BitPat("b111")
        def JALR:   BitPat = BitPat("b010")
        /*
        def BEQ:    BitPat = BitPat("b000")
        def BNE:    BitPat = BitPat("b001")
        def BLT:    BitPat = BitPat("b100")
        def BGE:    BitPat = BitPat("b101")
        def BLTU:   BitPat = BitPat("b110")
        def BGEU:   BitPat = BitPat("b111")
        def JALR:   BitPat = BitPat("b010")
        def N:      BitPat = BitPat("b011")

         */

        def X:      BitPat = BitPat("b???")
        def WIDTH:  Int = X.getWidth
    }
    object ACT{
        def N:      BitPat = BitPat("b0")
        def Y:      BitPat = BitPat("b1")
        def X:      BitPat = BitPat("b?")
        def WIDTH:  Int = X.getWidth
    }
}
