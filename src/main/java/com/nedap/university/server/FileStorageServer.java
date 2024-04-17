package com.nedap.university.server;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FileStorageServer {

  private FileStorageServiceHandler serviceHandler;
  private DatagramSocket socket;

  public FileStorageServer() throws IOException {
    serviceHandler = new FileStorageServiceHandler("/home/pi/FileStorage");
    socket = new DatagramSocket(FileStorageServiceHandler.PI_PORT);
  }

  public void runServer() throws IOException {
    serviceHandler.serverHandshake(socket);
  }
}
