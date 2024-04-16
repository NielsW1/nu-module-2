package com.nedap.university.packet;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.nedap.university.packet.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.MODE;

public class FileStoragePacketReader {
  int PAYLOAD_SIZE;
  int HEADER_SIZE;

  public FileStoragePacketReader() {
    this.PAYLOAD_SIZE = FileStorageServiceHandler.PAYLOAD_SIZE;
    this.HEADER_SIZE = FileStorageServiceHandler.HEADER_SIZE;
  }

  public byte[] getPayload(DatagramPacket packet) {
    return Arrays.copyOfRange(packet.getData(), HEADER_SIZE, packet.getLength());
  }

  public int getPayloadSize(DatagramPacket packet) {
    int payloadSize = 0;
    payloadSize |= (packet.getData()[4] & 0xff) << 8;
    payloadSize |= (packet.getData()[5] & 0xff);

    return payloadSize;
  }

  public int getSequenceNumber(DatagramPacket packet) {
    int sequenceNumber = 0;
    sequenceNumber |= (packet.getData()[0] & 0xff) << 24;
    sequenceNumber |= (packet.getData()[1] & 0xff) << 16;
    sequenceNumber |= (packet.getData()[2] & 0xff) << 8;
    sequenceNumber |= packet.getData()[3] & 0xff;

    return sequenceNumber;
  }

  public boolean hasFlag(DatagramPacket packet, FileStorageHeaderFlags flag) {
    int flags = packet.getData()[6];
    return switch (flag) {
      case ERROR -> ((flags >>> 3 & 1) == 1);
      case FINAL -> ((flags >>> 2 & 1) == 1);
      case ACK -> ((flags >>> 1 & 1) == 1);
      case MODE -> ((flags & 1) == 1);
    };
  }
}
