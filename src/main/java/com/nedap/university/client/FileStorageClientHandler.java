package com.nedap.university.client;

import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.server.FileStorageServer;
import com.nedap.university.service.FileStorageFileHandler;
import com.nedap.university.service.FileStorageServiceHandler;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.nedap.university.packet.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.packet.FileStorageHeaderFlags.SEND;

public class FileStorageClientHandler {
  private final FileStorageServiceHandler serviceHandler;
  private final DatagramSocket socket;

  public FileStorageClientHandler(String address, int port) throws IOException {
    serviceHandler = new FileStorageServiceHandler(System.getProperty("user.home") + "/Downloads");
    serviceHandler.setAddressAndPort(InetAddress.getByName(address), port);
    socket = new DatagramSocket();
  }

  public void sendFile(String pathToFile) throws IOException, FileException {
    Path filePath = Paths.get(pathToFile);
    if (Files.notExists(filePath)) {
      throw new FileException();
    }
    long fileSize = serviceHandler.clientHandshake(socket, pathToFile, SEND);
    System.out.println("Handshake successful, Sending file...");
    serviceHandler.sendFile(socket, filePath, fileSize);
    System.out.println("File sent successfully");
  }

  public void retrieveFile(String fileName) throws IOException {
    long fileSize = serviceHandler.clientHandshake(socket, fileName, RETRIEVE);
    System.out.println("Handshake successful, retrieving file...");
    String outputPath = serviceHandler.receiveFile(socket, fileName, fileSize);
    System.out.println("File downloaded to " + outputPath);
  }
}
