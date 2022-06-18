package vector
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._
import scala.collection.immutable.ListMap

class EMULCalcSpec extends AnyFlatSpec with ChiselScalatestTester {
  "EMULCalc" should "pass" in {
    test(new EMULCalc).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val lmul = List(mf8, mf4, mf2, m1, m2, m4, m8)
      val sew = List(e8, e16, e32)
      val eew = List(e8, e16, e32)
      val lmulfp = List(0.125F, 0.25F, 0.5F, 1.0F, 2.0F, 4.0F, 8.0F)
      val sewfp = List(8.0F, 16.0F, 32.0F)
      val eewfp = List(8.0F, 16.0F, 32.0F)
      val mapfpbin = ListMap(0.125F -> mf8, 0.25F -> mf4, 0.5F -> mf2, 1.0F -> m1, 2.0F -> m2, 4.0F -> m4, 8.0F -> m8)


      def fp2bin(i: Int, j: Int, k: Int): (UInt, Boolean) ={
        val emul = eewfp(j)/sewfp(k)*lmulfp(i)
        val invalid = (emul < 0.125F) || (emul > 8.0F)
        if(invalid){
          (0x69.U, true)
        } else{
          (mapfpbin(emul),false)
        }
      }
      dut.io.en.poke(true.B)
      for(i <- lmul.indices){
        for(j <- eew.indices){
          for(k <- sew.indices){
            dut.io.lmul.poke(lmul(i))
            dut.io.eew.poke(eew(j))
            dut.io.sew.poke(sew(k))
            dut.clock.step()
            val (emul, ill) = fp2bin(i, j, k)
            if(ill){
              dut.io.invalid.expect(true.B)
            } else{
              dut.io.invalid.expect(false.B)
              dut.io.emul.expect(emul)
            }

          }
        }
      }
    }
  }
}