package com.nedap.university.server;

import com.nedap.university.service.FileStorageServiceHandler;
import com.nedap.university.service.exceptions.FileException;
import com.nedap.university.service.exceptions.RequestException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileStorageServer {

  public static final int PI_PORT = 8080;
  public static final String PI_HOSTNAME = "172.16.1.1";

  private final FileStorageServiceHandler serviceHandler;
  private final DatagramSocket socket;

  public FileStorageServer() throws IOException {
    this("/home/pi/FileStorage", "/home/pi/FileStorage/ServerLog");
  }

  public FileStorageServer(String fileStoragePath, String logPath) throws IOException {
    if (!Files.exists(Paths.get(logPath))) {
      logPath = Files.createDirectories(Paths.get(logPath)).toString();
    }
    socket = new DatagramSocket(PI_PORT);
    FileStorageServerLogger serverLogger = new FileStorageServerLogger(logPath);
    serviceHandler = new FileStorageServiceHandler(socket, fileStoragePath, serverLogger);
  }

  public void handleRequest() throws IOException {
    try {
      serviceHandler.serverHandleRequest();
    } catch (IOException | FileException | RequestException e) {
      serviceHandler.sendErrorPacket(e.getMessage());
      serviceHandler.logMessage("File/request error: " + e.getMessage(), true);
    }
  }

  public void closeSocket() {
    socket.close();
  }
}
