package com.nedap.university.service.exceptions;

public class FileException extends Exception {
  public FileException() {
    super("File does not exist or is not in this directory");
  }

  public FileException(String message) {
    super(message);
  }
}
