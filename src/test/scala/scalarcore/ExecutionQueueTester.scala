package scalarcore
import org.scalatest.flatspec.AnyFlatSpec
import chiseltest._

class ExecutionQueueTester extends AnyFlatSpec with ChiselScalatestTester {
  val garbage = 100
  "ExecutionQueue" should "pass" in{
    test(new ExecutionQueue(garbage)).withAnnotations(Seq(WriteVcdAnnotation)){ dut =>

    }

  }
}
