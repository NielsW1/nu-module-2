package com.nedap.university.service;

import com.nedap.university.service.exceptions.FileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FileStorageFileHandler {
  private final String fileStoragePath;

  public FileStorageFileHandler(String fileStoragePath) {
    this.fileStoragePath = fileStoragePath;
  }

  public byte[] getFileNameBytes(String filePath) {
    String[] splitPath = filePath.split("/+");
    return splitPath[splitPath.length - 1].getBytes();
  }

  public Path updateFileName(String fileName) {
    int fileNum = 1;
    while (Files.exists(Paths.get(fileStoragePath + "/" + fileName))) {
      String[] splitFileName = fileName.split("[.]|(\\([0-9]+\\))");
      fileName = splitFileName[0] + "(" + fileNum++ + ")." + splitFileName[splitFileName.length - 1];
    }
    return Paths.get((fileStoragePath + "/" + fileName));
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

  public Path getFileStoragePath(String fileName) {
    return Paths.get(fileStoragePath + "/" + fileName);
  }

  public long getFileSize(String fileName) throws IOException{
    return Files.size(getFileStoragePath(fileName));
  }

}
