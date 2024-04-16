package com.nedap.university.service;

import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    sendFile(socket, piAddress, PI_PORT, filePath);
  }

  public void sendFile(DatagramSocket socket, InetAddress address, int port, String filePath) throws IOException {
    byte[] fileBytes = fileHandler.getFileBytes(filePath);
    Queue<DatagramPacket> packetQueue = packetAssembler.createPacketQueue(address, port, fileBytes);

    while (!packetQueue.isEmpty()) {
      DatagramPacket packetToSend = packetQueue.poll();
      int sequenceNumber = packetReader.getSequenceNumber(packetToSend);

      while (true) {
        sendPacket(socket, packetToSend);
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

  public void receiveFile(DatagramSocket socket, String fileName) throws IOException {
    receiveFile(socket, piAddress, PI_PORT, fileName);
  }

  public void receiveFile(DatagramSocket socket, InetAddress address, int port, String fileName) throws IOException {
    HashMap<Integer, byte[]> receivedPacketMap = new HashMap<>();
    boolean finalPacket = false;
    int lastSequenceNumber = 0;

    while (!finalPacket) {
      DatagramPacket receivedPacket = packetAssembler.createBufferPacket();
      socket.receive(receivedPacket);

      int sequenceNumber = packetReader.getSequenceNumber(receivedPacket);

      if (!receivedPacketMap.containsKey(sequenceNumber)) {
        byte[] payload = packetReader.getPayload(receivedPacket);
        lastSequenceNumber = sequenceNumber;

        if (packetReader.hasFlag(receivedPacket, FINAL)) {
          payload = Arrays.copyOfRange(payload, 0, packetReader.getPayloadSize(receivedPacket));
          finalPacket = true;
        }
        receivedPacketMap.put(sequenceNumber, payload);
        sendAcknowledgement(socket, packetAssembler.createAcknowledgementPacket(address, port, sequenceNumber));
      } else {
        sendAcknowledgement(socket, packetAssembler.createAcknowledgementPacket(address, port, lastSequenceNumber) );
      }
    }
    fileHandler.writeBytesToFile(fileHandler.getByteArrayFromMap(receivedPacketMap), fileName);
  }

  public DatagramPacket awaitResponsePacket(DatagramSocket socket) throws IOException {
    DatagramPacket bufferPacket = packetAssembler.createBufferPacket();
    socket.receive(bufferPacket);
    return bufferPacket;
  }

  public void sendPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
    socket.send(packet);
    System.out.println(packet.getAddress() + ": Sending packet " + packetReader.getSequenceNumber(packet));
  }

  public void sendAcknowledgement(DatagramSocket socket, DatagramPacket packet) throws IOException {
    socket.send(packet);
    System.out.println(packet.getAddress() + ": Sending ACK " + packetReader.getSequenceNumber(packet));
  }

  public boolean clientHandshake(DatagramSocket socket, String filePath,
      Set<FileStorageHeaderFlags> flagSet) throws IOException {
    int flags = packetAssembler.setFlags(flagSet);
    byte[] requestPacket = fileHandler.getFileNameBytes(filePath);
    requestPacket = packetAssembler.addPacketHeader(requestPacket, 0, flags, requestPacket.length);
    socket.send(new DatagramPacket(requestPacket, requestPacket.length, piAddress, PI_PORT));
    System.out.println("Request sent!");

    DatagramPacket receivedPacket = awaitResponsePacket(socket);
    if (packetReader.hasFlag(receivedPacket, ERROR)) {
      throw new IOException(new String(packetReader.getPayload(receivedPacket)));
    }
    if (!packetReader.hasFlag(receivedPacket, ACK)) {
      throw new IOException("No acknowledgement received from server");
    }
    System.out.println("Ack received!");
    return true;
  }

  public void serverHandshake(DatagramSocket socket) throws IOException {
    DatagramPacket requestPacket = awaitResponsePacket(socket);
    InetAddress address = requestPacket.getAddress();
    int port = requestPacket.getPort();
    String fileName = new String(packetReader.getPayload(requestPacket));

    if (packetReader.hasFlag(requestPacket, MODE)) {
      if (fileHandler.fileExistsCheck(fileName)) {
        sendAcknowledgement(socket, packetAssembler.createAcknowledgementPacket(address, port, 0));
        sendFile(socket, address, port, fileHandler.getFileStoragePath() + "/" + fileName);
      } else {
        socket.send(packetAssembler.createErrorPacket(address, port, 0, FileStorageFileHandler.NOT_EXISTS));
        System.out.println("Sending error: " + FileStorageFileHandler.NOT_EXISTS);
      }
    } else {
      if (!fileHandler.fileExistsCheck(fileName)) {
        sendAcknowledgement(socket, packetAssembler.createAcknowledgementPacket(address, port, 0));
        receiveFile(socket, address, port, fileName);
      } else {
        socket.send(packetAssembler.createErrorPacket(address, port, 0, FileStorageFileHandler.EXISTS));
        System.out.println("Sending error: " + FileStorageFileHandler.EXISTS);
      }
    }
  }
}
