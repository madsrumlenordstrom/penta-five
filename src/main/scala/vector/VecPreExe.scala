package vector
import chisel3._
import utility.Constants._
import utility.VecControlSignals._
import chisel3.util._
class VecPreExe extends Module{
  val io = IO(new VecPreExeIO)


  // Stage 1.
  // Tasks:
  // Read current vl, vlmul and sew
  // Check if new emul is out of range
  // Calculate new vl
  // Read scalar registers

  // CSR's
  val vill = RegInit(true.B)
  val vma, vta = RegInit(0.U(1.W))
  val vsew, vlmul = RegInit(0.U(3.W))
  val vlenb = (VLEN/8).U
  val vl = RegInit(0.U(DATA_WIDTH.W))

  val villNext = WireDefault(vill)
  val vmaNext = WireDefault(vma)
  val vtaNext = WireDefault(vta)
  val vsewNext = WireDefault(vsew)
  val vlmulNext = WireDefault(vlmul)
  val vlNext = WireDefault(vl)
  val vtype = villNext ## 0.U(23.W) ## vmaNext ## vtaNext ## vsewNext ## vlmulNext

  val illegalEmul = RegInit(false.B)
  val keepVL = RegInit(false.B)
  val keepVLNext = WireDefault(false.B)
  val vlmax = RegInit(0.U(log2Up(VLEN + 1).W))
  val vlmaxNext = WireDefault(vlmax)
  val sew = Mux(io.dec.opb === OPB.IMM, io.dec.zimm(5,3), io.scalar.xs2(5,3))
  val lmul = Mux(io.dec.opb === OPB.IMM, io.dec.zimm(2,0), io.scalar.xs2(2,0))



  // Output to scalar core
  io.scalar.rs1 := io.dec.vs1
  io.scalar.rs2 := io.dec.vs2


  val stage1 = RegInit(0.U.asTypeOf(new VecPreExeStage1))

  // Calculators
  val vecLenCalc = Module(new VecLenCalc)
  val avl = WireDefault(io.dec.vs1) // immediate value as default
  when((io.dec.opa =/= OPA.IMM) & (io.dec.vs1 === 0.U)){
    when(io.dec.vd =/= 0.U){
      avl := ~(0.U(XLEN.W))
    }.otherwise{
      avl := vlNext
      keepVLNext := io.dec.vset === VSET.Y
    }
  } . elsewhen(io.dec.opa =/= OPA.IMM){
    avl := io.scalar.xs1
  }

  // We just check if the differenc
  val sewDiff = vsew - vsewNext
  val oldVsetFrac = lmul - vlmulNext

  vecLenCalc.io.avl := avl
  vecLenCalc.io.vlmul := lmul
  vecLenCalc.io.vsew := sew
  vecLenCalc.io.en := io.en

  when(io.en){
    illegalEmul := ((vlmulNext === mf8) & (io.dec.eew < vsewNext)) | (vlmulNext === mf4 & (io.dec.eew === e8 ) & (vsewNext === e32)) | (vlmulNext === m4 & (io.dec.eew === e32) & (vsewNext === e8)) | (vlmulNext === m8 & (io.dec.eew > vsewNext))
    keepVL := keepVLNext
    stage1.vs1 := io.dec.vs1
    stage1.vs2 := io.dec.vs2
    stage1.vd := io.dec.vd
    stage1.opcode := io.dec.opcode
    stage1.opa := io.dec.opa
    stage1.opb := io.dec.opb
    stage1.memRE := io.dec.memRE
    stage1.memWE := io.dec.memWE
    stage1.ew := Mux((io.dec.memWE === MEMWE.Y) | (io.dec.memRE === MEMRE.Y), io.dec.eew, vsewNext)
    stage1.vl := vlNext
    stage1.regWE := io.dec.regWE
    stage1.scalar.xs1 := io.scalar.xs1
    stage1.scalar.rd := io.dec.vd
    stage1.scalar.we := io.dec.regWE === REGWE.INT
    stage1.vlmul := vecLenCalc.io.vlmul
    stage1.vsew := vecLenCalc.io.vsew
    stage1.vm := io.dec.vm
    stage1.vset := io.dec.vset
    stage1.active := io.dec.active
    stage1.lane := io.dec.lane
    vill := villNext
    vma := vmaNext
    vta := vtaNext
    vsew := vsewNext
    vlmul := vlmulNext
    vl := vlNext
  }

  // Stage 2.
  // Tasks:
  // Last step of calculating emul
  // Check if we can execute the instruction now, or we must stall.
  // Write new vl to scalar register
  io.scalar.rd := stage1.vd
  io.scalar.we := (stage1.vset === VSET.Y) & (stage1.active)
  io.scalar.xrd := vlNext
  when((((stage1.memWE === MEMWE.Y) | (stage1.memRE === MEMRE.Y)) & illegalEmul) | ((stage1.vset === VSET.Y) & vecLenCalc.io.illegal)){
    villNext := true.B
  } .elsewhen(stage1.vset === VSET.Y){ // Legal setting of vtype
    villNext := false.B
  }
  // Check if operands are valid with the current lmul setting.
  val writeToVec = stage1.regWE === REGWE.VEC
  val writeToMem = stage1.memWE === MEMWE.Y
  val isLoad = (stage1.opa === OPA.VRS3)
  val vs2IsVec = stage1.opa === OPA.VRS2
  val vs1IsVec = stage1.opb === OPB.VRS1
  val vectorOperands = (stage1.opa === OPA.VRS2) | (stage1.opa === OPA.VRS3) | (stage1.opb === OPB.VRS1) & (stage1.active)
  switch(vlmul){
    is(m2){ // when lmul = 2, registers v1, v3, v5... are invalid source or destination registers.
      when(stage1.active & ((stage1.vd(0) & (writeToVec | writeToMem)) | ((stage1.vs1(0) & vs1IsVec)|(stage1.vs2(0) & vs2IsVec)))){
        villNext := true.B
      }
    }
    is(m4) { // when lmul = 4, registers v1, v2, v3, v5, v6, v7... are invalid source or destination registers.
      when(stage1.active & (((stage1.vd(0) | stage1.vd(1)) & (writeToVec | writeToMem)) | (((stage1.vs1(0) | stage1.vs1(1)) & vs1IsVec)|((stage1.vs2(0) | stage1.vs2(1)) & vs2IsVec)))){
        villNext := true.B
      }
    }
    is(m8){ // when lmul = 8, registers v1, v2, v3, v4, v5, v6, v7, v9, v10... are invalid source or destination registers.
      when(stage1.active & (((stage1.vd(0) | stage1.vd(1) | stage1.vd(2)) & (writeToVec | writeToMem)) | (((stage1.vs1(0) | stage1.vs1(1) | stage1.vs1(2)) & vs1IsVec)|((stage1.vs2(0) | stage1.vs2(1) | stage1.vs2(2)) & vs2IsVec)))){
        villNext := true.B
      }
    }
  }

  when(stage1.vset === VSET.Y){
    vsewNext := stage1.vsew
    vlmulNext := stage1.vlmul
    vlNext := vecLenCalc.io.vl
    vlmaxNext := vecLenCalc.io.vlmax
    when(keepVL & (vlmax =/= vlmaxNext)){
      villNext := true.B
    }
  }
  when(villNext){
    vmaNext := 0.U
    vtaNext := 0.U
    vsewNext := 0.U
    vlmulNext := 0.U
    vlNext := 0.U
    vlmax := 0.U
  }
  val stage2 = RegInit(0.U.asTypeOf(new VecPreExeStage2))
  when(io.en){
    stage2.vs1 := stage1.vs1
    stage2.vs2 := stage1.vs2
    stage2.vd := stage1.vd
    stage2.opcode := stage1.opcode
    stage2.opa := stage1.opa
    stage2.opb := stage1.opb
    stage2.memRE := stage1.memRE
    stage2.memWE := stage1.memWE
    stage2.regWE := stage1.regWE
    stage2.ew := stage1.ew
    stage2.vl := vlNext
    stage2.valid := (stage1.vset === VSET.N) & (stage1.active) & !villNext
    stage2.xs1 := stage1.scalar.xs1
    stage2.vm := stage1.vm
    stage2.lane := stage1.lane
  }
  io.pending := io.dec.active | stage1.active | stage2.valid
  io.vill := villNext
  io.exe <> stage2
}
