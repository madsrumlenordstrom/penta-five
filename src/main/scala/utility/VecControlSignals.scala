package utility
import chisel3._
import chisel3.util._
object VecControlSignals {
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

    // Operations for third lane
    def DIV:    BitPat = BitPat("b010?")
    def DIVU:   BitPat = BitPat("b000?")
    def REM:    BitPat = BitPat("b011?")
    def REMU:   BitPat = BitPat("b001?")
    def X:      BitPat = BitPat("b????")
    def WIDTH:  Int = X.getWidth
  }
  object OPA{
    def VRS2:   BitPat = BitPat("b00")
    def VRS3:   BitPat = BitPat("b01")
    def XRS1:   BitPat = BitPat("b10")
    def IMM:    BitPat = BitPat("b11")
    def X:      BitPat = BitPat("b??")
    def WIDTH:  Int = X.getWidth
  }
  object OPB{
    def VRS1:   BitPat = BitPat("b00")
    def XRS1:   BitPat = BitPat("b01")
    def XRS2:   BitPat = BitPat("b10")
    def IMM:    BitPat = BitPat("b11")
    def X:      BitPat = BitPat("b??")
    def WIDTH:  Int = X.getWidth
  }
  object VM{
    def N:      BitPat = BitPat("b0")
    def Y:      BitPat = BitPat("b1")

    def X:      BitPat = BitPat("b?")
    def WIDTH:  Int = X.getWidth
  }
  object MEMRE{
    def N:      BitPat = BitPat("b0")
    def Y:      BitPat = BitPat("b1")

    def X:      BitPat = BitPat("b?")
    def WIDTH:  Int = X.getWidth
  }
  object MEMWE{
    def N:      BitPat = BitPat("b0")
    def Y:      BitPat = BitPat("b1")

    def X:      BitPat = BitPat("b?")
    def WIDTH:  Int = X.getWidth
  }
  object VSET{
    def N:      BitPat = BitPat("b0")
    def Y:      BitPat = BitPat("b1")

    def X:      BitPat = BitPat("b?")
    def WIDTH:  Int = X.getWidth
  }
  object EEW{
    def E8:     BitPat = BitPat("b000")
    def E16:    BitPat = BitPat("b001")
    def E32:    BitPat = BitPat("b010")

    def X:      BitPat = BitPat("b???")
    def WIDTH:  Int = X.getWidth
  }
  object REGWE{
    def N:      BitPat = BitPat("b00") // No writeback
    def INT:    BitPat = BitPat("b01") // Write to integer registers
    def FP:     BitPat = BitPat("b10") // Write to float registers
    def VEC:    BitPat = BitPat("b11")

    def X:      BitPat = BitPat("b??")
    def WIDTH:  Int = X.getWidth
  }
  object LANE{
    def LI:     BitPat = BitPat("b00") // Load and alu32i
    def LII:    BitPat = BitPat("b01") // Divide
    def LIII:   BitPat = BitPat("b10") // Multiply

    def X:      BitPat = BitPat("b??")
    def WIDTH:  Int = X.getWidth
  }
  object ACT{
    def N:      BitPat = BitPat("b0")
    def Y:      BitPat = BitPat("b1")
    def WIDTH:  Int = X.getWidth
  }
}
