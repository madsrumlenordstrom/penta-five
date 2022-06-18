package fp
import chisel3._
import chisel3.util._
import FPConstants._

class FPRounder extends Module{
  val io = IO(new Bundle{
    val a = Input(UInt(36.W))
    val en = Input(Bool())
    val rm = Input(UInt(3.W))
    val y = Output(UInt(32.W))
  })
  val m_in = io.a(26,0)
  val e_in = io.a(34,27)
  val s_in = io.a(35)
  val m_out = WireDefault(m_in(25,3)) // Default is just truncation
  val e_out = WireDefault(e_in)

  val G = m_in(2)
  val R = m_in(1)
  val S = m_in(0)
  val y = RegInit(0.U(32.W))
  switch(io.rm){
    /*
      GRS =>
      0XX: round down (truncate)
      100: tie. mantissa(LSB)? round up: round down
      101: round up
      110: round up
      111: round up
     */
    is(RNE){
      when(G){
        when((!(R | S) & m_in(3)) | (R | S)){ // tie or normal round up
          m_out := (m_in + 4.U)(25,3) // TODO: Check for carry out. In the case of carry out, we need to normalize again
        }
      }
    }
    is(RDN){
      when(s_in & (G | R | S)){ // Negative values with non zero GRS must be rounded down (rounded up in magnitude)
        m_out :=  (m_in + 4.U)(25,3) // TODO: Check for carry out. In the case of carry out, we need to normalize again
      }
    }
    is(RUP){
      when(!s_in & (G | R | S)){ // Negative values with non zero GRS must be rounded up
        m_out :=  (m_in + 4.U)(25,3) // TODO: Check for carry out. In the case of carry out, we need to normalize again
      }
    }
    is(RMM){
      when(G){ // As it ties with max magnitude, we always round up if
        m_out := (m_in + 4.U)(25,3) // TODO: Check for carry out. In the case of carry out, we need to normalize again
      }
    }
  }
  when(io.en){
    y := s_in ## e_out ##  m_out
  }
  io.y := y
}

