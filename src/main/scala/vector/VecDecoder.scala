package vector
import chisel3._
import utility.Instructions._
import utility.VecControlSignals._
import chisel3.util.experimental.decode._
import utility.VecControlSignals._
import chisel3.util._
object ControlLogic{

  // Control table

  val defaultCtrlWord: Seq[BitPat] = Seq(OP.X,      OPA.X, OPB.X,    MEMRE.X, MEMWE.X, VSET.N, EEW.X, REGWE.X, LANE.X, ACT.N)

  val ctrlTable: TruthTable = TruthTable(Map(
  VLE8_V     -> Seq(OP.X,      OPA.VRS3, OPB.XRS1, MEMRE.Y, MEMWE.N, VSET.N, EEW.E8,  REGWE.VEC, LANE.LI,   ACT.Y),
  VLE16_V    -> Seq(OP.X,      OPA.VRS3, OPB.XRS1, MEMRE.Y, MEMWE.N, VSET.N, EEW.E16, REGWE.VEC, LANE.LI,   ACT.Y),
  VLE32_V    -> Seq(OP.X,      OPA.VRS3, OPB.XRS1, MEMRE.Y, MEMWE.N, VSET.N, EEW.E32, REGWE.VEC, LANE.LI,   ACT.Y),
  VSE8_V     -> Seq(OP.X,      OPA.VRS3, OPB.XRS1, MEMRE.N, MEMWE.Y, VSET.N, EEW.E8,  REGWE.N,   LANE.X,    ACT.Y),
  VSE16_V    -> Seq(OP.X,      OPA.VRS3, OPB.XRS1, MEMRE.N, MEMWE.Y, VSET.N, EEW.E16, REGWE.N,   LANE.X,    ACT.Y),
  VSE32_V    -> Seq(OP.X,      OPA.VRS3, OPB.XRS1, MEMRE.N, MEMWE.Y, VSET.N, EEW.E32, REGWE.N,   LANE.X,    ACT.Y),
  VSETVLI    -> Seq(OP.X,      OPA.XRS1, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.Y, EEW.X,   REGWE.INT, LANE.X,    ACT.Y),
  VSETIVLI   -> Seq(OP.X,      OPA.IMM,  OPB.IMM,  MEMRE.N, MEMWE.N, VSET.Y, EEW.X,   REGWE.INT, LANE.X,    ACT.Y),
  VSETVL     -> Seq(OP.X,      OPA.XRS1, OPB.XRS2, MEMRE.N, MEMWE.N, VSET.Y, EEW.X,   REGWE.INT, LANE.X,    ACT.Y),
  VADD_VV    -> Seq(OP.ADD,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSUB_VV    -> Seq(OP.SUB,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VAND_VV    -> Seq(OP.AND,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VOR_VV     -> Seq(OP.OR,     OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VXOR_VV    -> Seq(OP.XOR,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSLL_VV    -> Seq(OP.SLL,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSRL_VV    -> Seq(OP.SRL,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSRA_VV    -> Seq(OP.SRA,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSEQ_VV   -> Seq(OP.SEQ,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSNE_VV   -> Seq(OP.SNE,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLTU_VV  -> Seq(OP.SLTU,   OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLT_VV   -> Seq(OP.SLT,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLEU_VV  -> Seq(OP.SLEU,   OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLE_VV   -> Seq(OP.SLE,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMULHU_VV  -> Seq(OP.MULHU,  OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VMUL_VV    -> Seq(OP.MUL,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VMULHSU_VV -> Seq(OP.MULHSU, OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VMULH_VV   -> Seq(OP.MULH,   OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VDIVU_VV   -> Seq(OP.DIVU,   OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII,  ACT.Y),
  VDIV_VV    -> Seq(OP.DIV,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII,  ACT.Y),
  VREMU_VV   -> Seq(OP.REMU,   OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII,  ACT.Y),
  VREM_VV    -> Seq(OP.REM,    OPA.VRS2, OPB.VRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII,  ACT.Y),
  VADD_VI    -> Seq(OP.ADD,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VAND_VI    -> Seq(OP.ADD,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VOR_VI     -> Seq(OP.OR,     OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VXOR_VI    -> Seq(OP.XOR,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSLL_VI    -> Seq(OP.SLL,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSRL_VI    -> Seq(OP.SRL,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSRA_VI    -> Seq(OP.SRA,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSEQ_VI   -> Seq(OP.SEQ,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSNE_VI   -> Seq(OP.SNE,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLEU_VI  -> Seq(OP.SLEU,   OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLE_VI   -> Seq(OP.SLE,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSGTU_VI  -> Seq(OP.SGTU,   OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSGT_VI   -> Seq(OP.SGT,    OPA.VRS2, OPB.IMM,  MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VADD_VX    -> Seq(OP.ADD,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSUB_VX    -> Seq(OP.SUB,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VAND_VX    -> Seq(OP.AND,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VOR_VX     -> Seq(OP.OR,     OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VXOR_VX    -> Seq(OP.XOR,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSLL_VX    -> Seq(OP.SLL,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSRL_VX    -> Seq(OP.SRL,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VSRA_VX    -> Seq(OP.SRA,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSEQ_VX   -> Seq(OP.SEQ,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSNE_VX   -> Seq(OP.SNE,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLTU_VX  -> Seq(OP.SLTU,   OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLT_VX   -> Seq(OP.SLT,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLEU_VX  -> Seq(OP.SLEU,   OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSLE_VX   -> Seq(OP.SLE,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSGTU_VX  -> Seq(OP.SGTU,   OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMSGT_VX   -> Seq(OP.SGT,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LI,   ACT.Y),
  VMUL_VX    -> Seq(OP.MUL,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VMULHU_VX  -> Seq(OP.MULHU,  OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VMULHSU_VX -> Seq(OP.MULHSU, OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VMULH_VX   -> Seq(OP.MULH,   OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LIII, ACT.Y),
  VDIVU_VX   -> Seq(OP.DIVU,   OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII , ACT.Y),
  VDIV_VX    -> Seq(OP.DIV,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII , ACT.Y),
  VREMU_VX   -> Seq(OP.REMU,   OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII , ACT.Y),
  VREM_VX    -> Seq(OP.REM,    OPA.VRS2, OPB.XRS1, MEMRE.N, MEMWE.N, VSET.N, EEW.X,   REGWE.VEC, LANE.LII , ACT.Y)
  ).map({case (k, v) => k -> v.reduce(_ ## _)}), defaultCtrlWord.reduce(_ ## _))

}

class VecDecoder extends Module {
  val io = IO(new VecDecoderIO)


  val ctrl = RegInit(0.U.asTypeOf(new VecCtrl))
  val ctrlWord: UInt = decoder(minimizer = QMCMinimizer, input = io.inst, truthTable = ControlLogic.ctrlTable)
  val ctrlWordWidth = ctrlWord.getWidth
  var ctrlOffset = 0
  val opcode = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OP.WIDTH)
  ctrlOffset = ctrlOffset + OP.WIDTH
  val operandA = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OPA.WIDTH)
  ctrlOffset = ctrlOffset + OPA.WIDTH
  val operandB = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - OPB.WIDTH)
  ctrlOffset = ctrlOffset + OPB.WIDTH
  val memRE = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - MEMRE.WIDTH)
  ctrlOffset = ctrlOffset + MEMRE.WIDTH
  val memWE = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - MEMWE.WIDTH)
  ctrlOffset = ctrlOffset + MEMWE.WIDTH
  val vset = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - VSET.WIDTH)
  ctrlOffset = ctrlOffset + VSET.WIDTH
  val eew = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - EEW.WIDTH)
  ctrlOffset = ctrlOffset + EEW.WIDTH
  val regWE = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - REGWE.WIDTH)
  ctrlOffset = ctrlOffset + REGWE.WIDTH
  val lane = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - LANE.WIDTH)
  ctrlOffset = ctrlOffset + LANE.WIDTH
  val active = ctrlWord(ctrlWordWidth - ctrlOffset - 1, ctrlWordWidth - ctrlOffset - ACT.WIDTH)
  ctrlOffset = ctrlOffset + ACT.WIDTH

  when(io.en) {
    ctrl.vd := io.inst(11, 7)
    ctrl.vs1 := io.inst(19, 15) // Also works as uimm for vsetivli
    ctrl.vs2 := io.inst(24, 20)
    ctrl.zimm := io.inst(30, 20)
    ctrl.opcode := opcode
    ctrl.opa := operandA
    ctrl.opb := operandB
    ctrl.memRE := memRE
    ctrl.memWE := memWE
    ctrl.vset := vset
    ctrl.eew := eew
    ctrl.regWE := regWE
    ctrl.vm := io.inst(25)
    ctrl.lane := lane
    ctrl.active := active
  }
  io.pending := active
  io.ctrl <> ctrl
}

