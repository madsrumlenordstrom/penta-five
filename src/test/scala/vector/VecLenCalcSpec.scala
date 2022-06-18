package vector
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._


class VecLenCalcSpec extends AnyFlatSpec with ChiselScalatestTester {
  "Vector Manager" should "pass" in {
    test(new VecLenCalc).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      def step(steps: Int = 1): Unit = {
        dut.clock.step(steps)
      }
      def printSetting(i: Int, j: Int): Unit={
        var sew = ""
        i match{
          case 0 => sew = "e8"
          case 1 => sew = "e16"
          case 2 => sew = "e32"
        }
        var lmul = ""
        j match{
          case 0 => lmul = "mf8"
          case 1 => lmul = "mf4"
          case 2 => lmul = "mf2"
          case 3 => lmul = "m1"
          case 4 => lmul = "m2"
          case 5 => lmul = "m4"
          case 6 => lmul = "m8"
        }
        if(dut.io.illegal.peek().litToBoolean){println("ILLEGAL")}
        println("vlen = " + VLEN)
        println("sew = " + sew + "\nlmul = " + lmul)
        println("vlmax = " + dut.io.vlmax.peek().litValue.toString)
        println("-------------------------")
      }
      val lmuls = List(mf8, mf4, mf2, m1, m2, m4, m8)
      val sews = List(e8, e16, e32)
      for(i <- sews.indices){
        dut.io.vsew.poke(sews(i))
        for(j <- lmuls.indices){
          dut.io.vlmul.poke(lmuls(j))
          dut.clock.step()
          printSetting(i, j)
        }
      }
    }
  }
}