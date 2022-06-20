/*package fp
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._
import java.lang.Float.{floatToIntBits, intBitsToFloat}
import java.util.concurrent.ThreadLocalRandom


class FPConvertWRound extends Module{
  val io = IO(new Bundle{
    val a = Input(UInt(32.W))
    val y = Output(UInt(32.W))
    val op = Input(UInt(2.W))
  })
  val converter = Module(new FPConvert)
  val rounder = Module(new FPRounder)
  converter.io.a := io.a
  converter.io.en := true.B
  converter.io.op := io.op
  rounder.io.en := true.B
  rounder.io.rm := 0.U
  rounder.io.a := converter.io.y.asUInt
  io.y := converter.io.y
  when(io.op(0)){
    io.y := rounder.io.y.asUInt
  }
}
class FPConvertSpec extends AnyFlatSpec with ChiselScalatestTester{
  val rounds = 20
  "FPConvert" should "pass" in{
    test(new FPConvertWRound).withAnnotations(Seq(WriteVcdAnnotation)){dut =>
      // Special cases first

      // Positive infinity
      dut.io.op.poke(CONVOP.FCVTWS)
      dut.io.a.poke(0x7F800000.U)
      dut.clock.step(1)
      dut.io.y.expect(2147483647.U)
      dut.io.op.poke(CONVOP.FCVTWUS)
      dut.clock.step(1)
      dut.io.y.expect((-1).U)
      // Negative infinity
      dut.io.op.poke(CONVOP.FCVTWS)
      dut.io.a.poke(0xFF800000.U)
      dut.clock.step(1)
      dut.io.y.expect((-2147483648).U)
      dut.io.op.poke(CONVOP.FCVTWUS)
      dut.clock.step(1)
      dut.io.y.expect(0.U)
      // Out of range positive input
      dut.io.op.poke(CONVOP.FCVTWS)
      dut.io.a.poke(floatToIntBits(2147483648L.toFloat).asUInt)
      dut.clock.step(1)
      dut.io.y.expect(2147483647.U)
      dut.io.op.poke(CONVOP.FCVTWUS)
      dut.io.a.poke(floatToIntBits(4294967296L.toFloat).asUInt)
      dut.clock.step(1)
      dut.io.y.expect((-1).U)
      // Out of range negative input
      dut.io.op.poke(CONVOP.FCVTWS)
      dut.io.a.poke(floatToIntBits((-2147483649L).toFloat).asUInt)
      dut.clock.step(1)
      dut.io.y.expect((-2147483648).U)
      dut.io.op.poke(CONVOP.FCVTWUS)
      dut.io.a.poke(floatToIntBits((-4294967296L).toFloat).asUInt)
      dut.clock.step(1)
      dut.io.y.expect(0.U)

      val r = new scala.util.Random()
      dut.io.op.poke(CONVOP.FCVTWS) // Float to signed int
      for(i <- 0 until rounds){
        var a = r.nextFloat() * 1000
        if(r.nextBoolean()){a = -a}
        dut.io.a.poke(floatToIntBits(a).U)
        dut.clock.step()
        dut.io.y.expect(a.toInt.U)
      }
      dut.io.op.poke(CONVOP.FCVTSW) // Signed int to float
      for(i <- 0 until rounds){
        val a = r.nextInt()
        dut.io.a.poke(a.U)
        dut.clock.step(2)
        dut.io.y.expect(floatToIntBits(a.toFloat).U)
      }
      dut.io.op.poke(CONVOP.FCVTWUS) // Float to unsigned int
      for(i <- 0 until rounds){
        var a = r.nextFloat() * 1000
        val neg = r.nextBoolean
        if(neg){a = -a}
        dut.io.a.poke(floatToIntBits(a).U)
        dut.clock.step(1)
        dut.io.y.expect((if(neg){-a}else{a}).toInt.U)
      }
      dut.io.op.poke(CONVOP.FCVTSWU) // Unsigned int to float
      for(i <- 0 until rounds){
        var a = ThreadLocalRandom.current().nextLong(0, 4294967296L)
        dut.io.a.poke(a.toInt.U)
        dut.clock.step(1)
        dut.io.y.expect(floatToIntBits(a.toFloat).U)
      }
    }
  }
}*/