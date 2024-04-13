package com.nedap.university.client;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class FileStorageClientHandler {
  private FileStorageServiceHandler serviceHandler;
  private DatagramSocket socket;

  public FileStorageClientHandler() throws IOException {
    serviceHandler = new FileStorageServiceHandler(System.getProperty("user.home") + "/Downloads");
    socket = new DatagramSocket();
  }

  public void sendFile(String filePath) {

  }

  public void retrieveFile() {

  }
}
