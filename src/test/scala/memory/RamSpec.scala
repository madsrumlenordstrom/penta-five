package memory

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class RamSpec extends AnyFlatSpec with ChiselScalatestTester {
  val data = "data.txt"
  "SyncRAM" should "pass" in {
    test(new SyncRAM(128, data)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 to 5) {
        dut.io.addr.poke(i.U)
        dut.clock.step(1)
      }
    }
  }
}