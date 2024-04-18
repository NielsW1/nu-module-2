package com.nedap.university.packet;

import com.nedap.university.service.FileStorageServiceHandler;
import java.net.DatagramPacket;
import java.util.Arrays;

public class FileStoragePacketDecoder {
  int PAYLOAD_SIZE;
  int HEADER_SIZE;

  public FileStoragePacketDecoder() {
    PAYLOAD_SIZE = FileStorageServiceHandler.PAYLOAD_SIZE;
    HEADER_SIZE = FileStorageServiceHandler.HEADER_SIZE;
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

  public boolean hasFlag(DatagramPacket packet, FileStorageHeaderFlags flag) {
    int flags = packet.getData()[6];
    return switch (flag) {
      case ERROR -> ((flags >>> 4 & 1) == 1);
      case FINAL -> ((flags >>> 3 & 1) == 1);
      case ACK -> ((flags >>> 2 & 1) == 1);
      case RETRIEVE -> ((flags >>> 1 & 1) == 1);
      case SEND -> ((flags & 1) == 1);
    };
  }
}
