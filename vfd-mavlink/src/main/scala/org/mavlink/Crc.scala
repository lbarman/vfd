package org.mavlink

/**
 * X.25 CRC calculation for MAVlink messages. The checksum must be initialized,
 * updated with witch field of the message, and then finished with the message
 * id.
 */
case class Crc(val crc: Int = 0xffff) extends AnyVal {

  def next(datum: Byte): Crc = {
    val d = datum & 0xff
    var tmp = d ^ (crc & 0xff)
    tmp ^= (tmp << 4) & 0xff;
    Crc(
      ((crc >> 8) & 0xff) ^ (tmp << 8) ^ (tmp << 3) ^ ((tmp >> 4) & 0xff))
  }

  def lsb: Byte = crc.toByte
  def msb: Byte = (crc >> 8).toByte

}