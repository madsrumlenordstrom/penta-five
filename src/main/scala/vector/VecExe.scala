package vector
import chisel3._
import utility.Functions.connect
import scalarcore.ALU32I
import mdu._
import utility.Constants.DATA_WIDTH
import utility.VecControlSignals._
import chisel3.util._

class VecExe(memSize: Int, maxDelay: Int) extends Module {
  val io = IO(new VecExeIO(memSize))

  val vecReg = Module(new VecReg)
  val vecLS = Module(new VecLSUnit(memSize))
  val alu32i = Module(new ALU32I)
  val div = Module(new PipeDiv)
  val mul = Module(new IntMultiplier)
  val dataReg = RegInit(0.U(DATA_WIDTH.W))
  val dataRegNext = WireDefault(alu32i.io.y)
  val cntPipeDelay = RegInit(0.U(log2Up(maxDelay).W))
  val cntPipeDelayNext = WireDefault(cntPipeDelay)
  val readDone = RegInit(false.B)
  val writeBegin = RegInit(false.B)
  val idle :: init :: ari :: load :: store :: Nil = Enum(5)
  println("VecExe states:")
  println("idle: " + idle.litValue.toInt.toBinaryString)
  println("init: " + init.litValue.toInt.toBinaryString)
  println("ari: " + ari.litValue.toInt.toBinaryString)
  println("load: " + load.litValue.toInt.toBinaryString)
  println("store: " + store.litValue.toInt.toBinaryString)
  val state = RegInit(idle)
  val nextState = WireDefault(state)
  val en = io.en
  val opcode = io.preExe.opcode
  val opa = WireDefault(vecReg.io.read.vec(0))
  val opb = WireDefault(vecReg.io.read.vec(1))
  when(io.preExe.opb === OPB.IMM){ // VI
    opb := io.preExe.vs1
  } . elsewhen(io.preExe.opb === OPB.XRS1){ // VX
    opb := io.preExe.xs1
  }
  // Connect alu32i
  alu32i.io.op := opcode
  alu32i.io.a := opa
  alu32i.io.b := opb

  // Connect divide
  div.io.op := opcode
  div.io.a := opa
  div.io.b := opb
  div.io.en := en

  // Connect multiply
  mul.io.op := opcode
  mul.io.a := opa
  mul.io.b := opb
  mul.io.en := en


  // Connect reg bank
  vecReg.io.en := en
  vecReg.io.write.we := false.B
  vecReg.io.write.vd := io.preExe.vd
  vecReg.io.write.din := dataReg
  when(io.preExe.lane === LANE.LII){
    vecReg.io.write.din := div.io.y
  }
  when(io.preExe.lane === LANE.LIII){
    vecReg.io.write.din := mul.io.res
  }
  vecReg.io.write.ew := io.preExe.ew
  vecReg.io.write.vl := io.preExe.vl
  vecReg.io.write.vstart := 0.U
  vecReg.io.write.vm := io.preExe.vm
  vecReg.io.read.re := false.B
  vecReg.io.read.vs(0) := io.preExe.vs2
  when(io.preExe.memWE === MEMWE.Y){
    vecReg.io.read.vs(0) := io.preExe.vd
  }
  vecReg.io.read.vs(1) := io.preExe.vs1
  vecReg.io.read.ew := io.preExe.ew
  vecReg.io.read.vl := io.preExe.vl
  vecReg.io.read.vstart := 0.U
  // Connect LS unit
  vecLS.io.core.addr := io.preExe.xs1
  vecLS.io.core.dout := dataReg
  vecLS.io.core.we := false.B
  vecLS.io.core.valid := false.B
  vecLS.io.core.vl := io.preExe.vl
  vecLS.io.core.vstart := 0.U
  vecLS.io.core.vm := io.preExe.vm
  vecLS.io.core.mask := vecReg.io.mask
  vecLS.io.core.ew := io.preExe.ew
  vecLS.io.en := en

  // Output
  io.done := (nextState === idle) | ((state === idle) & !io.preExe.valid)
  connect(io.ram.elements, vecLS.io.ram.elements)
  switch(state){
    is(idle){
      when(io.preExe.valid){
        nextState := init
      }
    }
    is(init){
      vecReg.io.read.re := (io.preExe.memRE =/= MEMRE.Y)
      vecLS.io.core.valid := (io.preExe.memWE === MEMWE.Y) | (io.preExe.memRE === MEMRE.Y)
      cntPipeDelayNext := 1.U
      when(io.preExe.lane === LANE.LII){
        cntPipeDelayNext := 52.U
      }
      when(io.preExe.lane === LANE.LIII){
        cntPipeDelayNext := 3.U
      }
      when(io.preExe.memRE === MEMRE.Y){
        nextState := load
      } .elsewhen(io.preExe.memWE === MEMWE.Y){
        nextState := store
        readDone := true.B
      } .otherwise{
        nextState := ari
      }
    }
    is(ari){
      // Reading register bank
      vecReg.io.read.re := !readDone
      when(vecReg.io.read.done){
        readDone := true.B
      }
      // Writing register bank
      when(cntPipeDelay === 1.U){ // First element is ready next cycle
        vecReg.io.write.we := true.B // Tell register bank to be ready next cycle
      } .otherwise{
        cntPipeDelayNext := cntPipeDelay - 1.U
      }
      when(vecReg.io.write.done){
        nextState := idle // Just simple for now
        readDone := false.B
      }
    }
    is(load){
      // VecLSUnit has loaded in base addr and has send valid signal to ram
      // Next clock cycle we will have data ready

      dataRegNext := vecLS.io.core.din
      vecLS.io.core.valid := !readDone
      when(vecLS.io.core.ready){
        writeBegin := true.B
      }
      when(vecLS.io.core.done){
        readDone := true.B
      }
      when(writeBegin | vecLS.io.core.ready){
        vecReg.io.write.we := true.B
      }
      when(vecReg.io.write.done){
        writeBegin := false.B
        readDone := false.B
        nextState := idle
      }
    }
    is(store){
      dataRegNext := vecReg.io.read.vec(0)
      vecReg.io.read.re := !readDone | vecLS.io.core.ready
      when(vecLS.io.core.ready){
        readDone := false.B
      }
      vecLS.io.core.valid := true.B // Unit will be ready next cycle
      vecLS.io.core.we := true.B
      when(vecReg.io.read.done){
        readDone := true.B
      }
      when(vecLS.io.core.done){
        readDone := false.B
        nextState := idle
      }
    }
  }
  // Connect en
  when(en){
    dataReg := dataRegNext
    state := nextState
    cntPipeDelay := cntPipeDelayNext
  }



}
