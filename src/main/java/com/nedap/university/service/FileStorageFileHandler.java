package com.nedap.university.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
      String[] splitFileName = fileName.split("(\\([0-9]+\\)[.])");
      if (splitFileName.length < 2) {
        int dot = fileName.lastIndexOf(".");
        splitFileName = new String[] {fileName.substring(0, dot), fileName.substring(dot + 1)};
      }
      fileName = splitFileName[0] + "(" + fileNum++ + ")." + splitFileName[1];
    }
    return Paths.get((fileStoragePath + "/" + fileName));
  }

  public boolean fileExists(String fileName) {
    return Files.exists(Paths.get(fileStoragePath + "/" + fileName));
  }

  public Path getFileStoragePath(String fileName) {
    return Paths.get(fileStoragePath + "/" + fileName);
  }

  public long getFileSize(String fileName) throws IOException {
    return Files.size(getFileStoragePath(fileName));
  }

  public Path deleteFile(String fileName) throws IOException {
    Path fileToDelete = getFileStoragePath(fileName);
    Files.deleteIfExists(fileToDelete);
    return fileToDelete;
  }

  public byte[] getFilesInDirectory() throws IOException {
    StringBuilder fileString = new StringBuilder();
    for (String file: getFileNames()) {
      fileString.append(file).append(",");
    }
    return fileString.toString().getBytes();
  }

  private Set<String> getFileNames() throws IOException{
    try (Stream<Path> stream = Files.list(Paths.get(fileStoragePath))) {
      return stream
          .filter(file -> !Files.isDirectory(file))
          .map(Path::getFileName)
          .map(Path::toString)
          .collect(Collectors.toSet());
    }
  }

}
