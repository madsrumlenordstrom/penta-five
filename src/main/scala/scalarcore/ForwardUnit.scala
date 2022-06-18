
package scalarcore
import chisel3._
import utility.ForwardUnitIO
import utility.Controlsignals._
import utility.Constants._

class ForwardUnit(maxDelay: Int) extends Module {
  val io = IO(new ForwardUnitIO(maxDelay))

  // Forward from memory stage
  val memHazardIntA = io.exmem.wbCtrl.regWE === RegWE.INT & (io.exmem.rd =/= 0.U) & (io.idex.exCtrl.opa === OPA.XRS1) & (io.idex.rs1 === io.exmem.rd)
  val memHazardIntB = io.exmem.wbCtrl.regWE === RegWE.INT & (io.exmem.rd =/= 0.U) & (io.idex.exCtrl.opb === OPB.XRS2) & (io.idex.rs2 === io.exmem.rd)
  val memHazardIntC = io.exmem.wbCtrl.regWE === RegWE.INT & (io.exmem.rd =/= 0.U) & (io.idex.exCtrl.opc === OPC.XRS2) & (io.idex.rs2 === io.exmem.rd)
  val memHazardFPA = io.exmem.wbCtrl.regWE === RegWE.FP & (io.idex.exCtrl.opa === OPA.FRS1) & (io.idex.rs1 === io.exmem.rd)
  val memHazardFPB = io.exmem.wbCtrl.regWE === RegWE.FP & (io.idex.exCtrl.opb === OPB.FRS2) & (io.idex.rs2 === io.exmem.rd)
  val memHazardFPC = io.exmem.wbCtrl.regWE === RegWE.FP & (((io.idex.exCtrl.opc === OPC.FRS2) & (io.idex.rs2 === io.exmem.rd)) | ((io.idex.exCtrl.opc === OPC.FRS3) & (io.idex.rs3 === io.exmem.rd)))
  val memHazardA = memHazardIntA | memHazardFPA
  val memHazardB = memHazardIntB | memHazardFPB
  val memHazardC = memHazardIntC | memHazardFPC

  // Forward from writeback stage
  val wbHazardIntA = io.memwb.wbCtrl.regWE === RegWE.INT & (io.memwb.rd =/= 0.U) & (io.idex.exCtrl.opa === OPA.XRS1) & (io.idex.rs1 === io.memwb.rd)
  val wbHazardIntB = io.memwb.wbCtrl.regWE === RegWE.INT & (io.memwb.rd =/= 0.U) & (io.idex.exCtrl.opb === OPB.XRS2) & (io.idex.rs2 === io.memwb.rd)
  val wbHazardIntC = io.memwb.wbCtrl.regWE === RegWE.INT & (io.memwb.rd =/= 0.U) & (io.idex.exCtrl.opc === OPC.XRS2) & (io.idex.rs2 === io.memwb.rd)
  val wbHazardFPA = io.memwb.wbCtrl.regWE === RegWE.FP & (io.idex.exCtrl.opa === OPA.FRS1) & (io.idex.rs1 === io.memwb.rd)
  val wbHazardFPB = io.memwb.wbCtrl.regWE === RegWE.FP & (io.idex.exCtrl.opb === OPB.FRS2) & (io.idex.rs2 === io.memwb.rd)
  val wbHazardFPC = io.memwb.wbCtrl.regWE === RegWE.FP & (((io.idex.exCtrl.opc === OPC.FRS2) & (io.idex.rs2 === io.memwb.rd)) | ((io.idex.exCtrl.opc === OPC.FRS3) & (io.idex.rs3 === io.memwb.rd)))
  val wbHazardA = !memHazardA & ( wbHazardIntA | wbHazardFPA)
  val wbHazardB = !memHazardB & ( wbHazardIntB | wbHazardFPB)
  val wbHazardC = !memHazardC & (wbHazardIntC | wbHazardFPC)


  io.forward.EX.A := FORWARD_NO
  when(memHazardA){ io.forward.EX.A := FORWARD_MEM }
  when(wbHazardA){ io.forward.EX.A := FORWARD_WB }
  io.forward.EX.B := FORWARD_NO
  when(memHazardB){ io.forward.EX.B := FORWARD_MEM }
  when(wbHazardB){ io.forward.EX.B := FORWARD_WB }
  io.forward.EX.C := FORWARD_NO
  when(memHazardC){ io.forward.EX.C := FORWARD_MEM }
  when(wbHazardC){ io.forward.EX.C := FORWARD_WB }
}