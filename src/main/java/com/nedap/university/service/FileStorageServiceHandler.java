package com.nedap.university.service;

import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;

import static com.nedap.university.packet.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.MODE;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;

public class FileStorageServiceHandler {
  public static final int PI_PORT = 8080;
  public static final String PI_HOSTNAME = "172.16.1.1";
  public static final int PAYLOAD_SIZE = 512;
  public static final int HEADER_SIZE = 8;

  private final InetAddress piAddress;
  private final FileStoragePacketAssembler packetAssembler;
  private final FileStoragePacketReader packetReader;
  private final FileStorageFileHandler fileHandler;

  public FileStorageServiceHandler(String fileStoragePath) throws IOException {
    packetAssembler = new FileStoragePacketAssembler();
    packetReader = new FileStoragePacketReader();
    fileHandler = new FileStorageFileHandler(fileStoragePath);
    piAddress = InetAddress.getByName(PI_HOSTNAME);
  }

  public void sendFile(DatagramSocket socket, String filePath) throws IOException {
    sendFile(socket, filePath, piAddress, PI_PORT);
  }

  public void sendFile(DatagramSocket socket, String filePath, InetAddress address, int port) throws IOException {
    byte[] fileBytes = fileHandler.getFileBytes(filePath);
    Queue<DatagramPacket> packetQueue = packetAssembler.createPacketQueue(address, port, fileBytes);

    while (!packetQueue.isEmpty()) {
      DatagramPacket packetToSend = packetQueue.poll();
      int sequenceNumber = packetReader.getSequenceNumber(packetToSend);

      while (true) {
        socket.send(packetToSend);
        DatagramPacket response = awaitResponsePacket(socket);

        if (packetReader.hasFlag(response, ERROR)) {
          throw new IOException(new String(packetReader.getPayload(response)));
        }
        if (packetReader.hasFlag(response, ACK) && packetReader.getSequenceNumber(response) == sequenceNumber) {
          break;
        }
      }
    }
  }
  public void receiveFile(DatagramSocket socket) throws IOException {
    receiveFile(socket, piAddress, PI_PORT);
  }

  public void receiveFile(DatagramSocket socket, InetAddress address, int port) throws IOException {
    HashMap<Integer, byte[]> receivedPacketMap = new HashMap<>();
    boolean finalPacket = false;
    int lastSequenceNumber = 0;

    while (!finalPacket) {
      DatagramPacket receivedPacket = packetAssembler.createBufferPacket();
      socket.receive(receivedPacket);

      int sequenceNumber = packetReader.getSequenceNumber(receivedPacket);

      if (sequenceNumber > lastSequenceNumber && !receivedPacketMap.containsKey(sequenceNumber)) {
        byte[] payload = packetReader.getPayload(receivedPacket);
        lastSequenceNumber = sequenceNumber;

        if (packetReader.hasFlag(receivedPacket, FINAL)) {
          payload = Arrays.copyOfRange(payload, 0, packetReader.getPayloadSize(receivedPacket));
          finalPacket = true;
        }
        receivedPacketMap.put(sequenceNumber, payload);
        socket.send(packetAssembler.createAcknowledgementPacket(address, port, sequenceNumber));
      } else {
        socket.send(packetAssembler.createAcknowledgementPacket(address, port, lastSequenceNumber));
      }
    }
  }

  public DatagramPacket awaitResponsePacket(DatagramSocket socket) throws IOException {
    DatagramPacket bufferPacket = packetAssembler.createBufferPacket();
    socket.receive(bufferPacket);
    return bufferPacket;
  }

  public boolean clientHandshake(DatagramSocket socket, String filePath,
      Set<FileStorageHeaderFlags> flagSet) throws IOException {
    int flags = packetAssembler.setFlags(flagSet);
    byte[] requestPacket = fileHandler.getFileNameBytes(filePath);
    requestPacket = packetAssembler.addPacketHeader(requestPacket, 0, flags, requestPacket.length);
    socket.send(new DatagramPacket(requestPacket, requestPacket.length, piAddress, PI_PORT));

    DatagramPacket receivedPacket = awaitResponsePacket(socket);
    if (packetReader.hasFlag(receivedPacket, ERROR)) {
      throw new IOException(new String(packetReader.getPayload(receivedPacket)));
    }
    if (!packetReader.hasFlag(receivedPacket, ACK)) {
      throw new IOException("No acknowledgement received from server");
    }
    return true;
  }

}
