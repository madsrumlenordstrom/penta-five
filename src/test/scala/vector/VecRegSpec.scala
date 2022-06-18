package vector
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._

import scala.collection.immutable.ListMap
import scala.math.abs
import scala.util.Random


class VecRegSpec extends AnyFlatSpec with ChiselScalatestTester {
  "Vector Manager" should "pass" in {
    test(new VecReg).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val mask = ListMap(0 -> 0x000000FF, 1 -> 0x0000FFFF, 2 -> 0xFFFFFFFF)
      def write(vec: Seq[Int], ew: Int, vl: Int, vd: Int): Unit ={
        dut.io.write.ew.poke(ew.U)
        dut.io.write.vl.poke(vl.U)    // Vector of 16 bytes (corresponding to lmul = 1 since sew = 8 and VLEN = 128)
        dut.io.write.vd.poke(vd.U)
        dut.io.write.vstart.poke(0.U) // Zero vstart
        dut.io.write.we.poke(true.B)
        while(!dut.io.write.ready.peek().litToBoolean){ // Wait for register to accept writing
          dut.clock.step()
        }
        for(i <- 0 until vl) {
          dut.io.write.din.poke(vec(i).U)
          dut.clock.step()
        }
        dut.io.write.we.poke(false.B)
      }
      def read(vec1: Seq[Int], vec2: Seq[Int], vec3: Seq[Int],  ew: Int, vl: Int, vs1: Int, vs2: Int, vs3: Int): Unit ={
        dut.io.read.ew.poke(ew.U)
        dut.io.read.vl.poke(vl.U)
        dut.io.read.vstart.poke(0.U)
        dut.io.read.vs(0).poke(vs1.U)
        dut.io.read.vs(1).poke(vs2.U)
        dut.io.read.vs(2).poke(vs3.U)
        dut.io.read.re.poke(true.B)
        while(!dut.io.read.ready.peek().litToBoolean){ // Wait for register to accept writing
          dut.clock.step()
        }
        for(i <- 0 until vl) {
          /*
          println("Read vec: " + i)
          var suc = (dut.io.read.vec(0).peekInt() & mask(ew)) == (vec1(i) & mask(ew))
          if(!suc){
            println("Expected: " + (vec1(i) & mask(ew)).toHexString)
            println("Got : " + (dut.io.read.vec(0).peekInt() & mask(ew)).toInt.toHexString)
          }

           */
          assert((dut.io.read.vec(0).peekInt() & mask(ew)) == (vec1(i) & mask(ew)))
          assert((dut.io.read.vec(1).peekInt() & mask(ew)) == (vec2(i) & mask(ew)))
          assert((dut.io.read.vec(2).peekInt() & mask(ew)) == (vec3(i) & mask(ew)))
          dut.clock.step()
        }
        dut.io.read.re.poke(false.B)
      }
      /*
      def read(vec: List[Int], ew: Int, vl: Int, vs: Int, channel: Int): Unit ={
        dut.io.read.ew.poke(ew.U)
        dut.io.read.vl.poke(vl.U)    // Vector of 16 bytes (corresponding to lmul = 1 since sew = 8 and VLEN = 128)
        dut.io.read.vstart.poke(0.U) // Zero vstart
        dut.io.read.vs(channel).poke(vs.U)
        dut.io.read.re.poke(true.B)
        while(!dut.io.read.ready.peek().litToBoolean){ // Wait for register to accept writing
          dut.clock.step()
        }
        for(i <- 0 until vl) {
          assert((dut.io.read.vec(channel).peekInt() & mask(ew)) == vec(i))
          dut.clock.step()
        }
        dut.io.read.re.poke(false.B)
      }

       */
      def WRVector(vec1: Seq[Int], vec2: Seq[Int], vec3: Seq[Int], ew: Int, vl: Int, vs1: Int, vs2: Int, vs3: Int): Unit ={
        // Write vec 1, 2 and 3
        write(vec1, ew, vl, vs1)
        write(vec2, ew, vl, vs2)
        write(vec3, ew, vl, vs3)
        read(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)

      }
      dut.io.en.poke(true.B)
      // Write and read with lmul = 1 and sew = e8
      var vl = 16
      var vs1 = 1
      var vs2 = 2
      var vs3 = 3
      var ew = 0
      var vec1 = Seq.fill(VLEN)(abs(Random.nextInt))
      var vec2 = Seq.fill(VLEN)(abs(Random.nextInt))
      var vec3 = Seq.fill(VLEN)(abs(Random.nextInt))
      WRVector(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)
      // Write and read with lmul = 1 and sew = e16
      vl = 8
      ew = 1
      WRVector(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)
      // Write and read with lmul = 1 and sew = e32
      vl = 4
      ew = 2
      WRVector(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)
      // Write and read with lmul = 2 and sew = e32
      vl = 8
      ew = 2
      vs1 = 2
      vs2 = 4
      vs3 = 6
      WRVector(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)
      // Write and read with lmul = 4 and sew = e32
      vl = 16
      ew = 2
      vs1 = 4
      vs2 = 8
      vs3 = 12
      WRVector(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)
      // Write and read with lmul = 8 and sew = e8
      vl = 128
      ew = 0
      vs1 = 8
      vs2 = 16
      vs3 = 24
      WRVector(vec1, vec2, vec3, ew, vl, vs1, vs2, vs3)
    }
  }
}