package scalarcore

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import utility.Instructions._
import utility.Controlsignals._
import utility.Constants._
import utility._

object ControlLogic{

    // Control table

    val defaultCtrlWord: Seq[BitPat] = Seq(OP.X     , OPA.X, OPB.X, OPC.X, Imm.X, Delay.ZERO, Lane.X, MemData.X, MemWE.N, MemRE.X, WB.X, RegWE.X, Jump.X, ACT.N)

    val ctrlTable: TruthTable = TruthTable(Map(
        //
        //
        //                 Operation                                   Immediate type                                    Memory write enable                  Jump type
        //                 |             Operand A                     |      Execution delay                            |        Memory read enable          |
        //                 |             |         Operand B           |      |                Execution lane            |        |        Write back data    |
        //                 |             |         |         Operand C |      |                |             Memory data |        |        |       Reg write enable
        //                 |             |         |         |         |      |                |             |           |        |        |       |          |
        LUI         -> Seq(OP.ADD      , OPA.ZERO, OPB.IMM , OPC.X   , Imm.U, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        AUIPC       -> Seq(OP.ADD      , OPA.PC  , OPB.IMM , OPC.X   , Imm.U, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        JAL         -> Seq(OP.ADD      , OPA.PC  , OPB.IMM , OPC.X   , Imm.J, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.PC4, RegWE.INT, Jump.N   , ACT.Y),
        JALR        -> Seq(OP.ADD      , OPA.PC  , OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.PC4, RegWE.INT, Jump.JALR, ACT.Y),
        BEQ         -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.IMM , Imm.B, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.N  , Jump.BEQ , ACT.Y),
        BNE         -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.IMM , Imm.B, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.N  , Jump.BNE , ACT.Y),
        BLT         -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.IMM , Imm.B, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.N  , Jump.BLT , ACT.Y),
        BGE         -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.IMM , Imm.B, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.N  , Jump.BGE , ACT.Y),
        BLTU        -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.IMM , Imm.B, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.N  , Jump.BLTU, ACT.Y),
        BGEU        -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.IMM , Imm.B, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.N  , Jump.BGEU, ACT.Y),
        LB          -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.B , MemWE.N, MemRE.Y, WB.MEM, RegWE.INT, Jump.N   , ACT.Y),
        LH          -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.H , MemWE.N, MemRE.Y, WB.MEM, RegWE.INT, Jump.N   , ACT.Y),
        LW          -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.W , MemWE.N, MemRE.Y, WB.MEM, RegWE.INT, Jump.N   , ACT.Y),
        LBU         -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.BU, MemWE.N, MemRE.Y, WB.MEM, RegWE.INT, Jump.N   , ACT.Y),
        LHU         -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.HU, MemWE.N, MemRE.Y, WB.MEM, RegWE.INT, Jump.N   , ACT.Y),
        SB          -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.XRS2, Imm.S, Delay.ZERO     , Lane.LI   , MemData.B , MemWE.Y, MemRE.N, WB.X  , RegWE.N  , Jump.N   , ACT.Y),
        SH          -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.XRS2, Imm.S, Delay.ZERO     , Lane.LI   , MemData.H , MemWE.Y, MemRE.N, WB.X  , RegWE.N  , Jump.N   , ACT.Y),
        SW          -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.XRS2, Imm.S, Delay.ZERO     , Lane.LI   , MemData.W , MemWE.Y, MemRE.N, WB.X  , RegWE.N  , Jump.N   , ACT.Y),
        ADDI        -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SLTI        -> Seq(OP.SLT      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SLTIU       -> Seq(OP.SLTU     , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        XORI        -> Seq(OP.XOR      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        ORI         -> Seq(OP.OR       , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        ANDI        -> Seq(OP.AND      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SLLI        -> Seq(OP.SLL      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SRLI        -> Seq(OP.SRL      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SRAI        -> Seq(OP.SRA      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        ADD         -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SUB         -> Seq(OP.SUB      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SLL         -> Seq(OP.SLL      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SLT         -> Seq(OP.SLT      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SLTU        -> Seq(OP.SLTU     , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        XOR         -> Seq(OP.XOR      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SRL         -> Seq(OP.SRL      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        SRA         -> Seq(OP.SRA      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        OR          -> Seq(OP.OR       , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        AND         -> Seq(OP.AND      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        MUL         -> Seq(OP.MUL      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.MUL      , Lane.LII  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        MULH        -> Seq(OP.MULH     , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.MULH     , Lane.LII  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        MULHSU      -> Seq(OP.MULHSU   , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.MULHSU   , Lane.LII  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        MULHU       -> Seq(OP.MULHU    , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.MULHU    , Lane.LII  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        DIV         -> Seq(OP.DIV      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.DIV      , Lane.LIII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        DIVU        -> Seq(OP.DIVU     , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.DIVU     , Lane.LIII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        REM         -> Seq(OP.REM      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.REM      , Lane.LIII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        REMU        -> Seq(OP.REMU     , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.REMU     , Lane.LIII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FLW         -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.X   , Imm.I, Delay.ZERO     , Lane.LI   , MemData.W , MemWE.N, MemRE.Y, WB.MEM, RegWE.FP , Jump.N   , ACT.Y),
        FSW         -> Seq(OP.ADD      , OPA.XRS1, OPB.IMM , OPC.FRS2, Imm.S, Delay.ZERO     , Lane.LI   , MemData.W , MemWE.Y, MemRE.N, WB.X  , RegWE.N  , Jump.N   , ACT.Y),
        FMADD_S     -> Seq(OP.FMADD    , OPA.FRS1, OPB.FRS2, OPC.FRS3, Imm.X, Delay.FMADD_S  , Lane.LIX  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FMSUB_S     -> Seq(OP.FMSUB    , OPA.FRS1, OPB.FRS2, OPC.FRS3, Imm.X, Delay.FMSUB_S  , Lane.LIX  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FNMSUB_S    -> Seq(OP.FNMADD   , OPA.FRS1, OPB.FRS2, OPC.FRS3, Imm.X, Delay.FNMADD_S , Lane.LIX  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FNMADD_S    -> Seq(OP.FNMSUB   , OPA.FRS1, OPB.FRS2, OPC.FRS3, Imm.X, Delay.FNMSUB_S , Lane.LIX  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FADD_S      -> Seq(OP.FADD_S   , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.FADD_S   , Lane.LXIII, MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FSUB_S      -> Seq(OP.FADD_S   , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.FADD_S   , Lane.LXIII, MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FMUL_S      -> Seq(OP.FMUL     , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.FMUL_S   , Lane.LXI  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FSQRT_S     -> Seq(OP.X        , OPA.FRS1, OPB.X   , OPC.X   , Imm.X, Delay.FSQRT_S  , Lane.LXIV , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FDIV_S      -> Seq(OP.FDIV     , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.FDIV_S   , Lane.LXII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FSGNJ_S     -> Seq(OP.FSGNJ_S  , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FSGNJN_S    -> Seq(OP.FSGNJN_S , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FSGNJX_S    -> Seq(OP.FSGNJX_S , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVII , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FMIN_S      -> Seq(OP.FMIN     , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVI  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FMAX_S      -> Seq(OP.FMAX     , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVI  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FCVT_W_S    -> Seq(OP.FCVT_W_S , OPA.FRS1, OPB.X   , OPC.X   , Imm.X, Delay.FCVT_W_S , Lane.LIV  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FCVT_WU_S   -> Seq(OP.FCVT_WU_S, OPA.FRS1, OPB.X   , OPC.X   , Imm.X, Delay.FCVT_WU_S, Lane.LIV  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FMV_X_W     -> Seq(OP.ADD      , OPA.FRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y), // XRS2 will always be xero therefore it is used along with int add
        FEQ_S       -> Seq(OP.FEQ      , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVI  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FLT_S       -> Seq(OP.FLT      , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVI  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FLE_S       -> Seq(OP.FLE      , OPA.FRS1, OPB.FRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LVI  , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y),
        FCLASS_S    -> Seq(OP.X        , OPA.FRS1, OPB.X   , OPC.X   , Imm.X, Delay.ZERO     , Lane.LV   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.INT, Jump.N   , ACT.Y), // Instruction has own lane therefor no opcode
        FCVT_S_W    -> Seq(OP.FCVT_S_W , OPA.FRS1, OPB.X   , OPC.X   , Imm.X, Delay.FCVT_S_W , Lane.LX   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FCVT_S_WU   -> Seq(OP.FCVT_S_WU, OPA.FRS1, OPB.X   , OPC.X   , Imm.X, Delay.FCVT_S_WU, Lane.LX   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        FMV_W_X     -> Seq(OP.ADD      , OPA.XRS1, OPB.XRS2, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.ALU, RegWE.FP , Jump.N   , ACT.Y),
        CSRR        -> Seq(OP.X        , OPA.X   , OPB.X   , OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.CSR, RegWE.INT, Jump.N   , ACT.Y), // Pseudo instruction
        CSRW        -> Seq(OP.ADD      , OPA.XRS1, OPB.ZERO, OPC.X   , Imm.X, Delay.ZERO     , Lane.LI   , MemData.X , MemWE.N, MemRE.N, WB.X  , RegWE.CSR, Jump.N   , ACT.Y), // Pseudo instruction (this instruction is cursed)

        //CSRRW       -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //CSRRS       -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //CSRRC       -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //CSRRWI      -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //CSRRSI      -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //CSRRCI      -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        
        //ECALL       -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //FENCE       -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        //EBREAK      -> Seq(Y, X       , X           , X       , X       , X       , X        ),
        ).map({case (k, v) => k -> v.reduce(_ ## _)}), defaultCtrlWord.reduce(_ ## _))

}

class Decode extends Module{

    val io = IO(new DecodeIO)

    // Main pipeline register
    val idexReg = RegInit(0.U.asTypeOf(new IDEX))
    val idexNext = WireInit(0.U.asTypeOf(new IDEX))

    // Control word derived from truthtable
    val ctrlWord: UInt = decoder(minimizer = QMCMinimizer, input = io.ifid.inst, truthTable = ControlLogic.ctrlTable)

    // Just used for cleaner code (see below)
    val ctrlWordWidth = ctrlWord.getWidth
    var ctrlOffset = 0

    // Divide controlword into smaller parts for cleaner code
    val opcode = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OP.WIDTH)
    ctrlOffset = ctrlOffset + OP.WIDTH
    val operandA = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OPA.WIDTH)
    ctrlOffset = ctrlOffset + OPA.WIDTH
    val operandB = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OPB.WIDTH)
    ctrlOffset = ctrlOffset + OPB.WIDTH
    val operandC = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OPC.WIDTH)
    ctrlOffset = ctrlOffset + OPC.WIDTH
    val immType = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - Imm.WIDTH)
    ctrlOffset = ctrlOffset + Imm.WIDTH
    val delay = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - Delay.WIDTH)
    ctrlOffset = ctrlOffset + Delay.WIDTH
    val lane = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - Lane.WIDTH)
    ctrlOffset = ctrlOffset + Lane.WIDTH
    val memData = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - MemData.WIDTH)
    ctrlOffset = ctrlOffset + MemData.WIDTH
    val memWE = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - MemWE.WIDTH)
    ctrlOffset = ctrlOffset + MemWE.WIDTH
    val memRE = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - MemRE.WIDTH)
    ctrlOffset = ctrlOffset + MemRE.WIDTH
    val selWB = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - WB.WIDTH)
    ctrlOffset = ctrlOffset + WB.WIDTH
    val regWE = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - RegWE.WIDTH)
    ctrlOffset = ctrlOffset + RegWE.WIDTH
    val jumpType = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - Jump.WIDTH)
    ctrlOffset = ctrlOffset + Jump.WIDTH
    val active = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - ACT.WIDTH)
    ctrlOffset = ctrlOffset + ACT.WIDTH

    // Control signal bundles
    val exCtrl = Wire(new EXControl)
    val meCtrl = Wire(new MEControl)
    val wbCtrl = Wire(new WBControl)

    // Execution control signals
    exCtrl.opcode := opcode
    exCtrl.lane := lane
    exCtrl.delay := delay
    exCtrl.jumpType := jumpType
    exCtrl.opa := operandA
    exCtrl.opb := operandB
    exCtrl.opc := operandC

    // Memory control signals
    meCtrl.memData := memData
    meCtrl.memRE := memRE
    meCtrl.memWE := memWE

    // Write back control signals
    wbCtrl.selWB := selWB
    wbCtrl.regWE := regWE
    wbCtrl.active := active(0).asBool // It is really just a 1 bit UInt
    io.active := wbCtrl.active

    // Divide instruction into smaller pieces
    val rs1, rs2, rs3, rd = Wire(UInt(5.W))
    rs1 := io.ifid.inst(19,15)
    rs2 := io.ifid.inst(24,20)
    rs3 := io.ifid.inst(31,27)
    rd := io.ifid.inst(11,7)
    val csrSrcDest = Wire(UInt(log2Ceil(NUM_OF_CSR).W))
    csrSrcDest := io.ifid.inst(log2Ceil(NUM_OF_CSR) - 1 + 20, 20)

    // Register bank holding integers
    val intRegBank = Module(new IntRegBank)
    intRegBank.io.rs1 := rs1
    intRegBank.io.rs2 := rs2
    intRegBank.io.rd := io.rd//rd
    intRegBank.io.xrd := io.regData
    intRegBank.io.write := io.regWE === RegWE.INT
    io.vec.xs1 := intRegBank.io.xrs1
    io.vec.xs2 := intRegBank.io.xrs2
    when(io.vec.bankAccess){
        intRegBank.io.rs1 := io.vec.rs1
        intRegBank.io.rs2 := io.vec.rs2
        intRegBank.io.rd := io.vec.rd
        intRegBank.io.xrd := io.vec.xrd
        intRegBank.io.write := io.vec.we
    }

    // Register bank holding floats
    val fpRegBank = Module(new FPRegBank)
    fpRegBank.io.rs1 := rs1
    fpRegBank.io.rs2 := rs2
    fpRegBank.io.rs3 := rs3
    fpRegBank.io.rd := io.rd//rd
    fpRegBank.io.frd := io.regData
    fpRegBank.io.write := io.regWE === RegWE.FP

    // Immediate generator
    val immGen = Module(new ImmGen)
    immGen.io.inst := io.ifid.inst
    immGen.io.immType := immType

    // Registers used for operation
    // idexReg.rs1 := rs1
    //idexReg.rs2 := rs2
    //idexReg.rs3 := rs3
    //idexReg.rd := rd

    idexNext.rs1 := rs1
    idexNext.rs2 := rs2
    idexNext.rs3 := rs3
    idexNext.rd := rd
    idexNext.csrSrcDest := csrSrcDest

    // Operand A selection
    /*
    idexReg.opa := MuxCase(DontCare, Seq(
        (operandA === OPA.ZERO) -> 0.U(DATA_WIDTH.W),
        (operandA === OPA.XRS1) -> intRegBank.io.xrs1,
        (operandA === OPA.PC  ) -> io.ifid.pc,
        (operandA === OPA.FRS1) -> fpRegBank.io.frs1,
    ))

     */
    idexNext.opa := MuxCase(DontCare, Seq(
        (operandA === OPA.ZERO) -> 0.U(DATA_WIDTH.W),
        (operandA === OPA.XRS1) -> intRegBank.io.xrs1,
        (operandA === OPA.PC  ) -> io.ifid.pc,
        (operandA === OPA.FRS1) -> fpRegBank.io.frs1,
    ))

    // Operand B selection
    /*
    idexReg.opb := MuxCase(DontCare, Seq(
        (operandB === OPB.XRS2) -> intRegBank.io.xrs2,
        (operandB === OPB.IMM ) -> immGen.io.immRes,
        (operandB === OPB.FRS2) -> fpRegBank.io.frs2,
    ))

     */
    idexNext.opb := MuxCase(DontCare, Seq(
        (operandB === OPB.XRS2) -> intRegBank.io.xrs2,
        (operandB === OPB.IMM ) -> immGen.io.immRes,
        (operandB === OPB.FRS2) -> fpRegBank.io.frs2,
        (operandB === OPB.ZERO) -> 0.U(DATA_WIDTH.W),
    ))

    // Operand C selection
    /*
    idexReg.opc := MuxCase(DontCare, Seq(
        (operandC === OPC.XRS2) -> intRegBank.io.xrs2,
        (operandC === OPC.FRS2) -> fpRegBank.io.frs2,
        (operandC === OPC.FRS3) -> fpRegBank.io.frs3,
    ))
     */
    idexNext.opc := MuxCase(DontCare, Seq(
        (operandC === OPC.XRS2) -> intRegBank.io.xrs2,
        (operandC === OPC.FRS2) -> fpRegBank.io.frs2,
        (operandC === OPC.FRS3) -> fpRegBank.io.frs3,
        (operandC === OPC.IMM)  -> immGen.io.immRes,
    ))
    
    //idexReg.exCtrl := exCtrl
    //idexReg.meCtrl := meCtrl
    //idexReg.wbCtrl := wbCtrl

    idexNext.exCtrl := exCtrl
    idexNext.meCtrl := meCtrl
    idexNext.wbCtrl := wbCtrl

    //idexReg.pc := io.ifid.pc
    //idexReg.branched := io.ifid.branched
    //idexReg.pcPlusFour := io.ifid.pcPlusFour

    idexNext.pc := io.ifid.pc
    idexNext.branched := io.ifid.branched
    idexNext.pcPlusFour := io.ifid.pcPlusFour
    // Output to execution
    when(io.en){
        idexReg := Mux(io.flush, 0.U.asTypeOf(new IDEX), idexNext)
    }
    io.idex := idexReg
    io.idexNext := idexNext
}