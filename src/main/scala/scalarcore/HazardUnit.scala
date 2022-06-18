
package scalarcore
import chisel3._
import utility.HazardUnitIO
import utility.Controlsignals._
class HazardUnit(maxDelay: Int) extends Module {
  val io = IO(new HazardUnitIO(maxDelay))

  val rawHazard = WireDefault(false.B)
  val wawHazard = WireDefault(false.B)
  val memHazard = WireDefault(false.B)

  def writesSrc(i: Int): Bool ={
    val regWE = io.idexQueue(i).wbCtrl.regWE
    val rd = io.idexQueue(i).rd
    val rs1 = io.ifid.rs1
    val rs2 = io.ifid.rs2
    val rs3 = io.ifid.rs3
    val opa = io.ifid.exCtrl.opa
    val opb = io.ifid.exCtrl.opb
    val opc = io.ifid.exCtrl.opc
    val writesFloat = (regWE === RegWE.FP) & ((rd === rs1) | (rd === rs2) | (rd === rs3)) & ((opa === OPA.FRS1) | (opb === OPB.FRS2) | (opc === OPC.FRS2) | (opc === OPC.FRS3) )
    val writesInt = (regWE === RegWE.INT) & (rd =/= 0.U) & ((rd === rs1) | (rd === rs2) | (rd === rs3)) & ((opa === OPA.XRS1) | (opb === OPB.XRS2) | (opc === OPC.XRS2))
    writesInt | writesFloat
  }
  def writesDest(i: Int): Bool={
    val regWE_idex = io.idexQueue(i).wbCtrl.regWE
    val regWE_ifid = io.ifid.wbCtrl.regWE
    val rd_idex = io.idexQueue(i).rd
    val rd_ifid = io.ifid.rd
    val writesFloat = (regWE_idex === RegWE.FP) & (regWE_ifid === RegWE.FP)  & (rd_idex === rd_ifid)
    val writesInt = (regWE_idex === RegWE.INT) & (regWE_ifid === RegWE.INT) & (rd_idex =/= 0.U)  & (rd_idex === rd_ifid)
    writesInt | writesFloat
  }

  // RAW hazard (for non loads)
  // If they are in the front of the queue, this doesn't matter as they will be in memory stage afterwards.
  for(i <- 1 until maxDelay){ // Check queue and what is waiting to get in the queue
    when(writesSrc(i)){
      rawHazard := true.B
    }
  }
  // Also check for IDEX register (not in queue yet)
  val regWE = io.idex.wbCtrl.regWE
  val rd = io.idex.rd
  val delay = io.idex.exCtrl.delay
  val rs1 = io.ifid.rs1
  val rs2 = io.ifid.rs2
  val rs3 = io.ifid.rs3
  val opa = io.ifid.exCtrl.opa
  val opb = io.ifid.exCtrl.opb
  val opc = io.ifid.exCtrl.opc
  val writesFloatSrc = (regWE === RegWE.FP) & ((rd === rs1) | (rd === rs2) | (rd === rs3)) & ((opa === OPA.FRS1) | (opb === OPB.FRS2) | (opc === OPC.FRS2) | (opc === OPC.FRS3) )
  val writesIntSrc = (regWE === RegWE.INT) & (rd =/= 0.U) & ((rd === rs1) | (rd === rs2) | (rd === rs3)) & ((opa === OPA.XRS1) | (opb === OPB.XRS2) | (opc === OPC.XRS2))
  when(delay =/= 0.U & (writesFloatSrc | writesIntSrc)){
    rawHazard := true.B
  }
  when(io.idexQueue(0).meCtrl.memRE.asBool & writesSrc(0)){
    memHazard := true.B
  }

  // WAW hazard
  for(i <- 1 until maxDelay){ // Check queue and what is waiting to get in the queue
    when(writesDest(i)){
      wawHazard := true.B
    }
  }
  // Also check for IDEX register (not in queue)
  val writesFloatDest = (io.idex.wbCtrl.regWE === RegWE.FP) & (io.ifid.wbCtrl.regWE === RegWE.FP)  & (io.idex.rd === io.ifid.rd)
  val writesIntDest = (io.idex.wbCtrl.regWE === RegWE.INT) & (io.ifid.wbCtrl.regWE === RegWE.INT) & (io.idex.rd =/= 0.U)  & (io.idex.rd === io.ifid.rd)
  when(delay =/= 0.U & (writesFloatDest | writesIntDest)){
    wawHazard := true.B
  }
  io.stall := memHazard | rawHazard | wawHazard
}
