package com.nedap.university.server;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FileStorageServer {

  private DatagramSocket socket;
  private FileStorageServerHandler serverHandler;

  public FileStorageServer() throws IOException {
    serverHandler = new FileStorageServerHandler();
    socket = new DatagramSocket(FileStorageServiceHandler.PORT);
  }

  public void runServer() throws IOException {
//    DatagramPacket request = new DatagramPacket(new byte[1], 1);
//    socket.receive(request);
//    System.out.println("Request received");
//    DatagramPacket response = new DatagramPacket(new byte[1], 1, request.getAddress(),
//        request.getPort());
//    socket.send(response);
//
//    byte[] buffer = new byte[65535];
//    DatagramPacket data = new DatagramPacket(buffer, buffer.length);
//    socket.receive(data);
//    System.out.println("Data received");
//
//    byte[] receivedBytes = new byte[data.getLength()];
//    System.arraycopy(buffer, 0, receivedBytes, 0, data.getLength());
//    serverHandler.writeBytesToFile(receivedBytes, "testFile.pdf");
  }

  public static void main(String[] args) {
    try {
      FileStorageServer server = new FileStorageServer();
      server.runServer();
    } catch (SocketException e) {
      System.out.println("Socket error: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("I/O error: " + e.getMessage());
    }
  }
}
