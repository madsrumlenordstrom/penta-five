package fp
import chisel3._
import chisel3.util._
class FPSignInj extends Module{
  val io = IO(new Bundle{
    val a,b = Input(UInt(32.W)) // a = rs1, b = rs2
    val op = Input(UInt(4.W))
    val y = Output(UInt(32.W))
  })
  io.y := (io.b(31) ## io.a(30,0))
  when(!io.op(1) & io.op(0)){
    io.y := (~io.b(31) ## io.a(30,0))
  } .elsewhen(io.op(1) & !io.op(0)){
    io.y := ((io.b(31) ^ io.a(31)) ## io.a(30, 0))
  }
}
