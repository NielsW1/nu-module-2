package com.nedap.university.client;

import com.nedap.university.service.FileStorageServiceHandler;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.nedap.university.protocol.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;

public class FileStorageClientHandler {
  private final FileStorageServiceHandler serviceHandler;
  private final DatagramSocket socket;

  public FileStorageClientHandler(String address, int port) throws IOException {
    socket = new DatagramSocket();
    serviceHandler = new FileStorageServiceHandler(socket, System.getProperty("user.home") + "/Downloads");
    serviceHandler.setAddressAndPort(InetAddress.getByName(address), port);
  }

  public void sendFile(String pathToFile) throws IOException, FileException {
    Path filePath = Paths.get(pathToFile);
    if (Files.notExists(filePath)) {
      throw new FileException();
    }
    long fileSize = serviceHandler.clientHandshake(pathToFile, SEND);
    System.out.println("Handshake successful, Sending file...");
    serviceHandler.sendFile(filePath, fileSize);
    System.out.println("File sent successfully");
  }

  public void retrieveFile(String fileName) throws IOException {
    long fileSize = serviceHandler.clientHandshake(fileName, RETRIEVE);
    System.out.println("Handshake successful, retrieving file...");
    String outputPath = serviceHandler.receiveFile(fileName, fileSize);
    System.out.println("File downloaded to " + outputPath);
  }

  public void closeSocket() {
    socket.close();
  }

  public DatagramSocket getSocket() {
    return socket;
  }
}
