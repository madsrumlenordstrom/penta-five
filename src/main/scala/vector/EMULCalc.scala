package vector
import chisel3._
import chisel3.util._
import utility.Constants._
class EMULCalc extends Module {
  val io = IO(new Bundle{
    val eew = Input(UInt(3.W))
    val sew = Input(UInt(3.W))
    val lmul = Input(UInt(3.W))
    val emul = Output(UInt(3.W))
    val invalid = Output(Bool())
    val en = Input(Bool())
  })
  io.emul := io.lmul
  val shift = io.lmul > m8
  val lmul = io.lmul
  val eew = io.eew
  val sew = io.sew
  val illegalEmul = ((lmul === mf8) & (eew < sew)) | (lmul === mf4 & (eew === e8 ) & (sew === e32)) | (lmul === m4 & (eew === e32) & (sew === e8)) | (lmul === m8 & (eew > sew))
  val isFrac = lmul(2)
  val shiftsA = WireDefault(0.U)
  val shiftsB = WireDefault(0.U)
  val emulFixed = RegInit(0.U(9.W))
  val eewFixed = MuxLookup(eew, 8.U(6.W), Array(e16 -> 16.U(6.W), e32 -> 32.U(6.W))) ## 0.U(3.W)

  when(isFrac){
    switch(lmul(1,0)){
      is(1.U){ shiftsA := 3.U }
      is(2.U){ shiftsA := 2.U }
      is(3.U){ shiftsA := 1.U }
    }
  } .otherwise{
    shiftsA:= lmul(1,0)
  }
  switch(sew){
    is(e8){ shiftsB := 3.U}
    is(e16){ shiftsB := 4.U}
    is(e32){shiftsB := 5.U}
  }
  when(io.en){
    emulFixed := Mux(isFrac, eewFixed >> (shiftsA + shiftsB), eewFixed >> (shiftsB - shiftsA))
  }
  io.emul := Mux(emulFixed(6,3) === 0.U, MuxLookup(emulFixed(2,0), mf8, Array(2.U(3.W) -> mf4, 4.U(3.W) -> mf2)),MuxLookup(emulFixed(6,3), m1, Array(2.U(4.W) -> m2, 4.U(4.W) -> m4, 8.U(4.W) -> m8)))
  io.invalid := illegalEmul


}
