package scalarcore
import chisel3._
import chisel3.util._
import fp.FPConstants.RNE
import utility.{EXMEM, ExecuteIO}
import utility.Constants._
import fp._
import mdu._
import utility.Controlsignals.Lane._
import utility.Controlsignals.Jump
class Execute(maxDelay: Int) extends Module {
  val io = IO(new ExecuteIO(maxDelay))

  val pipeReg = RegInit(0.U.asTypeOf(new EXMEM))

  val alu32i = Module(new ALU32I)
  val div = Module(new PipeDivWFP)
  val intmul = Module(new IntMultiplier)
  val fprnd = Module(new FPRounder)
  val fpadd = Module(new FPAdd)
  val fpmul = Module(new FPMultiply)
  val fpclss = Module(new FPClass)
  val fpcmp = Module(new FPCompare)
  val fpcnvt = Module(new FPConvert)
  val fpsgnj = Module(new FPSignInj)
  val fpsqrt = Module(new PipeSqrt)
  val exequeue = Module(new ExecutionQueue(maxDelay))
  val selQueue = WireDefault(exequeue.io.selQueue)
  val dataQueue = WireDefault(exequeue.io.dataQueue)
  val dout = WireInit(0.U.asTypeOf(new EXMEM))
  val exmem = RegInit(0.U.asTypeOf(new EXMEM))
  val opa = WireDefault(io.idex.opa)
  val opb = WireDefault(io.idex.opb)
  val opc = WireDefault(io.idex.opc)
  switch(io.forward.EX.A){
    is(FORWARD_MEM){opa := io.forward.MEMData}
    is(FORWARD_WB){opa := io.forward.WBData}
  }
  switch(io.forward.EX.B){
    is(FORWARD_MEM){opb := io.forward.MEMData}
    is(FORWARD_WB){opb := io.forward.WBData}
  }
  switch(io.forward.EX.C){
    is(FORWARD_MEM){opc := io.forward.MEMData}
    is(FORWARD_WB){opc := io.forward.WBData}
  }

  // Branching TODO finish branch logic
  val eq = alu32i.io.eq
  val ne = !eq
  val lt = alu32i.io.lt
  val ge = !lt | eq
  val ltu = alu32i.io.ltu
  val geu = !ltu | eq

  // Just used for shorter code
  val jumpType = io.idex.exCtrl.jumpType
  val branched = io.idex.branched

  // Check if branch was wrong or for JALR
  io.setPC := MuxCase(false.B, Seq(
    (jumpType === Jump.JALR)                      -> (true.B),
    (jumpType === Jump.BEQ  &&  eq  && !branched) -> (true.B),
    (jumpType === Jump.BEQ  && !eq  &&  branched) -> (true.B),
    (jumpType === Jump.BNE  &&  ne  && !branched) -> (true.B),
    (jumpType === Jump.BNE  && !ne  &&  branched) -> (true.B),
    (jumpType === Jump.BLT  &&  lt  && !branched) -> (true.B),
    (jumpType === Jump.BLT  && !lt  &&  branched) -> (true.B),
    (jumpType === Jump.BGE  &&  ge  && !branched) -> (true.B),
    (jumpType === Jump.BGE  && !ge  &&  branched) -> (true.B),
    (jumpType === Jump.BLTU &&  ltu && !branched) -> (true.B),
    (jumpType === Jump.BLTU && !ltu &&  branched) -> (true.B),
    (jumpType === Jump.BGEU &&  geu && !branched) -> (true.B),
    (jumpType === Jump.BGEU && !geu &&  branched) -> (true.B),
  ))

  // Set next PC
  io.branchPC := MuxCase(DontCare, Seq(
    (jumpType === Jump.JALR)                      -> (alu32i.io.y),
    (jumpType === Jump.BEQ  &&  eq  && !branched) -> ((io.idex.pc.asSInt + opc.asSInt).asUInt),
    (jumpType === Jump.BEQ  && !eq  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BNE  &&  ne  && !branched) -> ((io.idex.pc.asSInt + opc.asSInt).asUInt),
    (jumpType === Jump.BNE  && !ne  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BLT  &&  lt  && !branched) -> ((io.idex.pc.asSInt + opc.asSInt).asUInt),
    (jumpType === Jump.BLT  && !lt  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BGE  &&  ge  && !branched) -> ((io.idex.pc.asSInt + opc.asSInt).asUInt),
    (jumpType === Jump.BGE  && !ge  &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BLTU &&  ltu && !branched) -> ((io.idex.pc.asSInt + opc.asSInt).asUInt),
    (jumpType === Jump.BLTU && !ltu &&  branched) -> (io.idex.pcPlusFour),
    (jumpType === Jump.BGEU &&  geu && !branched) -> ((io.idex.pc.asSInt + opc.asSInt).asUInt),
    (jumpType === Jump.BGEU && !geu &&  branched) -> (io.idex.pcPlusFour),
  ))

  // Stalling units
  div.io.en := io.en
  intmul.io.en := io.en
  fpmul.io.en := io.en
  fpadd.io.en := io.en
  fpsqrt.io.en := io.en
  fpcnvt.io.en := io.en
  fprnd.io.en := io.en
  exequeue.io.en := io.en

  // Queue wiring
  exequeue.io.enqueue := io.en // TODO check if this is right
  exequeue.io.din := io.idex

  // Multiplexing input to next register wall.
  dout.addr := alu32i.io.y
  dout.data := alu32i.io.y
  dout.meCtrl := dataQueue(0).meCtrl
  dout.wbCtrl := dataQueue(0).wbCtrl
  dout.rs1 := dataQueue(0).rs1
  dout.rs2 := dataQueue(0).rs2
  dout.rs3 := dataQueue(0).rs3
  dout.rd := dataQueue(0).rd
  dout.pcPlusFour := dataQueue(0).pcPlusFour
  dout.csrSrcDest := dataQueue(0).csrSrcDest
  when(dataQueue(0).meCtrl.memWE(0)){
    dout.data := opc //io.idex.opc // does this depend on memWE?
  } .elsewhen(selQueue(0)(3)){
    dout.data := fprnd.io.y
  } .elsewhen(selQueue(0) === LII){
    dout.data := intmul.io.res
  } .elsewhen(selQueue(0) === LIII){
    dout.data := div.io.y.int
  } .elsewhen(selQueue(0) === LIV){
    dout.data := fpcnvt.io.y
  } .elsewhen(selQueue(0) === LV){
    dout.data := fpclss.io.mask
  } .elsewhen(selQueue(0) === LVI){
    dout.data := fpcmp.io.y.asUInt
  } .elsewhen(selQueue(0) === LVII){
    dout.data := fpsgnj.io.y.asUInt
  }
  // Multiplexing input to float rounder.
  fprnd.io.a := fpadd.io.y
  fprnd.io.rm := RNE //dataQueue(1).rm
  when(selQueue(1)(3)){ // Instruction that use the rounding unit has the fourth bit set
    when(selQueue(1) === LIX){
      // fprnd.io.a := fma.io.y
    } .elsewhen(selQueue(1) === LX){
      fprnd.io.a := fpcnvt.io.y
    } .elsewhen(selQueue(1) === LXI){
      fprnd.io.a := fpmul.io.res.asUInt
    } .elsewhen(selQueue(1) === LXII){
      fprnd.io.a := div.io.y.float
    } .elsewhen(selQueue(1) === LXIII){
      fprnd.io.a := fpsqrt.io.y
    }
  }


  // Connect opcode
  alu32i.io.op := io.idex.exCtrl.opcode
  div.io.op := io.idex.exCtrl.opcode
  fpcmp.io.op := io.idex.exCtrl.opcode
  fpcnvt.io.op := io.idex.exCtrl.opcode
  fpsgnj.io.op := io.idex.exCtrl.opcode
  intmul.io.op := io.idex.exCtrl.opcode
  fpadd.io.sub := io.idex.exCtrl.opcode(0)

  // Connect operands
  alu32i.io.a := opa
  alu32i.io.b := opb
  div.io.a := opa
  div.io.b := opb
  intmul.io.a := opa
  intmul.io.b := opb
  fpadd.io.a := opa
  fpadd.io.b := opb
  fpmul.io.a.sign := opa(31)
  fpmul.io.a.exp := opa(30, 23)
  fpmul.io.a.man := opa(22, 0)
  fpmul.io.b.sign := opb(31)
  fpmul.io.b.exp := opb(30, 23)
  fpmul.io.b.man := opb(22, 0)
  fpclss.io.a := opa
  fpcmp.io.a := opa
  fpcmp.io.b := opb
  fpcnvt.io.a := opa
  fpsgnj.io.a := opa
  fpsgnj.io.b := opb
  fpsqrt.io.a := opa

  // Stall out
  io.stall := exequeue.io.busy

  // Output register logic
  when(io.en){
    exmem <> dout
  }
  io.exmem <> exmem
  io.idexQueue <> dataQueue

}