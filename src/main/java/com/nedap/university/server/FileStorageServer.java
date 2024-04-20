package com.nedap.university.server;

import com.nedap.university.client.FileStorageClientHandler;
import com.nedap.university.service.FileStorageServiceHandler;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FileStorageServer {
  public static final int PI_PORT = 8080;
  public static final String PI_HOSTNAME = "172.16.1.1";

  private FileStorageServiceHandler serviceHandler;
  private DatagramSocket socket;

  public FileStorageServer() throws IOException {
    this("/home/pi/FileStorage");
  }

  public FileStorageServer(String fileStoragePath) throws IOException {
    serviceHandler = new FileStorageServiceHandler(fileStoragePath);
    socket = new DatagramSocket(PI_PORT);
  }

  public void runServer() throws IOException, FileException {
    serviceHandler.serverHandshake(socket);
  }
}
