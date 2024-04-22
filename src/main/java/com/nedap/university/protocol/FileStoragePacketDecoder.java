package com.nedap.university.protocol;

import static com.nedap.university.service.FileStorageServiceHandler.HEADER_SIZE;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.zip.CRC32;

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
    checksum.update(getPacketWithoutChecksum(packet));

    return getChecksum(packet) == checksum.getValue();
  }

  public long getChecksum(DatagramPacket packet) {
    long checksum = 0;
    checksum |= (long) (packet.getData()[8] & 0xff) << 24;
    checksum |= (long) (packet.getData()[9] & 0xff) << 16;
    checksum |= (long) (packet.getData()[10] & 0xff) << 8;
    checksum |= (long) packet.getData()[11] & 0xff;

    return checksum;
  }

  public byte[] getPacketWithoutChecksum(DatagramPacket packet) {
    byte[] packetWithoutChecksum = new byte[HEADER_SIZE + getPayloadSize(packet)];
    byte[] payload = getPayload(packet);
    System.arraycopy(packet.getData(), 0, packetWithoutChecksum, 0, HEADER_SIZE - 5);
    System.arraycopy(payload, 0, packetWithoutChecksum, HEADER_SIZE, payload.length);

    return packetWithoutChecksum;
  }

  public boolean hasFlag(DatagramPacket packet, FileStorageHeaderFlags flag) {
    int flags = packet.getData()[6];
    return switch (flag) {
      case REMOVE -> (flags >>> 7 & 1) == 1;
      case LIST -> (flags >>> 6 & 1) == 1;
      case ERROR -> (flags >>> 5 & 1) == 1;
      case FINAL -> (flags >>> 4 & 1) == 1;
      case NACK -> (flags >>> 3 & 1) == 1;
      case ACK -> (flags >>> 2 & 1) == 1;
      case RETRIEVE -> (flags >>> 1 & 1) == 1;
      case SEND -> (flags & 1) == 1;
    };
  }
}
