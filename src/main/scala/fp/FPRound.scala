package fp

import chisel3._
import chisel3.util._
import FPConstants._

class UnroundedFP(manBits: Int) extends Bundle{
    val sign = Bool() // Kinda useless, but nice to have
    val exp = UInt(EXP_WIDTH.W)
    val man = UInt(manBits.W)
}

//https://www.slideshare.net/prochwani95/06-floating-point

class FPRound(manBits: Int) extends Module {
    val io = IO(new Bundle{
        val in = Input(new UnroundedFP(manBits))
        val rm = Input(UInt(3.W))
        val out = Output(new Float32)
    })

    val sticky = Wire(Bool()) // Least significand bits of the mantissa
    val r = Wire(Bool())
    val manMS = Wire(UInt(MAN_WIDTH.W)) // Most significand bits of the mantissa

    sticky := (io.in.man(manBits - MAN_WIDTH - 2,0) > 0.U).asBool // Sticky is all bits OR'ed together
    r := io.in.man(manBits - MAN_WIDTH - 1).asBool
    manMS := io.in.man(manBits - 1, manBits - MAN_WIDTH)

    io.out := io.in

    switch(io.rm){
        is(RNE){
            when(r && sticky){
                manMS + 1.U
            }
        }
        is(RTZ){
            // Apparently you don't do anything in this mode TODO check up on this
        }
        is(RDN){

        }
        is(RUP){

        }
        is(RMM){

        }
    }
}

object FPRoundMain extends App {
    emitVerilog(new FPRound(48), args)
}