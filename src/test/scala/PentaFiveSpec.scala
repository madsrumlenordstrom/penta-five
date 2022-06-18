import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._
class PentaFiveSpec extends AnyFlatSpec with ChiselScalatestTester {

  //memSize: Int, maxBSize: Int, bSizeD: Int, linesD: Int, bSizeI: Int, linesI: Int, maxDelay: Int

  val memSize = 512
  val bSizeI = 64
  val linesI = 4
  val maxDelay = 56
  val program = "program.txt"
  "PentaFive" should "pass" in {
    test(new PentaFive(memSize, bSizeI, linesI, maxDelay, program)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(0)
      //dut.clock.step(1000000)
      var lastCSR = 0
      for (i <- 0 until 1000){
        dut.clock.step(1)
        //println(i.toHexString + ": " + dut.io.csr.peek().litValue.toInt)
        if(dut.io.csr.peek().litValue.toInt != lastCSR){
          lastCSR = dut.io.csr.peek().litValue.toInt
          println(i + ": " + lastCSR)
        }
      }
    }
  }
}
