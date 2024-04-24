package com.nedap.university.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;

public class FileStorageServerLogger {

  private final Path logPath;

  public FileStorageServerLogger(String logPath) {
    this.logPath = Paths.get(logPath + "/log.txt");
  }

  public void writeToLog(String message) throws IOException {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Files.write(logPath, String.format("%1.19s | %s\n", timestamp, message).getBytes(),
        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
  }
}
