package utility

import chisel3._
import chisel3.util._

import Constants._
import Controlsignals._

// Memory bundles
class ByteRAMIO(memSize: Int) extends Bundle{
    val we = Input(Bool())
    val addr = Input(UInt(log2Up(memSize).W))
    val din = Input(UInt(32.W))
    val dout = Output(UInt(32.W))
    val memWidth = Input(UInt(2.W))
}

class ClientIO(memSize: Int) extends Bundle{
    val valid, we = Output(Bool())
    val dout = Output(UInt(32.W))
    val addr = Output(UInt(log2Up(memSize).W))
    val memWidth = Output(UInt(2.W))
    val ready = Input(Bool())
    val din = Input(UInt(32.W))
    val burst = Output(Bool())
    val snoop = new Bundle{
        val addr = Input(UInt(log2Up(memSize).W)) // Address clients should snoop
        val invalidate = Input(Bool())
    }
}
class MemCtrlIO(channels: Int, memSize: Int) extends Bundle{
    val clients = Vec(channels, Flipped(new ClientIO(memSize)))
    val ram = Flipped(new ByteRAMIO(memSize))
}


class IDirectCacheIO(bSize : Int, memSize: Int) extends Bundle{
    val addr = Input(UInt(log2Up(memSize).W))
    val din = Input(UInt((8*bSize).W))
    val write, setValid, setInvalid = Input(Bool())
    val dout = Output(UInt(32.W))
    val hit = Output(Bool())
    val snoopAddr = Input(UInt(log2Up(memSize).W))
    val snoopHit = Output(Bool())
}

class ICacheCoreIO(memSize: Int) extends Bundle{
    val fatal, valid = Output(Bool())
    val addr = Input(UInt(log2Up(memSize).W))
    val dout = Output(UInt(32.W))
}

class ICacheIO(memSize: Int) extends Bundle{
    val ram = new ClientIO(memSize)
    val core = new ICacheCoreIO(memSize)
}

class ICacheCtrlIO(bSize: Int, lines: Int, memSize: Int) extends Bundle{
    val ram = new ClientIO(memSize)
    val core = new Bundle{
        val fatal, valid = Output(Bool())
        val addr = Input(UInt(log2Up(memSize).W))
        val dout = Output(UInt(32.W))
    }
    val cache = Flipped(new IDirectCacheIO(bSize, memSize))
}


// Pipeline bundles
class EXControl extends Bundle{
    // Control signals used in execution stage
    val opcode = UInt(OP.WIDTH.W)
    val lane = UInt(Lane.WIDTH.W)
    val delay = UInt(Delay.WIDTH.W)
    val jumpType = UInt(Jump.WIDTH.W)
    val opa = UInt(OPA.WIDTH.W)
    val opb = UInt(OPB.WIDTH.W)
    val opc = UInt(OPB.WIDTH.W)
}

class MEControl extends Bundle{
    // Control signals used in memory stage
    val memWE = UInt(MemWE.WIDTH.W)
    val memRE = UInt(MemRE.WIDTH.W)
    val memData = UInt(MemData.WIDTH.W)
}

class WBControl extends Bundle{
    // Control signals used in writeback stage
    val selWB = UInt(WB.WIDTH.W)
    val regWE = UInt(RegWE.WIDTH.W)
    val active = Bool()
}

class IFID extends Bundle{
    val pc = UInt(ADDR_WIDTH.W)
    val pcPlusFour = UInt(ADDR_WIDTH.W)
    val inst = UInt(INST_WIDTH.W)
    val branched = Bool()
}

class IDEX extends Bundle{
    val rs1, rs2, rs3, rd = UInt(5.W)
    val opa, opb, opc = UInt(DATA_WIDTH.W)
    val csrSrcDest = UInt(log2Ceil(NUM_OF_CSR).W)
    val exCtrl = new EXControl
    val meCtrl = new MEControl
    val wbCtrl = new WBControl
    val pc = UInt(ADDR_WIDTH.W)
    val branched = Bool()
    val pcPlusFour = UInt(ADDR_WIDTH.W)
}

class EXMEM extends Bundle{
    val rs1, rs2, rs3, rd = UInt(5.W)
    val csrSrcDest = UInt(log2Ceil(NUM_OF_CSR).W)
    val data = UInt(DATA_WIDTH.W)
    val addr = UInt(DATA_WIDTH.W)
    val pcPlusFour = UInt(ADDR_WIDTH.W)
    val meCtrl = new MEControl
    val wbCtrl = new WBControl
}

class MEMWB extends Bundle{
    val rd = UInt(5.W)
    val aluData = UInt(DATA_WIDTH.W)
    val memData = UInt(DATA_WIDTH.W)
    val csrData = UInt(DATA_WIDTH.W)
    val pcPlusFour = UInt(ADDR_WIDTH.W)
    val wbCtrl = new WBControl
}

class FetchIO(memSize: Int) extends Bundle{
    val branchPC = Input(UInt(DATA_WIDTH.W))
    val setPC = Input(Bool())
    val ifid = Output(new IFID)
    val flush = Input(Bool())
    val en = Input(Bool())
    val ram = new ClientIO(memSize)
    val fatal = Output(Bool())
    val vecInst = Output(UInt(32.W))
}

class DecodeIO extends Bundle{
    val regData = Input(UInt(DATA_WIDTH.W))
    val rd = Input(UInt(5.W))
    val regWE = Input(UInt(RegWE.WIDTH.W))
    val ifid = Input(new IFID)
    val idexNext = Output(new IDEX)
    val idex = Output(new IDEX)
    val en = Input(Bool())
    val flush = Input(Bool())
    val active = Output(Bool())
    val vec = new Bundle{
        val rs1, rs2, rd = Input(UInt(5.W))
        val xs1, xs2 = Output(UInt(32.W))
        val xrd = Input(UInt(32.W))
        val we = Input(Bool())
        val bankAccess = Input(Bool())
    }
}

class ExecuteIO(maxDelay: Int) extends Bundle{
    val idex = Input(new IDEX)
    val idexQueue = Output(Vec(maxDelay, new IDEX))
    val exmem = Output(new EXMEM)
    val branchPC = Output(UInt(DATA_WIDTH.W))
    val setPC = Output(Bool())
    val stall = Output(Bool())
    val en = Input(Bool())
    val forward = Input(new Bundle{
        val EX = new Bundle{
            val A = UInt(2.W)
            val B = UInt(2.W)
            val C = UInt(2.W)
        }
        val MEMData = UInt(DATA_WIDTH.W)
        val WBData = UInt(DATA_WIDTH.W)
    })
}

class MemoryIO(memSize: Int) extends Bundle{
    val exmem = Input(new EXMEM)
    val memwb = Output(new MEMWB)
    val csr = Output(Vec(NUM_OF_CSR,UInt(DATA_WIDTH.W)))
    val ram = new ClientIO(memSize)
    val stall = Output(Bool())
    val en = Input(Bool())
    val led = Output(Bool())
}

class WriteBackIO extends Bundle{
    val memwb = Input(new MEMWB)
    val regData = Output(UInt(DATA_WIDTH.W))
    val regWE = Output(UInt(RegWE.WIDTH.W))
    val rd = Output(UInt(5.W))
}

class HazardUnitIO(maxDelay: Int) extends Bundle{
    val ifid = Input(new IDEX)
    val idex = Input(new IDEX)
    val idexQueue = Input(Vec(maxDelay, new IDEX))
    val stall = Output(Bool())
}

class ForwardUnitIO(maxDelay: Int) extends Bundle{
    val idex = Input(new IDEX) // Output of pipeline register
    val exmem = Input(new EXMEM)
    val memwb = Input(new MEMWB)
    val forward = Output(new Bundle{
        val EX = new Bundle{
            val A = UInt(2.W)
            val B = UInt(2.W)
            val C = UInt(2.W)
        }
    })
}
class CoreIO(memSize: Int) extends Bundle{
    val clients = Vec(2, new ClientIO(memSize))
    val fatal = Output(Bool())
    val csr = Output(Vec(NUM_OF_CSR,UInt(DATA_WIDTH.W)))
    val vec = new Bundle{
        val rs1, rs2, rd = Input(UInt(5.W))
        val xs1, xs2 = Output(UInt(32.W))
        val xrd = Input(UInt(32.W))
        val we = Input(Bool())
        val busyIn = Input(Bool())
        val busyOut = Output(Bool())
        val haltFetch = Input(Bool())
        val inst = Output(UInt(32.W))
    }
    val led = Output(Bool())
}

// Various unused IO bundles
class RAMIO(size: Int) extends Bundle{
    val we = Input(Bool())
    val addr = Input(UInt(log2Up(size).W))
    val din = Input(UInt(32.W))
    val dout = Output(UInt(32.W))
}
class ClientWDCacheIO(size: Int, maxBSize: Int) extends Bundle{
    val valid, we = Output(Bool())
    val dout = Output(UInt(32.W))
    val addr = Output(UInt(log2Up(size).W))
    val ready = Input(Bool())
    val din = Input(UInt(32.W))
    val snoop = new Bundle{
        val addr = Input(UInt(log2Up(size).W)) // Address clients should snoop
        val request = Input(Bool()) // Request a client to put it's dirty data on the bus
        val update = Input(Bool())
        val invalidate = Input(Bool())
        val we = Output(Bool())
        val done = Output(Bool()) // Tell main controller that block has been transfered to client
        val hit = Output(Bool()) // Tell main controller that the block has been found
        val dirty = Output(Bool()) // Tell main controller if the block is dirty
        val bSizeOut = Output(UInt(log2Up(maxBSize+1).W)) // How big of a block does the client itself have
        val bSizeIn = Input(UInt(log2Up(maxBSize+1).W)) // How big of a block does the other client have
    }
}
class MemCtrlWDCacheIO(channels: Int, memSize: Int, maxBSize: Int) extends Bundle{
    val clients = Vec(channels, Flipped(new ClientWDCacheIO(memSize, maxBSize)))
    val ram = Flipped(new RAMIO(memSize))
}
class DDirectCacheIO(bSize : Int, lines: Int, memSize: Int) extends Bundle{
    val addr = Input(UInt(log2Up(memSize).W))
    val din = Input(UInt(32.W))
    val memWidth = Input(UInt(2.W))
    val write, setDirty, setValid, setInvalid, setClean = Input(Bool())
    val tagOut = Output(UInt((1+(31-log2Up(lines) + log2Up(bSize))).W))
    val dout = Output(UInt(32.W))
    val hit, valid, dirty = Output(Bool())
    val snoopAddr = Input(UInt(log2Up(memSize).W))
    val snoopHit = Output(Bool())
    val snoopDirty = Output(Bool())
}

class DCacheCoreIO(memSize: Int) extends Bundle{
    val addr = Input(UInt(log2Up(memSize).W))
    val dout = Input(UInt(32.W))
    val we, valid = Input(Bool())
    val memWidth = Input(UInt(2.W))
    val din = Output(UInt(32.W))
    val ready = Output(Bool())
    val fatal = Output(Bool())
}

class DCacheCtrlIO(bSize: Int, lines: Int, memSize: Int, maxBSize: Int) extends Bundle{
    val ram = new ClientWDCacheIO(memSize, maxBSize)
    val core = new DCacheCoreIO(memSize)
    val cache = Flipped(new DDirectCacheIO(bSize, lines, memSize))
}

class DCacheIO(memSize: Int, maxBSize: Int) extends Bundle{
    val ram = new ClientWDCacheIO(memSize, maxBSize)
    val core = new DCacheCoreIO(memSize)
}




