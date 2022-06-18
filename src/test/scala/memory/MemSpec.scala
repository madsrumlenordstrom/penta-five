package memory


import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.ClientIO
import utility.Functions.connect

class CtrlWMem(memSize: Int) extends Module{
  val io = IO(Flipped(new ClientIO(memSize: Int)))

  val ctrl = Module(new MemoryController(1, memSize))
  val ram = Module(new SyncRAM(memSize))
  connect(ctrl.io.ram.elements, ram.io.elements)
  connect(ctrl.io.clients(0).elements, io.elements)

}

class MemSpec extends AnyFlatSpec with ChiselScalatestTester {
  val memSize = 512
  "Mem" should "pass" in {
    test(new CtrlWMem(memSize)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.valid.poke(true.B)
      dut.io.we.poke(true.B)
      dut.io.memWidth.poke(2.U)
      dut.io.addr.poke(1.U)
      dut.io.dout.poke(0x12345678.U)
      while(!dut.io.ready.peekBoolean()){dut.clock.step()}
      dut.io.we.poke(false.B)
      dut.io.memWidth.poke(0.U)
      dut.clock.step()
      while(!dut.io.ready.peekBoolean()){dut.clock.step()}
      println("Read : " + dut.io.din.peekInt().toInt.toHexString)
      assert((dut.io.din.peekInt().toInt & 0x000000FF) == 0x78)
      dut.io.memWidth.poke(1.U)
      dut.clock.step()
      while(!dut.io.ready.peekBoolean()){dut.clock.step()}
      println("Read : " + dut.io.din.peekInt().toInt.toHexString)
      assert((dut.io.din.peekInt().toInt & 0x0000FFFF) == 0x5678)
      dut.io.addr.poke(2.U)
      dut.clock.step()
      while(!dut.io.ready.peekBoolean()){dut.clock.step()}
      println("Read : " + dut.io.din.peekInt().toInt.toHexString)
      assert((dut.io.din.peekInt().toInt & 0x0000FFFF) == 0x3456)
      dut.io.memWidth.poke(2.U)
      dut.clock.step()
      while(!dut.io.ready.peekBoolean()){dut.clock.step()}
      println("Read : " + dut.io.din.peekInt().toInt.toHexString)
      assert((dut.io.din.peekInt().toInt & 0xFFFFFFFF) == 0x00123456)


      println("Read : " + dut.io.din.peekInt().toInt.toHexString)

    }
  }
}