package com.nedap.university.service;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileTest {
  private FileStorageFileHandler fileHandler;
  private String path = System.getProperty("user.home") + "/Downloads";

  @BeforeEach
  public void setup() {
    fileHandler = new FileStorageFileHandler(path);
  }

  @Test
  public void testFileList() {
    try {
      Set<String> listofStuff = fileHandler.getFileNames();
      for (String thing : listofStuff) {
        System.out.println(thing);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
