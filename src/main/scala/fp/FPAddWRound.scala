package fp
import chisel3._

class FPAddWRound extends Module{
  val io = IO(new Bundle{
    val a, b = Input(SInt(32.W))
    val sub, en = Input(Bool())
    val y = Output(UInt(32.W))
  })
  val addFP = Module(new FPAdd)
  val roundFP = Module(new FPRounder)
  addFP.io.a := io.a
  addFP.io.b := io.b
  addFP.io.sub := io.sub
  addFP.io.en := io.en
  roundFP.io.rm := 0.U
  roundFP.io.en := io.en
  roundFP.io.a := addFP.io.y
  io.y := roundFP.io.y


}