package fp

import chisel3._
import InvLookUp._
import scala.math.pow

object InvLookUp{
  def FPDiv(x: BigInt, y: BigInt): BigInt={
    (x << 23)/y
  }
  def invLUT(width: Int = 5): Array[Int]={
    val a: BigInt = 0x800000
    var b: BigInt = 0x800000
    var max = 1
    for(i <- 0 until width){
      max = (max<< 1) | 1
    }
    val arr = Array.ofDim[Int](max + 1)
    for(i <- 0 to max){
      b = 0x800000 + (i << 18)
      arr(i) = FPDiv(a,b).toInt
    }
    arr
  }
}
class InvLookUp(width: Int = 5) extends Module{
  val io = IO(new Bundle{
    val mant = Input(UInt(width.W))
    val invMant = Output(UInt(24.W))
  })
  val arr: Array[Int] = invLUT()
  val iLUT = VecInit(arr.map(_.S(32.W)))
  io.invMant := iLUT(io.mant(4,0)).asUInt
}