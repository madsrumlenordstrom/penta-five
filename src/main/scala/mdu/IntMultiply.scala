package mdu

import chisel3._
import chisel3.util._
import MDUConstants._
import utility.Constants._
import utility.Controlsignals._

class BitSerialMultiply(width: Int) extends Module{
    /*
     * Module that computes product of two integers in 2 cycles
     */
    // IO for unsigned multiplier
    class unsignedIO(width: Int) extends Bundle{
        val upper = Input(Bool()) // Return upper half?
        val a, b = Input(UInt(width.W))
        val res = Output(SInt(width.W))
    }

    /*
    // IO for signed multiplier
    class signedIO(width: Int) extends unsignedIO(width){
      val signed = Input(Bool())
    }
     */
    val io = IO(new unsignedIO(width))

    val result = RegInit(0.U((2*width).W))
    val counter = RegInit(0.U(log2Ceil(width).W))

    val a = Wire(UInt(width.W))
    val bIdx = Wire(Bool())
    bIdx := io.b(counter) // Index of value b
    when(bIdx){
        a := io.a
    }.otherwise{
        a := 0.U
    }
    val shifted = Wire(UInt((2*width).W))
    shifted := result >> 1.U

    when(counter === width.U){
        counter := 0.U
        result := 0.U((2*width).W)
    }.otherwise{
        counter := counter + 1.U
        result := shifted + (a << width.U)
    }

    /*
    val LL = Reg(UInt(width.W))
    val LH = Reg(UInt(width.W))
    val HL = Reg(UInt(width.W))
    val HH = Reg(UInt(width.W))

    val aUpper = io.a(width - 1, width/2)

    val aLower = io.a(width/2 - 1, 0)

    val bUpper = io.b(width - 1, width/2)

    val bLower = io.b(width/2 -1 , 0)

    LL := aLower * bLower
    LH := aLower * bUpper
    HL := aUpper * bLower
    HH := aUpper * bUpper


    val result = Cat((HH - LL),(HL + LH))
    //val result = (HH ## LL) + (HL ## 0.U(width/2)) + (LH ## 0.U(width/2))
    */
    when(io.upper){
        io.res := shifted(2*width - 1, width).asSInt
    }. otherwise{
        io.res := shifted(width - 1, 0).asSInt
    }


    //io.res := Mux(io.upper, result(width - 1, 0), result(2*width - 1, width)) // TODO option to hardwire output to either upper or lower

    /*
    // Check if signed multiplier should be generated
    if (includeSign){
      val io = IO(new signedIO(width))

      val LL = Reg(UInt(width.W))
      val LH = Reg(SInt((width + 1).W))
      val HL = Reg(SInt((width + 1).W))
      val HH = Reg(UInt(width.W))

      val aUpper = Mux(io.signed, io.a(width - 1), 0.U(1.W)) ## io.a(width - 1, width/2).asSInt

      val aLower = io.a(width/2 - 1, 0)

      val bUpper = Mux(io.signed, io.b(width - 1), 0.U(1.W)) ## io.b(width - 1, width/2).asSInt

      val bLower = io.b(width/2 -1 , 0)

      LL := aLower * bLower
      LH := aLower * bUpper
      HL := aUpper * bLower
      HH := (aUpper * bUpper).asSInt

      val result = (HH ## LL).asSInt + (HL ## 0.U(width/2)).asSInt + (LH ## 0.U(width/2)).asSInt

      io.res := Mux(io.upper, result(width/2 - 1, 0), result(width - 1, width/2)) // TODO option to hardwire output to either upper or lower

    } else {
      val io = IO(new unsignedIO(width))

      val LL = Reg(UInt(width.W))
      val LH = Reg(UInt(width.W))
      val HL = Reg(UInt(width.W))
      val HH = Reg(UInt(width.W))

      val aUpper = io.a(width - 1, width/2)

      val aLower = io.a(width/2 - 1, 0)

      val bUpper = io.b(width - 1, width/2)

      val bLower = io.b(width/2 -1 , 0)

      LL := aLower * bLower
      LH := aLower * bUpper
      HL := aUpper * bLower
      HH := aUpper * bUpper

      val result = (HH ## LL) + (HL ## 0.U(width/2)) + (LH ## 0.U(width/2))

      io.res := Mux(io.upper, result(width/2 - 1, 0), result(width - 1, width/2)) // TODO option to hardwire output to either upper or lower
    }

     */
}

class DigitSerialMultiply32 extends Module{
    /*
    Digit serial multiplier with 4 stages for 32 bit numbers
     */
    val io = IO(new Bundle{
        val upper = Input(Bool())
        val a, b = Input(UInt(32.W))
        val res = Output(UInt(32.W))
    })

    class StageReg(stageN: Int) extends Bundle{
        val upper = Bool()
        val a = UInt(32.W)
        val b = UInt((32 - (stageN*8)).W)
        val res = UInt((32 + (stageN*8)).W)
    }

    class StageIO(stageN: Int) extends Bundle{
        val upperIn = Input(Bool())
        val upperOut = Output(Bool())
        val aOut = Output(UInt(32.W))
        val bOut = Output(UInt((32 - (stageN*8)).W))
        val aIn = Input(UInt(32.W))
        val bIn = Input(UInt((32 - ((stageN - 1)*8)).W))
        val resIn = Input(UInt((32 + ((stageN - 1)*8)).W))
        val resOut = Output(UInt((32 + (stageN*8)).W))
    }

    class Wires(stageN: Int) extends Bundle {
        val bIdx = Vec(8, Bool())
        val addVal = Vec(8, UInt(32.W))
        val res = Vec(8, UInt((32 + (stageN*8)).W))
    }

    class Stage(stageN: Int) extends Module {
        val io = IO(new StageIO(stageN))

        val PipeReg = Reg(new StageReg(stageN))
        val Wires = Wire(new Wires(stageN))

        for (i <- 0 until 8){
            Wires.bIdx(i) := io.bIn(i)
            when(Wires.bIdx(i)){
                Wires.addVal(i) := io.aIn
            }. otherwise{
                Wires.addVal(i) := 0.U
            }
            Wires.res(i) := Wires.addVal(i) << (i + ((stageN - 1)*8)).U
        }
        // Pipeline register assignment
        PipeReg.a := io.aIn
        PipeReg.b := (io.bIn >> (stageN * 8).U) // TODO
        PipeReg.upper := io.upperIn
        PipeReg.res :=
            Wires.res(0) +
                Wires.res(1) +
                Wires.res(2) +
                Wires.res(3) +
                Wires.res(4) +
                Wires.res(5) +
                Wires.res(6) +
                Wires.res(7) +
                io.resIn

        // Outputs
        io.aOut := PipeReg.a
        io.bOut := PipeReg.b
        io.upperOut := PipeReg.upper
        io.resOut := PipeReg.res
    }

    //-------------------------------------------------------------//
    // Stage 1
    val stage1 = Module(new Stage(1))
    stage1.io.upperIn := io.upper
    stage1.io.aIn := io.a
    stage1.io.bIn := io.b
    stage1.io.resIn := 0.U

    //-------------------------------------------------------------//
    // Stage 2
    val stage2 = Module(new Stage(2))
    stage2.io.upperIn := stage1.io.upperOut
    stage2.io.aIn := stage1.io.aOut
    stage2.io.bIn := stage1.io.bOut
    stage2.io.resIn := stage1.io.resOut

    //-------------------------------------------------------------//
    // Stage 3
    val stage3 = Module(new Stage(3))
    stage3.io.upperIn := stage2.io.upperOut
    stage3.io.aIn := stage2.io.aOut
    stage3.io.bIn := stage2.io.bOut
    stage3.io.resIn := stage2.io.resOut

    //-------------------------------------------------------------//
    // Stage 4

    val Wires = Wire(new Wires(4))

    for (i <- 0 until 8){
        Wires.bIdx(i) := stage3.io.bOut(i)
        when(Wires.bIdx(i)){
            Wires.addVal(i) := stage3.io.aOut
        }. otherwise{
            Wires.addVal(i) := 0.U
        }
        Wires.res(i) := Wires.addVal(i) << (i*4).U
    }
    val finalRes = Wire(UInt(64.W))
    finalRes :=
        Wires.res(0) +
            Wires.res(1) +
            Wires.res(2) +
            Wires.res(3) +
            Wires.res(4) +
            Wires.res(5) +
            Wires.res(6) +
            Wires.res(7) +
            stage3.io.resOut

    // Outputs
    io.res := Mux(stage3.io.upperOut,finalRes(63,32),finalRes(31,0))
}

class DigitSerialMultiply24 extends Module{

    // Module to multiply to 24 bit numbers. It is used in float multiply

    val io = IO(new Bundle{
        val a, b = Input(UInt(24.W))
        val en = Input(Bool())
        val res = Output(UInt(48.W))
    })


    class StageReg extends Bundle{
        val a = UInt(24.W)
        val b = UInt(12.W)
        val res = UInt(36.W)
    }

    class Wires(stageN: Int) extends Bundle{
        val bIdx = Vec(12, Bool())
        val addVal = Vec(12, UInt((24 + (12*stageN)).W))
        val sum0 = Vec(6, UInt((24 + (12*stageN)).W))
        val sum1 = Vec(3, UInt((24 + (12*stageN)).W))
    }

    //val bIdx = Wire(Vec(12, Bool()))
    //val addVal = Wire(Vec(12, Bool()))
    val PipeReg = RegInit(0.U.asTypeOf(new StageReg))
    val Wires1 = Wire(new Wires(1))
    //val sum0 = Wire(Vec(6,UInt(36.W)))
    //val sum1 = Wire(Vec(3,UInt(36.W)))

    for (i <- 0 until 12){

        Wires1.bIdx(i) := io.b(i)

        when(Wires1.bIdx(i)){
            Wires1.addVal(i) := io.a << i.U
        }. otherwise{
            Wires1.addVal(i) := 0.U
        }

        if(i % 2 == 0){
            Wires1.sum0(i/2) := Wires1.addVal(i) + Wires1.addVal(i + 1)
        }
        if (i % 4 == 0){
            Wires1.sum1(i/4) := Wires1.sum0(i/2) + Wires1.sum0(i/2 + 1)
        }
    }

    when(io.en){
        PipeReg.a := io.a
        PipeReg.b := io.b(23,12)
        PipeReg.res := Wires1.sum1(0) + Wires1.sum1(1) + Wires1.sum1(2)
    }. otherwise {
        PipeReg.a := PipeReg.a
        PipeReg.b := PipeReg.b
        PipeReg.res := PipeReg.res
    }

    val Wires2 = Wire(new Wires(2))

    for (i <- 0 until 12){
        Wires2.bIdx(i) := PipeReg.b(i)

        when(Wires2.bIdx(i)){
            Wires2.addVal(i) := PipeReg.a << (i + 12).U
        }. otherwise{
            Wires2.addVal(i) := 0.U
        }
        //Wires2.addVal(i) := (PipeReg.a & Wires2.bIdx(i)) << (i + 12).U
        if(i % 2 == 0){
            Wires2.sum0(i/2) := Wires2.addVal(i) + Wires2.addVal(i + 1)
        }
        if (i % 4 == 0){
            Wires2.sum1(i/4) := Wires2.sum0(i/2) + Wires2.sum0(i/2 + 1)
        }
    }
    io.res := Wires2.sum1(0) + Wires2.sum1(1) + Wires2.sum1(2) + PipeReg.res
}

class IntMultiplier extends Module{
    // Multiply module for execution stage
    class IntMultiplierIO extends Bundle{
        val a, b = Input(UInt(DATA_WIDTH.W))
        val op = Input(UInt(OP.WIDTH.W))
        val en = Input(Bool())
        val res = Output(UInt((DATA_WIDTH).W))
    }

    class QuarterMultiplyIO extends Bundle{
        val a = Input(UInt(DATA_WIDTH.W))
        val b = Input(UInt((DATA_WIDTH/4).W))
        val res = Output(UInt((DATA_WIDTH + (DATA_WIDTH/4)).W))
    }

    class QuarterMultiplyWires extends Bundle{
        val bIdx = Vec(DATA_WIDTH/4, Bool())
        val addVal = Vec(DATA_WIDTH/4, UInt(DATA_WIDTH.W))
        val sums0 = Vec(DATA_WIDTH/8, UInt((DATA_WIDTH + 2).W))
        val sums1 = Vec(DATA_WIDTH/16, UInt((DATA_WIDTH + 4).W))
    }

    class QuarterMultiply extends Module{
        // Used for multiplying a large number by a another number quarter of its width
        val io = IO(new QuarterMultiplyIO)

        val wires = Wire(new QuarterMultiplyWires)

        for (i <- 0 until DATA_WIDTH/4){
            wires.bIdx(i) := io.b(i)
            when(wires.bIdx(i)){
                wires.addVal(i) := io.a
            }. otherwise{
                wires.addVal(i) := 0.U
            }
            if (i%2 == 0){
                wires.sums0(i/2) := wires.addVal(i) +& (wires.addVal(i + 1) << 1.U).asUInt
            }
            if (i%4 == 0){
                wires.sums1(i/4) := wires.sums0(i/2) +& (wires.sums0(i/2 + 1) << 2.U).asUInt
            }
        }
        io.res := wires.sums1(0) + (wires.sums1(1) << 4.U)
    }

    // IO 
    val io = IO(new IntMultiplierIO)

    //-------------------------------------------------------------//
    // Stage 1
    // First step is to decode instruction and convert signed numbers to unsigned

    val uIntA, uIntB = WireDefault(0.U(DATA_WIDTH.W))
    val negA, negB = WireDefault(false.B)
    val resSign = Wire(Bool())
    val upper1Next = Wire(Bool())
    val upper1 = RegEnable(next = upper1Next, init = false.B, enable = io.en)

    when(io.op === MUL || io.op === MULH || io.op === MULHSU){
        when(io.a(DATA_WIDTH - 1)){
            uIntA := (~io.a) + 1.U
            negA := true.B
        }.otherwise{
            uIntA := io.a
            negA := false.B
        }
        when(io.op === MUL || io.op === MULH){
            when(io.b(DATA_WIDTH - 1)){
                uIntB := (~io.b) + 1.U
                negB := true.B
            }. otherwise{
                uIntB := io.b
                negB := false.B
            }
        }.otherwise{
            // MULHSU
            uIntB := io.b
            negB := false.B
        }
    }.elsewhen(io.op === MULHU){
        uIntA := io.a
        negA := false.B
        uIntB := io.b
        negB := false.B
    }

    val uIntRegA = RegEnable(next = uIntA, init = 0.U, enable = io.en)
    val uIntRegB = RegEnable(next = uIntB, init = 0.U, enable = io.en)
    
    // Determine if upper part is returned
    when (io.op === OP.MULH || io.op === OP.MULHU || io.op === OP.MULHSU){
        upper1Next := true.B // RegEnable(next = true.B, init = false.B, enable = io.en)
    }. otherwise {
        upper1Next := false.B // RegEnable(next = false.B, init = false.B, enable = io.en)
    }

    resSign := RegEnable(next = negA ^ negB, init = false.B, enable = io.en)

    //-------------------------------------------------------------//
    // Stage 2
    // Second stage the multiplication begins

    val partProds = Array.fill(4)(Module (new QuarterMultiply))
    val pipeReg = Array.fill(4)(Wire(UInt((DATA_WIDTH + (DATA_WIDTH/4)).W)))

    for (i <- 0 until 4){
        partProds(i).io.a := uIntRegA
        partProds(i).io.b := uIntRegB((i + 1)*DATA_WIDTH/4 - 1, i*DATA_WIDTH/4)
        pipeReg(i) := RegEnable(next = partProds(i).io.res, enable = io.en)
    }

    val signReg = RegEnable(next = resSign, init = false.B, enable = io.en)
    val upper2 = RegEnable(next = upper1, init = false.B, enable = io.en)

    //-------------------------------------------------------------//
    // Stage 3
    // Add partial products and converts numbers to 2's complement if necessary

    val sums = Wire(Vec(2, UInt((DATA_WIDTH + (DATA_WIDTH/2)).W)))
    val finalSum = Wire(UInt((2*DATA_WIDTH).W))
    val finalResNext = Wire(UInt(DATA_WIDTH.W))
    val finalRes = RegEnable(next = finalResNext, enable = io.en)
    
    sums(0) := pipeReg(0) + (pipeReg(1) << (DATA_WIDTH/4).U)
    sums(1) := pipeReg(2) + (pipeReg(3) << (DATA_WIDTH/4).U)
    
    when(signReg){
        finalSum := ~(sums(0) +& (sums(1) << (DATA_WIDTH/2).U).asUInt) + 1.U
    }. otherwise{
        finalSum := sums(0) +& (sums(1) << (DATA_WIDTH/2).U).asUInt
    }
    
    when(upper2){
        finalResNext := finalSum(2*DATA_WIDTH - 1, DATA_WIDTH)
    }.otherwise{
        finalResNext := finalSum(DATA_WIDTH - 1, 0)
    }
    
    io.res := finalRes
}