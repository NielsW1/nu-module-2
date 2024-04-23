package com.nedap.university.service;

import com.nedap.university.protocol.FileStorageHeaderFlags;
import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.protocol.FileStoragePacketDecoder;
import com.nedap.university.service.exceptions.FileException;
import com.nedap.university.service.exceptions.RequestException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.nedap.university.protocol.FileStorageHeaderFlags.DELETE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.protocol.FileStorageHeaderFlags.ACK;
import static com.nedap.university.protocol.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.protocol.FileStorageHeaderFlags.LIST;
import static com.nedap.university.protocol.FileStorageHeaderFlags.REPLACE;
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

  public void sendFile(Path filePath, long fileSize, boolean print) throws IOException {
    System.out.println("Sending " + fileSize + " bytes...");
    sender.sendFile(socket, filePath, fileSize, print);
    System.out.println("File sent successfully!");
  }

  public void replaceFile(String fileName, long fileSize, boolean print) throws IOException {
    System.out.println("Receiving " + fileSize + " bytes...");
    Path filePath = fileHandler.getFileStoragePath(fileName);
    receiver.receiveFile(socket, filePath, fileSize, print);
    System.out.println("File received and replaced: " + filePath.toString());
  }

  public void receiveFile(String fileName, long fileSize, boolean print) throws IOException {
    System.out.println("Receiving " + fileSize + " bytes...");
    Path filePath = fileHandler.updateFileName(fileName);
    receiver.receiveFile(socket, filePath, fileSize, print);
    System.out.println("File received and stored at: " + filePath.toString());
  }

  public void deleteFile(String fileName) throws IOException {
    Path deletedFile = fileHandler.deleteFile(fileName);
    System.out.println("File deleted: " + deletedFile.toString());
  }

  public void sendFileList() throws IOException {
    byte[] listOfFiles = fileHandler.getFilesInDirectory();
    int listOfFilesLength = listOfFiles.length;
    int i = 0;

    while (listOfFilesLength > 0) {
      byte[] payload = new byte[Math.min(listOfFilesLength, PAYLOAD_SIZE)];
      System.arraycopy(listOfFiles, i++ * PAYLOAD_SIZE, payload, 0, payload.length);
      listOfFilesLength -= payload.length;
      if (listOfFilesLength > 0) {
        socket.send(assembler.createPacket(payload, 0, assembler.setFlags(Set.of(LIST, ACK))));
      } else {
        socket.send(
            assembler.createPacket(payload, 0, assembler.setFlags(Set.of(LIST, ACK, FINAL))));
      }
    }
  }

  public void receiveFileList(List<DatagramPacket> packetList) {
    StringBuilder fileList = new StringBuilder();
    for (DatagramPacket packet: packetList) {
      fileList.append(new String(decoder.getPayload(packet)));
    }
    for (String fileName: fileList.toString().split(",")) {
      System.out.println(fileName);
    }
  }

  public long clientRequest(String filePath, FileStorageHeaderFlags flag)
      throws IOException, FileException {
    long fileSize = 0;

    switch (flag) {
      case SEND:
      case REPLACE:
        socket.send(assembler.createRequestPacket(Files.size(Paths.get(filePath)),
            fileHandler.getFileNameBytes(filePath),
            assembler.setFlags(flag)));
        break;

      case RETRIEVE:
      case DELETE:
        socket.send(assembler.createRequestPacket(0, fileHandler.getFileNameBytes(filePath),
            assembler.setFlags(flag)));
        break;

      case LIST:
        socket.send(assembler.createPacket(new byte[1], 0, assembler.setFlags(flag)));
        break;
    }

    while (true) {
      socket.setSoTimeout(30000);
      DatagramPacket receivedPacket = assembler.createBufferPacket(PACKET_SIZE);
      socket.receive(receivedPacket);

      if (decoder.hasFlag(receivedPacket, ERROR)) {
        throw new FileException(new String(decoder.getPayload(receivedPacket)));
      }
      if (!decoder.hasFlag(receivedPacket, ACK)) {
        continue;
      }
      if (decoder.hasFlags(receivedPacket, Set.of(SEND, REPLACE, RETRIEVE))) {
        fileSize = decoder.getFileSize(receivedPacket);
        break;
      } else if (decoder.hasFlag(receivedPacket, DELETE)) {
        System.out.println("File successfully deleted: " + decoder.getFileName(receivedPacket));
        break;
      } else if (decoder.hasFlag(receivedPacket, LIST)) {
        List<DatagramPacket> packetList = new ArrayList<>();
        packetList.add(receivedPacket);
        if (decoder.hasFlag(receivedPacket, FINAL)) {
          receiveFileList(packetList);
          break;
        }
      }
    }
    socket.setSoTimeout(0);
    return fileSize;
  }

  public void serverHandleRequest() throws IOException, FileException, RequestException {
    DatagramPacket requestPacket = assembler.createBufferPacket(PACKET_SIZE);
    socket.receive(requestPacket);
    setAddressAndPort(requestPacket.getAddress(), requestPacket.getPort());
    FileStorageHeaderFlags flag = decoder.getFlag(requestPacket);

    String fileName = "";
    long fileSize = 0;
    if (decoder.getPayloadSize(requestPacket) > 9) {
      fileName = decoder.getFileName(requestPacket);
      fileSize = decoder.getFileSize(requestPacket);
    }

    System.out.println("Request from: " + address.toString() + " " + flag + " " + fileName);

    switch (flag) {
      case SEND:
        fileTooLarge(fileSize);
        socket.send(assembler.createRequestPacket(fileSize, fileName.getBytes(),
                assembler.setFlags(Set.of(SEND, ACK))));
        receiveFile(fileName, fileSize, false);
        break;

      case REPLACE:
        fileExists(fileName);
        fileTooLarge(fileSize);
        socket.send(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(REPLACE, ACK))));
        replaceFile(fileName, fileSize, false);
        break;

      case RETRIEVE:
        fileExists(fileName);
        fileSize = fileHandler.getFileSize(fileName);
        socket.send(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(RETRIEVE, ACK))));
        sendFile(fileHandler.getFileStoragePath(fileName), fileSize, false);
        break;

      case DELETE:
        fileExists(fileName);
        deleteFile(fileName);
        socket.send(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(DELETE, ACK))));
        break;

      case LIST:
        sendFileList();
        break;

      default:
        throw new RequestException();
    }
  }

  public void sendErrorPacket(String error) throws IOException {
    socket.send(assembler.createPacket(error.getBytes(), 0, assembler.setFlags(ERROR)));
  }

  public void fileExists(String fileName) throws FileException {
    if (!fileHandler.fileExists(fileName)) {
      throw new FileException();
    }
  }

  public void fileTooLarge(long fileSize) throws FileException {
    if (fileSize > (Math.pow(2, 31) - 1) * PAYLOAD_SIZE) {
      throw new FileException("File is too large to send!");
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
