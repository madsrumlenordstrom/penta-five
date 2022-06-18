package scalarcore

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


class ImmGenTester extends AnyFlatSpec with ChiselScalatestTester{
    "Immediate generator" should "pass" in {
        test(new ImmGen).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            
            val immI = "b000".U
            val immS = "b001".U
            val immB = "b010".U
            val immU = "b011".U
            val immJ = "b100".U

            // ---------------- Tests for immediate type I ----------------
            dut.io.immType.poke(immI)

            //              Bits: 31 30:25 24:21 20        19:0
            dut.io.inst.poke(    "b0_000000_0000_0_00000000000000000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b000000000000000000000_000000_0000_0".U)

            //              Bits: 31 30:25 24:21 20        19:0
            dut.io.inst.poke(    "b1_000000_0000_0_00000000000000000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b111111111111111111111_000000_0000_0".U)

            //              Bits: 31 30:25 24:21 20        19:0
            dut.io.inst.poke(    "b0_011101_1011_1_00000000000000000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b000000000000000000000_011101_1011_1".U)

            //              Bits: 31 30:25 24:21 20        19:0
            dut.io.inst.poke(    "b1_100010_0100_0_00000000000000000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b111111111111111111111_100010_0100_0".U)

            // ---------------- Tests for immediate type S ----------------
            dut.io.immType.poke(immS)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b0_000000_0000000000000_0000_0_0000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b000000000000000000000_000000_0000_0".U)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b1_000000_0000000000000_0000_0_0000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b111111111111111111111_000000_0000_0".U)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b0_010110_0000000000000_0110_1_0000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b000000000000000000000_010110_0110_1".U)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b1_111010_0000000000000_1001_0_0000000".U)
            //              Bits:          31:11          10:5  4:1  0
            dut.io.immRes.expect("b111111111111111111111_111010_1001_0".U)

            // ---------------- Tests for immediate type B ----------------
            dut.io.immType.poke(immB)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b0_000000_0000000000000_0000_0_0000000".U)
            //              Bits:          31:11       11  10:5  4:1  0
            dut.io.immRes.expect("b00000000000000000000_0_000000_0000_0".U)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b1_000000_0000000000000_0000_0_0000000".U)
            //              Bits:          31:11       11  10:5  4:1  0
            dut.io.immRes.expect("b11111111111111111111_0_000000_0000_0".U)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b0_111010_0000000000000_0100_1_0000000".U)
            //              Bits:          31:11       11  10:5  4:1  0
            dut.io.immRes.expect("b00000000000000000000_1_111010_0100_0".U)

            //              Bits: 31 30:25      24:12     11:8 7   6:0
            dut.io.inst.poke(    "b1_000101_0000000000000_1011_1_0000000".U)
            //              Bits:          31:11       11  10:5  4:1  0
            dut.io.immRes.expect("b11111111111111111111_1_000101_1011_0".U)

            // ---------------- Tests for immediate type U ----------------
            dut.io.immType.poke(immU)

            //              Bits: 31    30:20     19:12      11:0
            dut.io.inst.poke(    "b0_00000000000_00000000_000000000000".U)
            //              Bits: 31    30:20     19:12      11:0
            dut.io.immRes.expect("b0_00000000000_00000000_000000000000".U)

            //              Bits: 31    30:20     19:12      11:0
            dut.io.inst.poke(    "b1_00000000000_00000000_000000000000".U)
            //              Bits: 31    30:20     19:12      11:0
            dut.io.immRes.expect("b1_00000000000_00000000_000000000000".U)

            //              Bits: 31    30:20     19:12      11:0
            dut.io.inst.poke(    "b0_01101001001_01100001_000000000000".U)
            //              Bits: 31    30:20     19:12      11:0
            dut.io.immRes.expect("b0_01101001001_01100001_000000000000".U)

            //              Bits: 31    30:20     19:12      11:0
            dut.io.inst.poke(    "b1_10010110110_10011110_000000000000".U)
            //              Bits: 31    30:20     19:12      11:0
            dut.io.immRes.expect("b1_10010110110_10011110_000000000000".U)

            // ---------------- Tests for immediate type J ----------------
            dut.io.immType.poke(immJ)

            //              Bits: 31 30:25 24:21 20  19:12     11:0
            dut.io.inst.poke(    "b0_000000_0000_0_00000000_000000000000".U)
            //              Bits:     31:20      19:12  11  10:5  4:1  0
            dut.io.immRes.expect("b000000000000_00000000_0_000000_0000_0".U)

            //              Bits: 31 30:25 24:21 20  19:12     11:0
            dut.io.inst.poke(    "b1_000000_0000_0_00000000_000000000000".U)
            //              Bits:     31:20      19:12  11  10:5  4:1  0
            dut.io.immRes.expect("b111111111111_00000000_0_000000_0000_0".U)

            //              Bits: 31 30:25 24:21 20  19:12     11:0
            dut.io.inst.poke(    "b0_010110_0110_1_10100110_000000000000".U)
            //              Bits:     31:20      19:12  11  10:5  4:1  0
            dut.io.immRes.expect("b000000000000_10100110_1_010110_0110_0".U)

            //              Bits: 31 30:25 24:21 20  19:12     11:0
            dut.io.inst.poke(    "b1_101001_1001_0_01011001_000000000000".U)
            //              Bits:     31:20      19:12  11  10:5  4:1  0
            dut.io.immRes.expect("b111111111111_01011001_0_101001_1001_0".U)
        }
    }
}
