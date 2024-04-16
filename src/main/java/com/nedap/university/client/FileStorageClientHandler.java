package com.nedap.university.client;

import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.service.FileStorageFileHandler;
import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.nedap.university.packet.FileStorageHeaderFlags.MODE;

public class FileStorageClientHandler {
  private final FileStorageServiceHandler serviceHandler;
  private final DatagramSocket socket;

  public FileStorageClientHandler() throws IOException {
    serviceHandler = new FileStorageServiceHandler(System.getProperty("user.home") + "/Downloads");
    socket = new DatagramSocket();
  }

  public void sendFile(String filePath) throws IOException {
    if (Files.notExists(Paths.get(filePath))) {
      throw new IOException(FileStorageFileHandler.FILE_ERROR);
    }
    if (serviceHandler.clientHandshake(socket, filePath, new HashSet<>())) {
      System.out.println("Handshake successful, Sending file...");
      serviceHandler.sendFile(socket, filePath);
      System.out.println("File sent successfully");
    }
  }

  public void retrieveFile(String fileName) throws IOException {
    if (serviceHandler.clientHandshake(socket, fileName, Set.of(MODE))) {
      System.out.println("Handshake successful, retrieving file...");
      String outputFileName = serviceHandler.receiveFile(socket, fileName);
      System.out.println("File downloaded to " + System.getProperty("user.home") + "/Downloads/" + outputFileName);
    }
  }
}
