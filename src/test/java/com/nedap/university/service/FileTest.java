package com.nedap.university.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileTest {
  private FileStorageFileHandler fileHandler;
  private String path = "./example_files";

  @BeforeEach
  public void setup() {
    fileHandler = new FileStorageFileHandler(path);
  }

  @Test
  public void testgetFileSize() throws IOException {
      assertEquals(31498458, fileHandler.getFileSize("/large.pdf"));
  }

  @Test
  public void fileNameBytesTest() {;
    assertArrayEquals("large.pdf".getBytes(), fileHandler.getFileNameBytes(path + "/large.pdf"));
  }

  @Test
  public void getFileListTest() throws IOException {
    String fileList = "tiny.pdf,medium.pdf,large.pdf,";
    assertEquals(fileList, new String(fileHandler.getFilesInDirectory()));
  }

  @Test
  public void testFileExists() {
    assertTrue(fileHandler.fileExists("large.pdf"));
    assertFalse(fileHandler.fileExists("NotAFile.pdf"));
  }

  @Test
  public void updateFileNameTest() {
    Path filePath = Paths.get(path + "/large(1).pdf");
    Path otherPath = Paths.get(path + "/NotAFile.txt");

    assertEquals(filePath, fileHandler.updateFileName("large.pdf"));
    assertEquals(otherPath, fileHandler.updateFileName("NotAFile.txt"));
  }

}
