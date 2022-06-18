package scalarcore

import chisel3._
import utility.Constants._

class IntRegBank extends Module {
    val io = IO(new Bundle {
        val rs1, rs2, rd = Input(UInt(5.W))
        val write = Input(Bool())
        val xrd = Input(UInt(DATA_WIDTH.W))
        val xrs1, xrs2 = Output(UInt(DATA_WIDTH.W))
    })

    val x = Reg(Vec(32,UInt(DATA_WIDTH.W)))

    // Write to registers
    when(io.write && io.rd =/= 0.U){
        // Only write to reg x1 - x31
        x(io.rd) := io.xrd
        //printRegs()
    }
    x(0) := 0.U(DATA_WIDTH.W) // Hardwired zero - statement comes last, so will overwrite above statement when rd = 0

    val forwardRS1 : Bool = io.rd === io.rs1 && io.rs1 =/= 0.U && io.write
    val forwardRS2 : Bool = io.rd === io.rs2 && io.rs2 =/= 0.U && io.write

    // Read from registers
    io.xrs1 := Mux(forwardRS1, io.xrd, x(io.rs1))
    io.xrs2 := Mux(forwardRS2, io.xrd, x(io.rs2))
    
    def printRegs() : Unit = {
        // Prints registers in hex format
        for(i <- 0 until 8){
            for(j <- 0 until 4){
                printf("x(" + (j*8 + i) + ")")
                if(j*8 + i == 8 || j*8 + i == 9){
                    printf(" ")
                }
                printf(p" = 0x${Hexadecimal(x(j*8 + i))}\t")
            }
            printf("\n")
        }
        printf("\n\n")
    }
}
