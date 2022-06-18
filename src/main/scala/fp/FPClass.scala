package fp
import chisel3._
import utility.Constants._
class FPClass extends Module{
  val io = IO(new Bundle {
    val a = Input(UInt(32.W))
    val mask = Output(UInt(10.W))
  })
  val a = io.a
  when(a === NEGINF){
    io.mask := (1 << 0).U
  } .elsewhen(a === POSINF){
    io.mask := (1 << 7).U
  } .elsewhen(a === SNAN){
    io.mask := (1 << 8).U
  } .elsewhen(a === QNAN){
    io.mask := (1 << 9).U
  } .elsewhen(a === NEGZR){
    io.mask := (1 << 3).U
  } .elsewhen(a === POSZR){
    io.mask := (1 << 4).U
  } .elsewhen(a === NEGSUBNORM){
    io.mask := (1 << 2).U
  } .elsewhen(a === POSSUBNORM){
    io.mask := (1 << 6).U
  } .elsewhen(a(31)){ // Negative normal number
    io.mask := (1 << 1).U
  } .otherwise{ // Positive normal number
    io.mask := (1 << 5).U
  }
}