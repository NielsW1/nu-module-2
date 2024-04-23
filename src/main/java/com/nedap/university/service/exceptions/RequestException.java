package com.nedap.university.service.exceptions;

public class RequestException extends Exception {
  public RequestException() {
    super("Invalid request received");
  }

  public RequestException(String message) {
    super(message);
  }

}
