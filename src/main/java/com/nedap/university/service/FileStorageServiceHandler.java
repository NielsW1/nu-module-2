package com.nedap.university.service;

import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static com.nedap.university.packet.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.MODE;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;

public class FileStorageServiceHandler {
  public static final int PAYLOAD_SIZE = 16384;
  public static final int HEADER_SIZE = 8;
  public static final int PACKET_SIZE = PAYLOAD_SIZE + HEADER_SIZE;
  public static final int WINDOW_SIZE = 10;

  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;
  private final FileStorageFileHandler fileHandler;
  private final FileStorageSender sender;
  private InetAddress address;
  private int port;

  public FileStorageServiceHandler(String fileStoragePath) throws IOException {
    assembler = new FileStoragePacketAssembler(this);
    decoder = new FileStoragePacketDecoder();
    fileHandler = new FileStorageFileHandler(fileStoragePath);
    sender = new FileStorageSender(assembler, decoder);
  }

  public void sendFile(DatagramSocket socket, Path filePath) throws IOException {
    sender.sendFile(socket, filePath);
  }

  public String receiveFile(DatagramSocket socket, String fileName) throws IOException {
    HashMap<Integer, byte[]> receivedPacketMap = new HashMap<>();
    boolean finalPacket = false;

    while (!finalPacket) {
      DatagramPacket receivedPacket = assembler.createBufferPacket(1);
      socket.receive(receivedPacket);

      int sequenceNumber = decoder.getSequenceNumber(receivedPacket);

      if (!receivedPacketMap.containsKey(sequenceNumber)) {
        byte[] payload = decoder.getPayload(receivedPacket);

        if (decoder.hasFlag(receivedPacket, FINAL)) {
          payload = Arrays.copyOfRange(payload, 0, decoder.getPayloadSize(receivedPacket));
          finalPacket = true;
        }
        receivedPacketMap.put(sequenceNumber, payload);
        socket.send(assembler.createAckPacket(sequenceNumber));
      }
    }
    return fileHandler.writeBytesToFile(fileHandler.getByteArrayFromMap(receivedPacketMap), fileName);
  }

  public boolean clientHandshake(DatagramSocket socket, String filePath,
      FileStorageHeaderFlags flag) throws IOException {
    byte[] requestPacket = fileHandler.getFileNameBytes(filePath);
    DatagramPacket packet = assembler.createPacket(requestPacket, 0, assembler.setFlags(flag));
    socket.send(packet);

    DatagramPacket receivedPacket = assembler.createBufferPacket(PACKET_SIZE);
    socket.receive(receivedPacket);
    if (decoder.hasFlag(receivedPacket, ERROR)) {
      throw new IOException(new String(decoder.getPayload(receivedPacket)));
    }
    if (!decoder.hasFlag(receivedPacket, ACK)) {
      throw new IOException("No acknowledgement received from server");
    }
    return true;
  }

  public void serverHandshake(DatagramSocket socket) throws IOException {
    DatagramPacket requestPacket = assembler.createBufferPacket(PACKET_SIZE);
    socket.receive(requestPacket);
    setAddressAndPort(requestPacket.getAddress(), requestPacket.getPort());
    String fileName = new String(decoder.getPayload(requestPacket));

    if (decoder.hasFlag(requestPacket, MODE)) {
      System.out.println("Request from: " + address.toString() + " to retrieve file: " + fileName);
      try {
        fileHandler.fileExists(fileName);
        socket.send(assembler.createAckPacket(0));
        sendFile(socket, Paths.get(fileHandler.getFileStoragePath(fileName)));
        System.out.println("File sent successfully");
      } catch (FileException e) {
        socket.send(
            assembler.createPacket(e.getMessage().getBytes(), 0, assembler.setFlags(ERROR)));
        System.out.println("Error: " + e.getMessage());
      }
    } else {
      System.out.println("Request from: " + address.toString() + " to send file: " + fileName);
      socket.send(assembler.createAckPacket(0));
      String outputFile = receiveFile(socket, fileName);
      System.out.println("File received and stored in: " + fileHandler.getFileStoragePath(outputFile));
    }
  }

  public void setAddressAndPort(InetAddress address, int port) {
    this.address = address;
    this.port = port;
  }

  public InetAddress getAddress() {
    return this.address;
  }

  public int getPort() {
    return this.port;
  }
}
