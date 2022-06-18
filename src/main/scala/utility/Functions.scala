package utility

import chisel3.Data
import chisel3.experimental.DataMirror
import chisel3._

import scala.collection.immutable

object Functions {
  def connect(ioA: immutable.SeqMap[String, Data], ioB: immutable.SeqMap[String, Data]):Unit={
    for ((id, pin) <- ioA) {
      DataMirror.specifiedDirectionOf(pin).toString match {
        case "Input" => ioB(id) := pin
        case "Output" => pin := ioB(id)
        case "Unspecified" => connect(ioA(id).asInstanceOf[Bundle].elements, ioB(id).asInstanceOf[Bundle].elements)
      }
    }
  }
}