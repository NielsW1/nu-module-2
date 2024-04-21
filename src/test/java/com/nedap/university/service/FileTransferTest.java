package com.nedap.university.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nedap.university.client.FileStorageClientHandler;
import com.nedap.university.protocol.FileStorageHeaderFlags;
import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.server.FileStorageServer;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileTransferTest {
  private FileStorageServer server;
  private FileStorageClientHandler client;
  private static final String HOSTNAME = "127.0.0.1";
  private static final String file = "./example_files/large.pdf";
  private static final String documents = System.getProperty("user.home") + "/Documents";
  private static final String downloads = System.getProperty("user.home") + "/Downloads";

  @BeforeEach
  public void setup() {
    try {
      client = new FileStorageClientHandler(HOSTNAME, 8080);
      server = new FileStorageServer(documents);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @AfterEach
  protected void finalizeTests() {
    try {
      client.closeSocket();
      server.closeSocket();
      Files.deleteIfExists(Paths.get(documents + "/large.pdf"));
      Files.deleteIfExists(Paths.get(downloads + "/large.pdf"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSendRetrieveFile() {
    try {
      runServerHelper();

      //test sending a file to the server
      assertThrows(FileException.class, () -> client.sendFile("NotaFile"));
      client.sendFile(file);
      assertEquals(-1, Files.mismatch(Paths.get(file), Paths.get(documents + "/large.pdf")));

      //test retrieving previously sent file
      assertThrows(IOException.class, () -> client.retrieveFile("NotAfile"));
      client.retrieveFile("large.pdf");
      assertEquals(-1, Files.mismatch(Paths.get(file), Paths.get(downloads + "/large.pdf")));
    } catch (IOException | FileException e) {
      e.printStackTrace();
    }
  }

  public void runServerHelper() {
    new Thread(() -> {
      while (true) {
        try {
          server.runServer();
        } catch (SocketException e) {
          break;
        } catch (FileException | IOException e) {
          System.out.println(e.getMessage());
        }
      }
    }).start();
  }
}
