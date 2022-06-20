
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._


class PentaSpec extends AnyFlatSpec with ChiselScalatestTester {

  //memSize: Int, maxBSize: Int, bSizeD: Int, linesD: Int, bSizeI: Int, linesI: Int, maxDelay: Int

  val memSize = 512*8
  val bSizeI = 64
  val linesI = 4
  val maxDelay = 56
  val program = "program.txt"
  "PentaFive" should "pass" in {
    test(new Penta(memSize, bSizeI, linesI, maxDelay, program)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(250)
      dut.clock.step(80)
    }
  }
}