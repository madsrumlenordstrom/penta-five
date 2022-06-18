package scalarcore

import chisel3._
import chisel3.util._

import utility.Controlsignals._
import utility.Constants._

class ALU32I extends Module{
  val io = IO(new Bundle{
    val op = Input(UInt(OP.WIDTH.W))
    val a = Input(UInt(DATA_WIDTH.W))
    val b = Input(UInt(DATA_WIDTH.W))
    val y = Output(UInt(DATA_WIDTH.W))
    val eq = Output(Bool())
    val lt = Output(Bool())
    val ltu = Output(Bool())
  })

  val a = io.a
  val b = io.b
  val shamt = b(4,0)
  val y = WireDefault(0.U(DATA_WIDTH.W))


  io.y := MuxCase(DontCare, Seq(
    (io.op === OP.ADD) -> ((a.asSInt + b.asSInt).asUInt),
    (io.op === OP.SUB) -> ((a.asSInt - b.asSInt).asUInt),
    (io.op === OP.AND) -> (a & b),
    (io.op === OP.OR)  -> (a | b),
    (io.op === OP.XOR) -> (a ^ b),
    (io.op === OP.SLT) -> (io.lt),
    (io.op === OP.SLL) -> (a << shamt),
    (io.op === OP.SLTU)-> (io.ltu),
    (io.op === OP.SRL) -> (a >> shamt),
    (io.op === OP.SRA) -> ((a.asSInt >> shamt).asUInt),
  ))

  io.eq := a === b
  io.lt := a.asSInt < b.asSInt
  io.ltu := a < b
}

