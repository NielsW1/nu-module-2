package com.nedap.university.service;

import com.nedap.university.protocol.FileStorageHeaderFlags;
import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.protocol.FileStoragePacketDecoder;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static com.nedap.university.protocol.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.protocol.FileStorageHeaderFlags.ACK;
import static com.nedap.university.protocol.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;

public class FileStorageServiceHandler {

  public static final int PAYLOAD_SIZE = 4096;
  public static final int HEADER_SIZE = 12;
  public static final int PACKET_SIZE = PAYLOAD_SIZE + HEADER_SIZE;
  public static final int WINDOW_SIZE = 50;

  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;
  private final FileStorageFileHandler fileHandler;
  private final FileStorageSender sender;
  private final FileStorageReceiver receiver;
  private final DatagramSocket socket;
  private InetAddress address;
  private int port;

  public FileStorageServiceHandler(DatagramSocket socket, String fileStoragePath) throws IOException {
    assembler = new FileStoragePacketAssembler(this);
    decoder = new FileStoragePacketDecoder();
    fileHandler = new FileStorageFileHandler(fileStoragePath);
    sender = new FileStorageSender(assembler, decoder);
    receiver = new FileStorageReceiver(assembler, decoder);
    this.socket = socket;
  }

  public void sendFile(Path filePath, long fileSize) throws IOException {
    sender.sendFile(socket, filePath, fileSize);
  }

  public String receiveFile(String fileName, long fileSize) throws IOException {
    Path filePath = fileHandler.updateFileName(fileName);
    receiver.receiveFile(socket, filePath, fileSize);
    return filePath.toString();
  }

  public long clientHandshake(String filePath,
      FileStorageHeaderFlags flag) throws IOException {
    byte[] fileNameBytes = fileHandler.getFileNameBytes(filePath);
    DatagramPacket packet;
    long fileSize;

    if (flag == SEND) {
      packet = assembler.createRequestPacket(Files.size(Paths.get(filePath)), fileNameBytes, assembler.setFlags(flag));
    } else {
      packet = assembler.createRequestPacket(0, fileNameBytes, assembler.setFlags(flag));
    }
    socket.send(packet);

    while (true) {
      DatagramPacket receivedPacket = assembler.createBufferPacket(PACKET_SIZE);
      socket.receive(receivedPacket);

      if (decoder.hasFlag(receivedPacket, ERROR)) {
        throw new IOException(new String(decoder.getPayload(receivedPacket)));
      }
      if (decoder.hasFlag(receivedPacket, ACK) && (decoder.hasFlag(receivedPacket, SEND)
          || decoder.hasFlag(receivedPacket, RETRIEVE))) {
        fileSize = decoder.getFileSize(receivedPacket);
        break;
      }
    }
    return fileSize;
  }

  public void serverHandshake() throws IOException, FileException {
    DatagramPacket requestPacket = assembler.createBufferPacket(PACKET_SIZE);
    socket.receive(requestPacket);
    setAddressAndPort(requestPacket.getAddress(), requestPacket.getPort());

    if (decoder.hasFlag(requestPacket, RETRIEVE)) {
      serverHandleRetrieve(requestPacket);
    } else if (decoder.hasFlag(requestPacket, SEND)) {
      serverHandleSend(requestPacket);
    } else {
      String error = "Invalid request, No send/retrieve flag";
      socket.send(assembler.createPacket(error.getBytes(), 0, assembler.setFlags(ERROR)));
      throw new IOException(error);
    }
  }

  public void serverHandleSend(DatagramPacket packet)
      throws IOException, FileException {
    String fileName = decoder.getFileName(packet);

    System.out.println("Request from: " + address.toString() + " to send file: " + fileName);
    long fileSize = decoder.getFileSize(packet);
    if (fileSize > (Math.pow(2, 31) - 1) * PAYLOAD_SIZE) {
      String error = "File is too large to send!";
      socket.send(assembler.createPacket(error.getBytes(), 0, assembler.setFlags(ERROR)));
      throw new FileException(error);

    } else {
      socket.send(assembler.createPacket(assembler.getFileSizeByteArray(fileSize), 0,
          assembler.setFlags(Set.of(ACK, SEND))));
      String outputPath = receiveFile(fileName, fileSize);
      System.out.println(
          "File received and stored at: " + outputPath);
    }
  }

  public void serverHandleRetrieve(DatagramPacket packet)
      throws IOException, FileException {
    String fileName = decoder.getFileName(packet);
    System.out.println("Request from: " + address.toString() + " to retrieve file: " + fileName);
    if (!fileHandler.fileExists(fileName)) {
      String error = "File does not exist or is not in this directory";
      socket.send(
          assembler.createPacket(error.getBytes(), 0, assembler.setFlags(ERROR)));
      throw new FileException(error);

    } else {
      long fileSize = fileHandler.getFileSize(fileName);
      socket.send(assembler.createRequestPacket(fileSize, fileName.getBytes(),
          assembler.setFlags(Set.of(ACK, RETRIEVE))));
      sendFile(fileHandler.getFileStoragePath(fileName), fileSize);
      System.out.println("File sent successfully");
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
