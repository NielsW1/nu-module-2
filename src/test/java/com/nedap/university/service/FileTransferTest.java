package com.nedap.university.service;

import static com.nedap.university.protocol.FileStorageHeaderFlags.REPLACE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nedap.university.client.FileStorageClientHandler;
import com.nedap.university.server.FileStorageServer;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileTransferTest {

  private FileStorageServer server;
  private FileStorageClientHandler client;
  private static final String HOSTNAME = "127.0.0.1";
  private static final String file = "./example_files/large.pdf";
  private static final String resources = "./src/main/resources";
  private static final String downloads = System.getProperty("user.home") + "/Downloads";

  @BeforeEach
  public void setup() {
    try {
      client = new FileStorageClientHandler(HOSTNAME, 8080);
      server = new FileStorageServer(resources, resources);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @AfterEach
  protected void finalizeTests() {
    try {
      client.closeSocket();
      server.closeSocket();
      Files.deleteIfExists(Paths.get(resources + "/large.pdf"));
      Files.deleteIfExists(Paths.get(downloads + "/large.pdf"));
      Files.deleteIfExists(Paths.get(downloads + "/large(1).pdf"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSendRetrieveFile() throws IOException, InterruptedException {
    try {
      runServerHelper();
      Path oriPath = Paths.get(file);

      //test sending a file to the server
      assertThrows(FileException.class, () -> client.sendReplaceFile("NotaFile.png", SEND));
      client.sendReplaceFile(file, SEND);
      assertEquals(-1, Files.mismatch(oriPath, Paths.get(resources + "/large.pdf")));

      //test replace file
      assertThrows(FileException.class,
          () -> client.sendReplaceFile("DefinitelyNotAFile.pdf", REPLACE));
      client.sendReplaceFile(file, REPLACE);
      assertEquals(-1, Files.mismatch(oriPath, Paths.get(resources + "/large.pdf")));

      //test retrieving previously sent file
      assertThrows(FileException.class, () -> client.retrieveFile("AlsoNotAfile.jpg"));
      client.retrieveFile("large.pdf");
      assertEquals(-1, Files.mismatch(oriPath, Paths.get(downloads + "/large.pdf")));

      //test filename gets updated if file is retrieved again
      client.retrieveFile("large.pdf");
      Path newPath = Paths.get(downloads + "/large(1).pdf");
      assertTrue(Files.exists(newPath));
      assertEquals(-1, Files.mismatch(oriPath, newPath));

      //test delete file
      assertThrows(FileException.class, () -> client.deleteFile("MostCertainlyNotAFile.mp4"));
      Path deletePath = Paths.get(resources + "/large.pdf");
      assertTrue(Files.exists(deletePath));
      client.deleteFile("large.pdf");
      assertFalse(Files.exists(deletePath));

    } catch (FileException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testListFiles() {
    runServerHelper();
    try {
      client.listOfFiles();
    } catch (IOException | FileException e) {
      e.printStackTrace();
    }
  }

  public void runServerHelper() {
    new Thread(() -> {
      while (true) {
        try {
          server.handleRequest();
        } catch (SocketException e) {
          break;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
}
