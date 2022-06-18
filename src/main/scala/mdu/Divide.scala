package mdu

import chisel3._
import chisel3.util._


// Calculates a / b

class Divide extends Module{
  val io = IO(new Bundle{
    val a,b = Input(SInt(32.W))
    val en, valid = Input(Bool())
    val op = Input(UInt(5.W))
    val y = Output(SInt(36.W))
    val ready = Output(Bool())
  })

  // Registers
  val R = RegInit(0.U(65.W)) // Remainder
  val M = RegInit(0.U(64.W)) // Divisor
  val op = RegInit(0.U(5.W)) // Operation
  val Q = RegInit(0.U(64.W)) // Dividend, but later the quotient
  val N = RegInit(0.U(7.W)) // Counter
  val e = RegInit(0.U(8.W)) // Exponent
  val s_a = RegInit(false.B) // Sign of a
  val s_b = RegInit(false.B) // Sign of b
  val m = RegInit(0.U(27.W)) // Mantissa
  val signIdx = RegInit(0.U(6.W))
  val sliceSel = RegInit(0.U(2.W))


  // Wiring for divider
  //   Initialize registers
  val initQ = Mux(io.op(2), (( 1.U ## io.a(22, 0)) << 23).asUInt, Mux(io.op(0) & io.a(31), (~io.a).asUInt + 1.U, io.a.asUInt))
  val initM = Mux(io.op(2), 1.U ## io.b(22,0), Mux(io.op(0) & io.b(31), (~io.b).asUInt + 1.U, io.b.asUInt))
  val initSignIdx = Mux(io.op(2), 46.U, MuxLookup(io.op(4,3), 31.U, Array(0.U -> 7.U, 1.U -> 15.U)))

  val initN = initSignIdx + 1.U
  val initSliceSel = Mux(io.op(2), 3.U, io.op(4,3))
  //   Masking remainder and quotient
  val sliceR = MuxLookup(sliceSel, R(46, 0), Array(0.U -> R(7, 0), 1.U -> R(15, 0), 2.U -> R(31, 0)))
  val sliceQ = MuxLookup(sliceSel, Q(45, 0), Array(0.U -> Q(6, 0), 1.U -> Q(14, 0), 2.U -> Q(30, 0)))
  //   Next remainder and quotient
  val nextR = sliceR ## Q(signIdx) +  Mux(R(signIdx + 1.U), M, -M)
  val nextQ = sliceQ ## (~nextR(signIdx + 1.U)).asUInt
  //   Float normalization
  val m_unorm = Q(24, 0) ## R(24, 22)
  val distMeas = PriorityEncoder(Reverse(m_unorm))
  val distExpct = 1.U

  // Divider
  val load :: divide :: normalize :: convert :: Nil = Enum(4)
  val state = RegInit(load)

  when(io.en){
    switch(state){
      is(load){
        op := io.op
        M := initM
        R := 0.U
        Q := initQ
        N := initN
        signIdx := initSignIdx
        sliceSel := initSliceSel
        e := (io.a(30, 23) - io.b(30, 23)) + 127.U
        s_a := io.a(31)
        s_b := io.b(31)
        when(io.valid){
          state := divide
        }
      }
      is(divide){
        N := N - 1.U
        R := Mux(N === 1.U & nextR(signIdx + 1.U), nextR + M, nextR) & (~(1.U << (signIdx + 2.U))).asUInt // Maybe there is a better solution to remove overflow bit
        Q := nextQ
        when(N === 1.U){
          state := Mux(op(2), normalize, Mux(op(0), convert, load))
        }
      }
      is(convert){
        when(s_a){
          R := (~R).asUInt + 1.U
        }
        when(s_a ^ s_b){
          Q := (~Q).asUInt + 1.U
        }
        state := load
      }
      is(normalize){
        m := m_unorm
        when(distMeas > distExpct){
          m := (m_unorm << (distMeas - distExpct)).asUInt | (R(21, 0) =/= 0.U)
          e := e - (distMeas - distExpct)
        } . elsewhen(distMeas < distExpct){
          m := (m_unorm >> (distExpct - distMeas)).asUInt | (R(21, 0) =/= 0.U)
          e := e + (distExpct - distMeas)
        }
        state := load
      }
    }
  }
  io.y := Mux(op(2), ((s_a ^ s_b) ## e ## m).asSInt, Mux(op(1), R, Q).asSInt)
  io.ready := state === load
}