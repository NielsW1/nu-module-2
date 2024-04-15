package com.nedap.university.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileStorageFileHandler {
  private final String fileStoragePath;

  public FileStorageFileHandler(String fileStoragePath) {
    this.fileStoragePath = fileStoragePath;
  }

  public byte[] getFileNameBytes(String filePath) {
    String[] splitPath = filePath.split("/+");
    return splitPath[splitPath.length - 1].getBytes();
  }

  public byte[] getFileBytes(String filePath) throws IOException {
    File file = new File(filePath);
    byte[] fileBytes = new byte[(int) file.length()];

    FileInputStream inputStream = new FileInputStream(file);
    inputStream.read(fileBytes);

    return fileBytes;
  }

  public void writeBytesToFile(byte[] fileBytes, String fileName) throws IOException {
    if (Files.exists(Paths.get(fileStoragePath + "/" + fileName))) {
      throw new IOException("File by that name already exists in this directory");
    }
    FileOutputStream outputStream = new FileOutputStream(fileStoragePath + "/" + fileName);
    outputStream.write(fileBytes);
  }

}
