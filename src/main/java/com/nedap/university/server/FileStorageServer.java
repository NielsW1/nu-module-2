package com.nedap.university.server;

import com.nedap.university.client.FileStorageClientHandler;
import com.nedap.university.service.FileStorageServiceHandler;
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
    serviceHandler = new FileStorageServiceHandler("/home/pi/FileStorage");
    socket = new DatagramSocket(PI_PORT);
  }

  public void runServer() throws IOException {
    serviceHandler.serverHandshake(socket);
  }
}
