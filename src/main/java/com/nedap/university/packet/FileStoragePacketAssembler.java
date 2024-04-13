package com.nedap.university.packet;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;

public class FileStoragePacketAssembler {
  public static final int PAYLOAD_SIZE = 512;
  public static final int HEADER_SIZE = 8;

  public FileStoragePacketAssembler() {}

  public Queue<DatagramPacket> createPacketQueue(InetAddress address, int port, String filePath) throws IOException {
    Queue<DatagramPacket> packetQueue = new LinkedList<>();
    byte[] fileBytes = getFileBytes(filePath);
    int sequenceNumber = 0;

    for (int i = 0; i < fileBytes.length; i = i + PAYLOAD_SIZE) {
      byte[] packet;
      if (i + PAYLOAD_SIZE < fileBytes.length) {
        packet = new byte[PAYLOAD_SIZE];
        System.arraycopy(fileBytes, i, packet, 0, PAYLOAD_SIZE);
      } else {
        packet = new byte[fileBytes.length - i];
        System.arraycopy(fileBytes, i, packet, 0, fileBytes.length - i);
      }
      packet = addPacketHeader(packet, sequenceNumber++);
      packetQueue.add(assembleDataPacket(address, port, packet));
    }
    return packetQueue;
  }

  public DatagramPacket assembleDataPacket(InetAddress address, int port, byte[] packet) {
    return new DatagramPacket(packet, packet.length, address, port);
  }

  public DatagramPacket assembleRequestPacket(InetAddress address, int port) {
    return new DatagramPacket(new byte[1], 1, address, port);
  }

  public byte[] addPacketHeader(byte[] packet, int sequenceNumber) {
    byte[] packetWithHeader = new byte[packet.length + HEADER_SIZE];

    packetWithHeader[0] = (byte) (sequenceNumber >>> 8 & 0xff);
    packetWithHeader[1] = (byte) (sequenceNumber & 0xff);
    System.arraycopy(packet, 0, packetWithHeader, HEADER_SIZE, packet.length);
    return packetWithHeader;
  }

  public byte[] getFileBytes(String filePath) throws IOException {
    File file = new File(filePath);
    byte[] fileBytes = new byte[(int) file.length()];

    FileInputStream inputStream = new FileInputStream(file);
    inputStream.read(fileBytes);

    return fileBytes;
  }
}
