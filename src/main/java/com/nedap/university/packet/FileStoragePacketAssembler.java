package com.nedap.university.packet;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import javax.xml.crypto.Data;

import static com.nedap.university.packet.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.MODE;

public class FileStoragePacketAssembler {
  int PAYLOAD_SIZE;
  int HEADER_SIZE;

  public FileStoragePacketAssembler() {
    this.PAYLOAD_SIZE = FileStorageServiceHandler.PAYLOAD_SIZE;
    this.HEADER_SIZE = FileStorageServiceHandler.HEADER_SIZE;
  }

  public Queue<DatagramPacket> createPacketQueue(InetAddress address, int port, byte[] fileBytes) throws IOException {
    Queue<DatagramPacket> packetQueue = new LinkedList<>();
    int sequenceNumber = 1;

    for (int i = 0; i < fileBytes.length; i = i + PAYLOAD_SIZE) {
      byte[] packet;

      if (i + PAYLOAD_SIZE < fileBytes.length) {
        packet = new byte[PAYLOAD_SIZE];
        System.arraycopy(fileBytes, i, packet, 0, PAYLOAD_SIZE);
        packet = addPacketHeader(packet, sequenceNumber, 0, PAYLOAD_SIZE);
      } else {
        int finalPacketSize = fileBytes.length - i;
        packet = new byte[finalPacketSize];
        System.arraycopy(fileBytes, i, packet, 0, finalPacketSize);
        packet = addPacketHeader(packet, sequenceNumber, setFlags(Set.of(FINAL)), finalPacketSize);
      }
      packetQueue.add(new DatagramPacket(packet, packet.length, address, port));
      sequenceNumber++;
    }
    return packetQueue;
  }

  public DatagramPacket createAcknowledgementPacket(InetAddress address, int port, int sequenceNumber) {
    byte[] acknowledgementPacket = addPacketHeader(new byte[1], sequenceNumber, setFlags(ACK), 1);
    return new DatagramPacket(acknowledgementPacket, acknowledgementPacket.length, address, port);
  }

  public DatagramPacket createErrorPacket(InetAddress address, int port, int sequenceNumber, String error) {
    byte[] errorPacket = error.getBytes();
    errorPacket = addPacketHeader(errorPacket, sequenceNumber, setFlags(ERROR), errorPacket.length);
    return new DatagramPacket(errorPacket, errorPacket.length, address, port);
  }

  public DatagramPacket createBufferPacket() {
    int packetBufferSize = PAYLOAD_SIZE + HEADER_SIZE;
    return new DatagramPacket(new byte[packetBufferSize], packetBufferSize);
  }

  public byte[] addPacketHeader(byte[] packet, int sequenceNumber, int flags, int payloadSize) {
    byte[] packetWithHeader = new byte[packet.length + HEADER_SIZE];

    packetWithHeader[0] = (byte) (sequenceNumber >> 24 & 0xff);
    packetWithHeader[1] = (byte) (sequenceNumber >> 16 & 0xff);
    packetWithHeader[2] = (byte) (sequenceNumber >> 8 & 0xff);
    packetWithHeader[3] = (byte) (sequenceNumber & 0xff);
    packetWithHeader[4] = (byte) (payloadSize >> 8 & 0xff);
    packetWithHeader[5] = (byte) (payloadSize & 0xff);
    packetWithHeader[6] = (byte) flags;
    System.arraycopy(packet, 0, packetWithHeader, HEADER_SIZE, packet.length);
    return packetWithHeader;
  }

  public int setFlags(FileStorageHeaderFlags flag) {
    return setFlags(Set.of(flag));
  }

  public int setFlags(Set<FileStorageHeaderFlags> flags) {
    int flagByte = 0;

    for (FileStorageHeaderFlags flag : flags) {
      if (flag == ERROR) {
        flagByte |= 1 << 3;
      }
      if (flag == FINAL) {
        flagByte |= 1 << 2;
      }
      if (flag == ACK) {
        flagByte |= 1 << 1;
      }
      if (flag == MODE) {
        flagByte |= 1;
      }
    }
    return flagByte;
  }
}
