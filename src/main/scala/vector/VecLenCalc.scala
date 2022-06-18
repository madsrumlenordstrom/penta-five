package vector
import chisel3._
import chisel3.util._
import utility.Constants._
class VecLenCalc extends Module{
  val io = IO(new Bundle{
    val vlmul, vsew = Input(UInt(3.W))
    val avl = Input(UInt(XLEN.W))
    val vlmax = Output(UInt(log2Up(VLEN + 1).W))
    val illegal = Output(Bool())
    val vl = Output(UInt(log2Up(VLEN + 1).W))
    val en = Input(Bool())
  })
  val vlmax = RegInit(0.U(log2Up(VLEN+1).W))
  val avl = RegInit(0.U(XLEN.W))
  val illegal = RegInit(false.B)
  val lmulShifts = MuxLookup(io.vlmul, 4.U, Array(mf8 -> 3.U, mf4 -> 2.U, mf2 -> 1.U, m1 -> 0.U, m2 -> 1.U, m4 -> 2.U, m8 -> 3.U))
  val sewShifts = MuxLookup(io.vsew, 0.U, Array(e8 -> 3.U, e16 -> 4.U, e32 -> 5.U))
  when(io.en){
    vlmax := Mux(io.vlmul <= m8, (VLEN.U >> sewShifts) << lmulShifts, VLEN.U >> (sewShifts + lmulShifts)).asUInt
    avl := io.avl
    illegal := lmulShifts === 4.U | sewShifts === 0.U | ((io.vsew === e32) & (io.vlmul === mf8))
  }
  io.vlmax := vlmax
  // Either unsupported lmul / sew or just invalid combination
  io.illegal := illegal

  // Simple vector length determination
  io.vl := vlmax
  when(avl <= vlmax){
    io.vl := avl
  }
}
