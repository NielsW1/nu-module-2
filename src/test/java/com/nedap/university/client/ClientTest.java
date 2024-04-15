package com.nedap.university.client;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClientTest {
  private FileStorageClientHandler clientHandler;
  private FileStorageServiceHandler serviceHandler;

  @BeforeEach
  public void setup() {
    try {
      clientHandler = new FileStorageClientHandler();
      serviceHandler = new FileStorageServiceHandler("bla");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
