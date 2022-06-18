package vector
import utility.VecControlSignals._
import utility.Constants._
import chisel3._
import chisel3.util._
import utility.ClientIO
class VecCtrl extends Bundle{
  val vs1, vs2, vd = UInt(5.W)
  val opcode = UInt(OP.WIDTH.W)
  val opa = UInt(OPA.WIDTH.W)
  val opb = UInt(OPB.WIDTH.W)
  val memRE = UInt(MEMRE.WIDTH.W)
  val memWE = UInt(MEMWE.WIDTH.W)
  val vset = UInt(VSET.WIDTH.W)
  val eew = UInt(EEW.WIDTH.W)
  val regWE = UInt(REGWE.WIDTH.W)
  val zimm = UInt(11.W)
  val vm = Bool()
  val lane = UInt(LANE.WIDTH.W)
  val active = Bool()
}
class VecDecoderIO extends Bundle{
  val inst = Input(UInt(32.W))
  val ctrl = Output(new VecCtrl)
  val en = Input(Bool())
  val pending = Output(Bool())
}
class VecPreExeStage1 extends Bundle{
  val vs1, vs2, vd = UInt(5.W)
  val opcode = UInt(OP.WIDTH.W)
  val opa = UInt(OPA.WIDTH.W)
  val opb = UInt(OPB.WIDTH.W)
  val memRE = UInt(MEMRE.WIDTH.W)
  val memWE = UInt(MEMWE.WIDTH.W)
  val vset = UInt(VSET.WIDTH.W)
  val ew = UInt(3.W)
  val regWE = UInt(REGWE.WIDTH.W)
  val vl = UInt(log2Up(VLEN).W)
  val scalar = new Bundle{
    val rd = UInt(5.W)
    val we = UInt(5.W)
    val xs1 = UInt(32.W)
  }
  val vsew = UInt(3.W)
  val vlmul = UInt(3.W)
  val vm = Bool()
  val active = Bool()
  val lane = UInt(LANE.WIDTH.W)
}
class VecPreExeStage2 extends Bundle{
  val vs1, vs2, vd = UInt(5.W)
  val opcode = UInt(OP.WIDTH.W)
  val opa = UInt(OPA.WIDTH.W)
  val opb = UInt(OPB.WIDTH.W)
  val memRE = UInt(MEMRE.WIDTH.W)
  val memWE = UInt(MEMWE.WIDTH.W)
  val ew = UInt(3.W)
  val regWE = UInt(REGWE.WIDTH.W)
  val vl = UInt(log2Up(VLEN).W)
  val xs1 = UInt(32.W)
  val valid = Bool()
  val lane = UInt(LANE.WIDTH.W)
  val vm = Bool()
}
class VecPreExeIO extends Bundle{
  val dec = Input(new VecCtrl)
  val en = Input(Bool())
  val scalar = new Bundle{
    val rs1, rs2, rd = Output(UInt(5.W))
    val xs1, xs2 = Input(UInt(32.W))
    val xrd = Output(UInt(32.W))
    val we = Output(Bool())
  }
  val vill = Output(Bool())
  val exe = Output(new VecPreExeStage2)
  val pending = Output(Bool())
}
class VecExeIO(memSize: Int) extends Bundle{
  val ram = new ClientIO(memSize)
  val preExe = Input(new VecPreExeStage2)
  val en = Input(Bool())
  val done = Output(Bool())
}
class VecCoreIO(memSize: Int, maxDelay: Int) extends Bundle{
  val scalar = new Bundle{
    val rs1, rs2, rd = Output(UInt(5.W))
    val xs1, xs2 = Input(UInt(32.W))
    val xrd = Output(UInt(32.W))
    val we = Output(Bool())
  }
  val inst = Input(UInt(32.W))
  val ram = new ClientIO(memSize)
  val en = Input(Bool())
  val done = Output(Bool())
  val pending = Output(Bool())
  val haltFetch = Output(Bool())
}