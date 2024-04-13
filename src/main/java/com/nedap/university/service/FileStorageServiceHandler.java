package com.nedap.university.service;

import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketReader;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class FileStorageServiceHandler {
  public static final int PORT = 8080;
  public static final String HOSTNAME = "172.16.1.1";
  private InetAddress piAddress;
  private FileStoragePacketAssembler packetAssembler;
  private FileStoragePacketReader packetReader;
  private String fileStoragePath;

  public FileStorageServiceHandler(String fileStoragePath) throws IOException {
    packetAssembler = new FileStoragePacketAssembler();
    packetReader = new FileStoragePacketReader();
    piAddress = InetAddress.getByName(HOSTNAME);
    this.fileStoragePath = fileStoragePath;
  }

  public void sendFile(DatagramSocket socket, String filePath) {
//    try {
//      DatagramPacket requestToSend = new DatagramPacket(new byte[1], 1, address, port);
//      socket.send(requestToSend);
//      System.out.println("Request sent!");
//      DatagramPacket response = new DatagramPacket(new byte[1], 1);
//      socket.receive(response);
//      System.out.println("Response received!");
//
//      DatagramPacket packet = packetHandler.assemblePacket(filePath);
//      socket.send(packet);
//      System.out.println("Packet sent!");
//
//      Thread.sleep(10000);
//
//    } catch (SocketTimeoutException ex) {
//      System.out.println("Timeout error: " + ex.getMessage());
//      ex.printStackTrace();
//    } catch (IOException ex) {
//      System.out.println("Client error: " + ex.getMessage());
//      ex.printStackTrace();
//    } catch (InterruptedException ex) {
//      ex.printStackTrace();
//    }
  }

  public void receiveFile() {

  }

  public void clientSendFileRequest() {

  }

  public void clientRetrieveFileRequest() {

  }

}
