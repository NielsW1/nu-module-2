package com.nedap.university.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nedap.university.client.FileStorageClientHandler;
import com.nedap.university.server.FileStorageServer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileStorageFileTest {
  private FileStorageFileHandler fileHandler;
  private static final String downloads = System.getProperty("user.home") + "/Downloads";

  @BeforeEach
  public void setup() {
    fileHandler = new FileStorageFileHandler(downloads);
  }

  @AfterAll
  public static void removeFile() {
    try {
      Files.deleteIfExists(Paths.get(downloads + "/tempFile"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testFileExists() {
    assertTrue(fileHandler.fileExists("/tempFile"));
  }




  }
