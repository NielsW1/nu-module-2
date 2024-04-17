package com.nedap.university.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FileStorageFileHandler {
  public static final String FILE_ERROR = "File does not exist or is not in this directory";
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

  public String writeBytesToFile(byte[] fileBytes, String fileName) throws IOException {
    int fileNum = 1;

    while (Files.exists(Paths.get(fileStoragePath + "/" + fileName))) {
      String[] splitFileName = fileName.split("[.]");
      fileName = splitFileName[0] + "(" + fileNum++ + ")." + splitFileName[1];
    }
    FileOutputStream outputStream = new FileOutputStream(fileStoragePath + "/" + fileName);
    outputStream.write(fileBytes);

    return fileName;
  }

  public byte[] getByteArrayFromMap(HashMap<Integer, byte[]> receivedPacketMap) {
    ByteArrayOutputStream combinedArray = new ByteArrayOutputStream();
    List<Integer> sequenceNumbers = new ArrayList<>(receivedPacketMap.keySet());
    Collections.sort(sequenceNumbers);
    for (Integer sequenceNumber : sequenceNumbers) {
      combinedArray.write(receivedPacketMap.get(sequenceNumber), 0, receivedPacketMap.get(sequenceNumber).length);
    }
    return combinedArray.toByteArray();
  }

  public boolean fileExists(String fileName) {
    return Files.exists(Paths.get(fileStoragePath + "/" + fileName));
  }

  public String getFileStoragePath() {
    return fileStoragePath;
  }

}
