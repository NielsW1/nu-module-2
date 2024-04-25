package com.nedap.university.service;

import static com.nedap.university.protocol.FileStorageHeaderFlags.ACK;
import static com.nedap.university.protocol.FileStorageHeaderFlags.DELETE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.protocol.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.protocol.FileStorageHeaderFlags.LIST;
import static com.nedap.university.protocol.FileStorageHeaderFlags.REPLACE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;

import com.nedap.university.protocol.FileStorageHeaderFlags;
import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.protocol.FileStoragePacketDecoder;
import com.nedap.university.server.FileStorageServerLogger;
import com.nedap.university.service.exceptions.FileException;
import com.nedap.university.service.exceptions.RequestException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
  private FileStorageServerLogger serverLogger;
  private final DatagramSocket socket;
  private InetAddress address;
  private int port;

  public FileStorageServiceHandler(DatagramSocket socket, String fileStoragePath)
      throws IOException {
    assembler = new FileStoragePacketAssembler(this);
    decoder = new FileStoragePacketDecoder();
    fileHandler = new FileStorageFileHandler(fileStoragePath);
    sender = new FileStorageSender(assembler, decoder);
    receiver = new FileStorageReceiver(assembler, decoder);
    this.socket = socket;
  }

  public FileStorageServiceHandler(DatagramSocket socket, String fileStoragePath,
      FileStorageServerLogger serverLogger) throws IOException {
    this(socket, fileStoragePath);
    this.serverLogger = serverLogger;
  }

  public void sendFile(Path filePath, long fileSize, boolean log) throws IOException {
    logMessage("Sending " + fileSize + " bytes...", false);
    sender.sendFile(socket, filePath, fileSize, log);
    logMessage("File sent successfully: " + filePath, log);
  }

  public void replaceFile(String fileName, long fileSize, boolean log) throws IOException {
    logMessage("Receiving " + fileSize + " bytes...", false);
    Path filePath = fileHandler.getFileStoragePath(fileName);
    receiver.receiveFile(socket, filePath, fileSize, log);
    logMessage("File received and replaced: " + filePath.toString(), log);
  }

  public void receiveFile(String fileName, long fileSize, boolean log) throws IOException {
    logMessage("Receiving " + fileSize + " bytes...", false);
    Path filePath = fileHandler.updateFileName(fileName);
    receiver.receiveFile(socket, filePath, fileSize, log);
    logMessage("File received and stored at: " + filePath.toString(), log);
  }

  public void deleteFile(String fileName) throws IOException {
    Path deletedFile = fileHandler.deleteFile(fileName);
    logMessage("File deleted: " + deletedFile.toString(), true);
  }

  public void sendFileList() throws IOException {
    byte[] listOfFiles = fileHandler.getFilesInDirectory();
    int listOfFilesLength = listOfFiles.length;
    int i = 0;

    if (listOfFilesLength == 0) {
      socket.send(
          assembler.createPacket("No files stored on the Pi".getBytes(), 0,
              assembler.setFlags(Set.of(LIST, ACK, FINAL))));
    }

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
    for (DatagramPacket packet : packetList) {
      fileList.append(new String(decoder.getPayload(packet)));
    }
    for (String fileName : fileList.toString().split(",")) {
      System.out.println(fileName);
    }
  }

  public long clientRequest(String filePath, FileStorageHeaderFlags flag)
      throws IOException, FileException {
    int retransmits = 0;
    long fileSize = 0;
    DatagramPacket requestPacket = switch (flag) {

      case SEND, REPLACE -> assembler.createRequestPacket(Files.size(Paths.get(filePath)),
          fileHandler.getFileNameBytes(filePath),
          assembler.setFlags(flag));

      case RETRIEVE, DELETE ->
          assembler.createRequestPacket(0, fileHandler.getFileNameBytes(filePath),
              assembler.setFlags(flag));

      case LIST -> assembler.createPacket(new byte[1], 0, assembler.setFlags(flag));

      default -> throw new IOException("Invalid request flag");
    };
    socket.send(requestPacket);

    while (true) {
      socket.setSoTimeout(1000);
      try {
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
          logMessage("File successfully deleted: " + decoder.getFileName(receivedPacket), false);
          break;
        } else if (decoder.hasFlag(receivedPacket, LIST)) {
          List<DatagramPacket> packetList = new ArrayList<>();
          packetList.add(receivedPacket);
          if (decoder.hasFlag(receivedPacket, FINAL)) {
            receiveFileList(packetList);
            break;
          }
        }
      } catch (SocketTimeoutException e) {
        socket.send(requestPacket);
        if (retransmits++ == 10) {
          throw new IOException("Receive timed out");
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

    logMessage(String.format("Request from: %s:%d %s %s", address.toString(), port, flag, fileName),
        true);

    switch (flag) {
      case SEND:
        fileTooLarge(fileSize);
        sendRequestAcknowledgement(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(SEND, ACK))));
        receiveFile(fileName, fileSize, true);
        break;

      case REPLACE:
        fileExists(fileName);
        fileTooLarge(fileSize);
        sendRequestAcknowledgement(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(REPLACE, ACK))));
        replaceFile(fileName, fileSize, true);
        break;

      case RETRIEVE:
        fileExists(fileName);
        fileSize = fileHandler.getFileSize(fileName);
        sendRequestAcknowledgement(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(RETRIEVE, ACK))));
        sendFile(fileHandler.getFileStoragePath(fileName), fileSize, true);
        break;

      case DELETE:
        fileExists(fileName);
        deleteFile(fileName);
        sendRequestAcknowledgement(assembler.createRequestPacket(fileSize, fileName.getBytes(),
            assembler.setFlags(Set.of(DELETE, ACK))));
        break;

      case LIST:
        sendFileList();
        break;

      default:
        throw new RequestException();
    }
  }

  public void sendRequestAcknowledgement(DatagramPacket ackPacket) throws IOException {
    while (true) {
      socket.setSoTimeout(3000);
      try {
        socket.send(ackPacket);

        DatagramPacket requestPacket = assembler.createBufferPacket(PACKET_SIZE);
        socket.receive(requestPacket);

        socket.send(ackPacket);
        System.out.println("Retransmitting ack");
      } catch (SocketTimeoutException e) {
        break;
      }
    }
    socket.setSoTimeout(0);
  }

  public void sendErrorPacket(String error) throws IOException {
    socket.send(assembler.createPacket(error.getBytes(), 0, assembler.setFlags(ERROR)));
  }

  public void fileExists(String fileName) throws FileException {
    if (!fileHandler.fileExists(fileName)) {
      throw new FileException("File '" + fileName + "' does not exist or is not in this directory");
    }
  }

  public void fileTooLarge(long fileSize) throws FileException {
    if (fileSize > (Math.pow(2, 31) - 1) * PAYLOAD_SIZE) {
      throw new FileException("File is too large to send!");
    }
  }

  public void logMessage(String message, boolean log) throws IOException {
    if (serverLogger != null && log) {
      serverLogger.writeToLog(message);
    }
    System.out.println(message);
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
