package com.nedap.university.client;

import com.nedap.university.packet.FileStorageHeaderFlags;
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
      throw new IOException("File does not exist or is not in this directory");
    }
    if (serviceHandler.clientHandshake(socket, filePath, new HashSet<>())) {
      serviceHandler.sendFile(socket, filePath);
    }
  }

  public void retrieveFile(String fileName) throws IOException {
    if (serviceHandler.clientHandshake(socket, fileName, Set.of(MODE))) {
      serviceHandler.receiveFile(socket, fileName);
    }
  }
}
