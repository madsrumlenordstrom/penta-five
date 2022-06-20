package vector
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._
import utility.Functions.connect
import memory._
import java.io._
import scala.collection.immutable.ListMap
import scala.math.abs
import scala.util.Random
class VecLSUnitWMem(memSize: Int, init: Seq[Int]) extends Module{
  val io = IO(new VecLSUnitCoreIO(memSize))

  val filename = "data.txt"
  val file = new File(filename)
  val writer = new PrintWriter(file)
  for(i <- init.indices){
    writer.write(init(i).toHexString + "\n")
  }
  writer.close()
  val lsUnit = Module(new VecLSUnit(memSize))
  val memCtrl = Module(new MemoryController(1, memSize))
  val ram = Module(new SyncRAM(memSize, filename))
  connect(memCtrl.io.clients(0).elements, lsUnit.io.ram.elements)
  connect(memCtrl.io.ram.elements, ram.io.elements)
  connect(io.elements, lsUnit.io.core.elements)
  lsUnit.io.en := true.B
}


class VecLSUnitSpec extends AnyFlatSpec with ChiselScalatestTester {
  val memSize = 512
  val mem = Seq.fill(memSize/4)(abs(Random.nextInt))
  "Vector LS Unit" should "pass" in {
    test(new VecLSUnitWMem(memSize, mem)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val bitmask = ListMap(0 -> 0x000000FF, 1 -> 0x0000FFFF, 2 -> 0xFFFFFFFF)
      def readMem(addr: Int): Int={
        val wordAddr = addr >>> 2
        val byteOffset = addr & 0x00000003
        var res = mem(wordAddr)
        if(byteOffset == 1){
          res =((mem(wordAddr) & 0xFFFFFF00) >> 8) | ((mem(wordAddr + 1) & 0x000000FF) << 24)
        }
        if(byteOffset == 2){
          res =((mem(wordAddr) & 0xFFFF0000) >> 16) | ((mem(wordAddr + 1) & 0x00FFFF) << 16)
        }
        if(byteOffset == 3){
          res =((mem(wordAddr) & 0xFF000000) >> 24) | ((mem(wordAddr + 1) & 0x00FFFFFF) << 8)
        }
        res
      }
      def load(addr: Int, vl: Int, ew: Int, vm: Boolean, mask: Boolean): Unit ={
        var currAddr = addr
        dut.io.addr.poke(addr.U)
        dut.io.vl.poke(vl.U)
        dut.io.ew.poke(ew.U)
        dut.io.vm.poke(vm.B)
        dut.io.mask.poke(mask.B)
        dut.io.valid.poke(true.B)
        for(i <- 0 until vl){
          if(ew == 0){
            while(!dut.io.ready.peekBoolean){dut.clock.step()} // Wait for data to arrive
            //println("i : " + i)
            //println("Out: " + (dut.io.din.peekInt() & bitmask(ew)).toInt.toHexString )
            //println("Exp: " + (readMem(currAddr) & bitmask(ew)).toHexString)
            assert((dut.io.din.peekInt() & bitmask(ew)) == (readMem(currAddr) & bitmask(ew)))
            currAddr+=1
            dut.io.addr.poke(currAddr.U)
            dut.clock.step() // Generate next adddress
          } else if(ew == 1){
            while(!dut.io.ready.peekBoolean){dut.clock.step()} // Wait for data to arrive
            assert((dut.io.din.peekInt() & bitmask(ew)) == (readMem(currAddr) & bitmask(ew)))
            currAddr+=2
            dut.io.addr.poke(currAddr.U)
            dut.clock.step() // Generate next adddress
          } else if(ew == 2){
            while(!dut.io.ready.peekBoolean){dut.clock.step()} // Wait for data to arrive
            assert((dut.io.din.peekInt() & bitmask(ew)) == (readMem(currAddr) & bitmask(ew)))
            currAddr+=4
            dut.io.addr.poke(currAddr.U)
            dut.clock.step() // Generate next adddress
          }
        }
        dut.io.valid.poke(false.B)
      }
      var addr = 4
      var vl = 8
      var ew = 0
      var vm = false
      var mask = false
      load(addr, vl, ew, vm, mask)
    }
  }
}