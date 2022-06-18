package fp

import chisel3._
import chisel3.util._
class FPSqrt extends Module{
  val io = IO(new Bundle{
    val a  = Input(UInt(32.W))
    val y = Output(UInt(36.W))
    val valid, en = Input(Bool())
    val ready = Output(Bool())
  })

  val load :: exponent :: rem :: root :: adjust :: normalize ::  Nil = Enum(6)
  val state = RegInit(load)
  val N = RegInit(0.U(6.W))
  val Q = RegInit(0.U(47.W))
  val R = RegInit(0.U(48.W))
  val D = RegInit(0.U(47.W))
  val E = RegInit(0.U(8.W))
  val nextR = (R << 2).asUInt | ((D >> (N ## 0.U).asUInt).asUInt & 3.U).asUInt
  io.ready := state === load
  io.y := ((0.U ## E ## 1.U ## Q(22, 0)) ## R(23, 22) ## (R(21, 0) =/= 0.U))
  val distMeas = PriorityEncoder(Reverse(Q))
  val distExpct = 23.U

  when(io.en){
    switch(state){
      is(load){
        N := 23.U
        D := 1.U ## io.a(22, 0) ## 0.U(23.W)
        R := 0.U
        Q := 0.U
        E := io.a(30,23) - 127.U
        when(io.valid){
          state := exponent
        }
      }
      is(exponent){
        when(!E(0)){ // Exponent is an even number
          E :=  (0.U ## (E >> 1).asUInt) + 127.U
        } . otherwise{
          D := D >> 1
          E := (0.U ## ((E + 1.U) >> 1).asUInt) + 127.U
        }
        state := rem
      }
      is(rem){
        R := Mux(R(24), nextR + ((Q << 2).asUInt | 3.U), nextR - ((Q << 2).asUInt | 1.U))
        state := root
      }
      is(root){
        Q := (Q << 1).asUInt | ~R(24)
        N := N - 1.U
        state := Mux(N === 0.U, adjust, rem)
      }
      is(adjust){
        when(R(24)){
          R := R + ((Q << 1).asUInt | 1.U)
        }
        state := normalize
      }
      is(normalize){
        when(distMeas > distExpct){
          Q := (Q << (distMeas - distExpct))
          E := E - (distMeas - distExpct)
        } . elsewhen(distMeas < distExpct){
          Q := (Q >> (distExpct - distMeas))
          E := E + (distExpct - distMeas)
        }
        state := load
      }
    }
  }
}