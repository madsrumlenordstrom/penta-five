package fp

import chisel3._
import FPConstants._
import mdu.DigitSerialMultiply24
import utility.Constants._

class FPMultiply extends Module{
  val io = IO(new Bundle{
    val a = Input(new Float32)
    val b = Input(new Float32)
    val en = Input(Bool())
    //val rm = Input(UInt(3.W)) // Float rounding mode
    //val fcsr = Input(UInt(32.W)) // Maybe remove 24 MSBs and 5 LSBs?
    val res = Output(new Float32)
  })

  // 2 stage multiplier for mantissa
  val manMult = Module(new DigitSerialMultiply24)

  manMult.io.en := io.en

  // Result is ready in two cycles
  manMult.io.a := (1.U(1.W) ## io.a.man) // Concatenate hidden fraction bit
  manMult.io.b := (1.U(1.W) ## io.b.man)

  val stage1Reg = Reg(new Bundle() {
    val sign = Bool()
    val expSum = UInt((EXP_WIDTH + 1).W)
    // Mantissa has own pipeline register in the module manMult
  })

  when(io.en){
    stage1Reg.sign := io.a.sign ^ io.b.sign // Calculate new sign
    stage1Reg.expSum := io.a.exp + io.b.exp // Add exponents
  }.otherwise{
    stage1Reg.sign := stage1Reg.sign
    stage1Reg.expSum := stage1Reg.expSum
  }

  val stage2Reg = Reg(new Bundle() {
    val sign = Bool()
    val expSum = UInt((EXP_WIDTH).W)
    val manProd = UInt((2*(MAN_WIDTH + 1)).W)
  })

  when(io.en){
    stage2Reg.sign := stage1Reg.sign
    stage2Reg.expSum := stage1Reg.expSum - EXP_BIAS.U // Remove bias
    stage2Reg.manProd := manMult.io.res // Get result from pipelined multiplier
  }. otherwise{
    stage2Reg.sign := stage2Reg.sign
    stage2Reg.expSum := stage2Reg.expSum
    stage2Reg.manProd := stage2Reg.manProd
  }

  val resReg = Reg(new Float32) 

  // TODO add rounding
  when(stage2Reg.manProd(2*(MAN_WIDTH + 1) - 1) === 1.U && io.en){
    resReg.sign := stage2Reg.sign
    resReg.exp := stage2Reg.expSum + 1.U
    resReg.man := stage2Reg.manProd(2*(MAN_WIDTH + 1) - 2, 2*(MAN_WIDTH + 1) - 1 - MAN_WIDTH)
  }.elsewhen(io.en){
    resReg.sign := stage2Reg.sign
    resReg.exp := stage2Reg.expSum
    resReg.man := stage2Reg.manProd(2*(MAN_WIDTH + 1) - 3, 2*(MAN_WIDTH + 1) - 2 - MAN_WIDTH)
  }.otherwise{
    resReg.sign := io.res.sign
    resReg.exp := io.res.exp
    resReg.man := io.res.man
  }

  io.res := resReg

  //io.res.man := stage2Reg.manProd(2*(MAN_WIDTH + 1) - 1, 2*(MAN_WIDTH + 1) - MAN_WIDTH)
  /*
  // Second stage
  when(manProd(2*(MAN_WIDTH + 1) - 1)){
    io.res.sign := newSign // Can be moved outside when statement
    io.res.exp := newExp + 1.U
    io.res.man := (manProd >> 1.U)(2*(MAN_WIDTH + 1) - 1, 2*(MAN_WIDTH + 1) - 1 - 23)
  }. otherwise{
    io.res.sign := newSign
    io.res.exp := newExp
    io.res.man := manProd(2*(MAN_WIDTH + 1) - 2, 2*(MAN_WIDTH + 1) - 2 - 23)
  }

   */
}

class CombinedMultiplier extends Module{
  val io = IO(new Bundle {
    val a = Input(UInt(DATA_WIDTH.W))
    val b = Input(UInt(DATA_WIDTH.W))
    val op = Input(UInt(3.W))
    val intOut = Output(UInt(DATA_WIDTH.W))
    val fpOut = Output(UInt(DATA_WIDTH.W))
  })
} 