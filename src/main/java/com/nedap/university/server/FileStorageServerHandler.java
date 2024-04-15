package com.nedap.university.server;


import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramSocket;

public class FileStorageServerHandler {
  private FileStorageServiceHandler serviceHandler;
  private DatagramSocket socket;

  public FileStorageServerHandler() throws IOException {
    serviceHandler = new FileStorageServiceHandler("/home/pi/FileStorage");
    socket = new DatagramSocket(FileStorageServiceHandler.PI_PORT);
  }

  public void sendFile() {

  }

  public void receiveFile() {

  }
}
