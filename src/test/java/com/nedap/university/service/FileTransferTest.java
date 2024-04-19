package com.nedap.university.service;

import com.nedap.university.client.FileStorageClientHandler;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import com.nedap.university.server.FileStorageServer;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileTransferTest {
  private FileStorageServer server;
  private FileStorageClientHandler client;
  private DatagramSocket socket;
  public static final String HOSTNAME = "127.0.0.1";
  String file;

  @BeforeEach
  public void setup() {
    try {
      file = "./example_files/large.pdf";
      client = new FileStorageClientHandler(HOSTNAME, 8080);
      socket = new DatagramSocket(8080);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testService() {
    try {
      client.sendFile(file);
    } catch (IOException | FileException e) {
      e.printStackTrace();
    }
  }
}
