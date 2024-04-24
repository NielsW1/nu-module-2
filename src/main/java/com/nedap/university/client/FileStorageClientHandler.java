package com.nedap.university.client;

import com.nedap.university.protocol.FileStorageHeaderFlags;
import com.nedap.university.service.FileStorageServiceHandler;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.nedap.university.protocol.FileStorageHeaderFlags.DELETE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.LIST;
import static com.nedap.university.protocol.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;

public class FileStorageClientHandler {
  private final FileStorageServiceHandler serviceHandler;
  private final DatagramSocket socket;

  public FileStorageClientHandler(String address, int port) throws IOException {
    socket = new DatagramSocket();
    serviceHandler = new FileStorageServiceHandler(socket, System.getProperty("user.home") + "/Downloads");
    serviceHandler.setAddressAndPort(InetAddress.getByName(address), port);
  }

  public void sendReplaceFile(String pathToFile, FileStorageHeaderFlags flag) throws IOException, FileException {
    Path filePath = Paths.get(pathToFile);
    if (Files.notExists(filePath)) {
      throw new FileException("File '" + pathToFile + "' does not exist or is not in this directory");
    }
    long fileSize = serviceHandler.clientRequest(pathToFile, flag);
    serviceHandler.sendFile(filePath, fileSize, false);
  }

  public void retrieveFile(String fileName) throws IOException, FileException {
    long fileSize = serviceHandler.clientRequest(fileName, RETRIEVE);
    serviceHandler.receiveFile(fileName, fileSize, false);
  }

  public void deleteFile(String fileName) throws IOException, FileException {
    serviceHandler.clientRequest(fileName, DELETE);
  }

  public void listOfFiles() throws IOException, FileException {
    serviceHandler.clientRequest("", LIST);
  }

  public void closeSocket() {
    socket.close();
  }
}
