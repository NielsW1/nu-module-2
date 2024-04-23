package com.nedap.university.server;

import com.nedap.university.service.FileStorageServiceHandler;
import com.nedap.university.service.exceptions.FileException;
import com.nedap.university.service.exceptions.RequestException;
import java.io.IOException;
import java.net.DatagramSocket;

public class FileStorageServer {
  public static final int PI_PORT = 8080;
  public static final String PI_HOSTNAME = "172.16.1.1";

  private FileStorageServiceHandler serviceHandler;
  private DatagramSocket socket;

  public FileStorageServer() throws IOException {
    this("/home/pi/FileStorage");
  }

  public FileStorageServer(String fileStoragePath) throws IOException {
    socket = new DatagramSocket(PI_PORT);
    serviceHandler = new FileStorageServiceHandler(socket, fileStoragePath);
  }

  public void handleRequest() throws IOException {
    try {
      serviceHandler.serverHandleRequest();
    } catch (FileException | RequestException e) {
      serviceHandler.sendErrorPacket(e.getMessage());
      System.out.println("File/request error: " + e.getMessage());
    }
  }

  public void closeSocket() {
    socket.close();
  }
}
