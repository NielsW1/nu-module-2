package com.nedap.university.server;


import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;

public class FileStorageServerHandler {
  private FileStorageServiceHandler serviceHandler;

  public FileStorageServerHandler() throws IOException {
    serviceHandler = new FileStorageServiceHandler("/home/pi/FileStorage");
  }
}
