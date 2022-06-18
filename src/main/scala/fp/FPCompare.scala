package fp

import chisel3._
import chisel3.util._
import utility.Constants._


class FPCompare extends Module{
  val io = IO(new Bundle{
    val op = Input(UInt(4.W))
    val a,b = Input(UInt(32.W)) // a: rs1, b: rs2
    val y = Output(UInt(32.W))
    val NV = Output(Bool())
  })
  val FMAX = "b000".U
  val FMIN = "b001".U
  val FEQ = "b010".U
  val FLT = "b011".U
  val FLE = "b100".U
  val aLargest = WireDefault(false.B)
  val bLargest = WireDefault(false.B)
  val aSNaN = io.a.asUInt === SNAN & io.a.asUInt =/= INF
  val aNaN = io.a.asUInt === NAN & io.a.asUInt =/= INF
  val bSNaN = io.b.asUInt === SNAN & io.b.asUInt =/= INF
  val bNaN = io.b.asUInt === NAN & io.b.asUInt =/= INF

  io.NV := false.B
  io.y := 0.U

  when(io.a(31) ^ io.b(31)){ // Simple case
    aLargest:= !io.a(31)
    bLargest := io.a(31)
  } .elsewhen(io.a(30,23) > io.b(30,23)){ // Check exponent
    aLargest := true.B
    bLargest := false.B
  } .elsewhen(io.b(30,23) > io.a(30,23)){
    aLargest := false.B
    bLargest := true.B
  } .elsewhen(io.a(22, 0) > io.b(22, 0)){ // Check mantissa
    aLargest := true.B
    bLargest := false.B
  } .elsewhen(io.b(22, 0) > io.a(22, 0)){
    aLargest := false.B
    bLargest := false.B
  }

  switch(io.op(2, 0)){
    is(FMAX){
      io.y := Mux(aLargest, io.a, io.b)
      when(aSNaN | bSNaN) {
        io.NV := true.B
      }
      when(!aNaN & bNaN){
        io.y := io.a
      }
      when(aNaN & !bNaN){
        io.y := io.b
      }
      when(aNaN & bNaN){
        io.y := CNAN
      }
    }
    is(FMIN){
      io.y := Mux(aLargest, io.b, io.a)
      when(aSNaN | bSNaN) {
        io.NV := true.B
      }
      when(!aNaN & bNaN){
        io.y := io.a
      }
      when(aNaN & !bNaN){
        io.y := io.b
      }
      when(aNaN & bNaN){
        io.y := CNAN
      }
    }
    is(FEQ){
      io.y := Mux(aNaN | bNaN, 0.U, (!(aLargest | bLargest)))
      when(aSNaN | bSNaN){
        io.NV := true.B
      }
    }
    is(FLT){
      io.y := bLargest
      when(aNaN | bNaN){
        io.y := 0.U
        io.NV := true.B
      }
    }
    is(FLE){
      io.y := (!(aLargest | bLargest) | bLargest)
      when(aNaN | bNaN){
        io.y := 0.U
        io.NV := true.B
      }
    }
  }

}
