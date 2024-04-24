package com.nedap.university.protocol;

import static com.nedap.university.protocol.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.service.FileStorageServiceHandler.HEADER_SIZE;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.CRC32;
import javax.xml.crypto.Data;

public class FileStoragePacketDecoder {

  public FileStoragePacketDecoder() {
  }

  public byte[] getPayload(DatagramPacket packet) {
    return Arrays.copyOfRange(packet.getData(), HEADER_SIZE, getPayloadSize(packet) + HEADER_SIZE);
  }

  public int getPayloadSize(DatagramPacket packet) {
    int payloadSize = 0;
    payloadSize |= (packet.getData()[4] & 0xff) << 8;
    payloadSize |= packet.getData()[5] & 0xff;

    return payloadSize;
  }

  public String getFileName(DatagramPacket packet) {
    byte[] payload = getPayload(packet);
    return new String(Arrays.copyOfRange(payload, 8, payload.length));
  }

  public long getFileSize(DatagramPacket packet) {
    byte[] payload = getPayload(packet);
    long fileSize = 0;

    for (int i = 1; i < 9; i++) {
      fileSize |= (long) (payload[i - 1] & 0xff) << ((8 - i) * 8);
    }

    return fileSize;
  }

  public int getSequenceNumber(DatagramPacket packet) {
    int sequenceNumber = 0;
    sequenceNumber |= (packet.getData()[0] & 0xff) << 24;
    sequenceNumber |= (packet.getData()[1] & 0xff) << 16;
    sequenceNumber |= (packet.getData()[2] & 0xff) << 8;
    sequenceNumber |= packet.getData()[3] & 0xff;

    return sequenceNumber;
  }

  public boolean verifyChecksum(DatagramPacket packet) {
    CRC32 checksum = new CRC32();
    long receivedChecksum = getChecksum(packet);
    checksum.update(getPacketWithoutChecksum(packet.getData()));

    return receivedChecksum == checksum.getValue();
  }

  public long getChecksum(DatagramPacket packet) {
    long checksum = 0;
    checksum |= (long) (packet.getData()[8] & 0xff) << 24;
    checksum |= (long) (packet.getData()[9] & 0xff) << 16;
    checksum |= (long) (packet.getData()[10] & 0xff) << 8;
    checksum |= (long) packet.getData()[11] & 0xff;

    return checksum;
  }

  public byte[] getPacketWithoutChecksum(byte[] packet) {
    for (int i = HEADER_SIZE - 4; i < HEADER_SIZE; i++) {
      packet[i] = 0;
    }
    return packet;
  }

  public boolean hasFlags(DatagramPacket packet, Set<FileStorageHeaderFlags> flags) {
    for (FileStorageHeaderFlags flag : flags) {
      if (hasFlag(packet, flag)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasFlag(DatagramPacket packet, FileStorageHeaderFlags flag) {
    int flags = (packet.getData()[6] & 0xff) << 8;
    flags |= packet.getData()[7] & 0xff;
    return switch (flag) {
      case ERROR -> (flags >>> 8 & 1) == 1;
      case FINAL -> (flags >>> 7 & 1) == 1;
      case NACK -> (flags >>> 6 & 1) == 1;
      case ACK -> (flags >>> 5 & 1) == 1;
      case LIST -> (flags >>> 4 & 1) == 1;
      case DELETE -> (flags >>> 3 & 1) == 1;
      case RETRIEVE -> (flags >>> 2 & 1) == 1;
      case REPLACE -> (flags >>> 1 & 1) == 1;
      case SEND -> (flags & 1) == 1;
    };
  }

  public FileStorageHeaderFlags getFlag(DatagramPacket packet) {
    for (FileStorageHeaderFlags flag : EnumSet.allOf(FileStorageHeaderFlags.class)) {
      if (hasFlag(packet, flag)) {
        return flag;
      }
    }
    return ERROR;
  }
}
