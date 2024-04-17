package com.nedap.university.service;

import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
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
  private final FileStoragePacketDecoder packetDecoder;
  private final FileStorageFileHandler fileHandler;

  public FileStorageServiceHandler(String fileStoragePath) throws IOException {
    packetAssembler = new FileStoragePacketAssembler();
    packetDecoder = new FileStoragePacketDecoder();
    fileHandler = new FileStorageFileHandler(fileStoragePath);
    piAddress = InetAddress.getByName(PI_HOSTNAME);
  }

  public void sendFile(DatagramSocket socket, String filePath) throws IOException {
    sendFile(socket, piAddress, PI_PORT, filePath);
  }

  public void sendFile(DatagramSocket socket, InetAddress address, int port, String filePath) throws IOException {
    byte[] fileBytes = fileHandler.getFileBytes(filePath);
    Queue<DatagramPacket> packetQueue = packetAssembler.createPacketQueue(address, port, fileBytes);

    while (!packetQueue.isEmpty()) {
      DatagramPacket packetToSend = packetQueue.poll();
      int sequenceNumber = packetDecoder.getSequenceNumber(packetToSend);

      while (true) {
        socket.send(packetToSend);
        DatagramPacket response = awaitPacket(socket, packetToSend);

        if (packetDecoder.hasFlag(response, ERROR)) {
          throw new IOException(new String(packetDecoder.getPayload(response)));
        }
        if (packetDecoder.hasFlag(response, ACK) && packetDecoder.getSequenceNumber(response) == sequenceNumber) {
          break;
        }
      }
    }
  }

  public String receiveFile(DatagramSocket socket, String fileName) throws IOException {
    return receiveFile(socket, piAddress, PI_PORT, fileName);
  }

  public String receiveFile(DatagramSocket socket, InetAddress address, int port, String fileName) throws IOException {
    HashMap<Integer, byte[]> receivedPacketMap = new HashMap<>();
    boolean finalPacket = false;

    while (!finalPacket) {
      DatagramPacket receivedPacket = packetAssembler.createBufferPacket();
      socket.receive(receivedPacket);

      int sequenceNumber = packetDecoder.getSequenceNumber(receivedPacket);

      if (!receivedPacketMap.containsKey(sequenceNumber)) {
        byte[] payload = packetDecoder.getPayload(receivedPacket);

        if (packetDecoder.hasFlag(receivedPacket, FINAL)) {
          payload = Arrays.copyOfRange(payload, 0, packetDecoder.getPayloadSize(receivedPacket));
          finalPacket = true;
        }
        receivedPacketMap.put(sequenceNumber, payload);
        sendPacket(socket, packetAssembler.createAcknowledgementPacket(address, port, sequenceNumber));
      }
    }
    return fileHandler.writeBytesToFile(fileHandler.getByteArrayFromMap(receivedPacketMap), fileName);
  }

  public DatagramPacket awaitPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
    socket.setSoTimeout(10);
    while (true) {
      DatagramPacket receivedPacket = packetAssembler.createBufferPacket();
      try {
        socket.receive(receivedPacket);
        return receivedPacket;
      } catch (SocketTimeoutException e) {
        socket.send(packet);
        System.out.println("Retransmitting packet: " + packetDecoder.getSequenceNumber(packet));
      }
    }
  }

  public void sendPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
    socket.send(packet);
  }

  public boolean clientHandshake(DatagramSocket socket, String filePath,
      Set<FileStorageHeaderFlags> flagSet) throws IOException {
    int flags = packetAssembler.setFlags(flagSet);
    byte[] requestPacket = fileHandler.getFileNameBytes(filePath);
    requestPacket = packetAssembler.addPacketHeader(requestPacket, 0, flags, requestPacket.length);
    DatagramPacket packet = packetAssembler.createDataPacket(requestPacket, piAddress, PI_PORT);
    socket.send(packet);

    DatagramPacket receivedPacket = awaitPacket(socket, packet);
    if (packetDecoder.hasFlag(receivedPacket, ERROR)) {
      throw new IOException(new String(packetDecoder.getPayload(receivedPacket)));
    }
    if (!packetDecoder.hasFlag(receivedPacket, ACK)) {
      throw new IOException("No acknowledgement received from server");
    }
    return true;
  }

  public void serverHandshake(DatagramSocket socket) throws IOException {
    DatagramPacket requestPacket = packetAssembler.createBufferPacket();
    socket.receive(requestPacket);
    InetAddress address = requestPacket.getAddress();
    int port = requestPacket.getPort();
    String fileName = new String(packetDecoder.getPayload(requestPacket));

    if (packetDecoder.hasFlag(requestPacket, MODE)) {
      System.out.println("Request from: " + address.toString() + " to retrieve file: " + fileName);
      if (fileHandler.fileExists(fileName)) {
        sendPacket(socket, packetAssembler.createAcknowledgementPacket(address, port, 0));
        sendFile(socket, address, port, fileHandler.getFileStoragePath() + "/" + fileName);
        System.out.println("File sent successfully");
      } else {
        socket.send(
            packetAssembler.createErrorPacket(address, port, 0, FileStorageFileHandler.FILE_ERROR));
        System.out.println("Error: " + FileStorageFileHandler.FILE_ERROR);
      }
    } else {
      System.out.println("Request from: " + address.toString() + " to send file: " + fileName);
      sendPacket(socket, packetAssembler.createAcknowledgementPacket(address, port, 0));
      String outputFile = receiveFile(socket, address, port, fileName);
      System.out.println("File received and stored in: " + fileHandler.getFileStoragePath() + "/" + outputFile);
    }
  }
}
