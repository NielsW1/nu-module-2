package com.nedap.university;

import com.nedap.university.server.FileStorageServer;
import com.nedap.university.server.QuoteServer;
import java.io.IOException;
import java.net.SocketException;

public class Main {

  private static boolean keepAlive = true;
  private static boolean running = false;

  private Main() {}

  public static void main(String[] args) {
    FileStorageServer server = null;
    try {
      server = new FileStorageServer();
    } catch (SocketException e) {
      System.out.println("Socket error: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("I/O error: " + e.getMessage());
    }
    running = true;
    System.out.println("Running service...");

    initShutdownHook();

    while (keepAlive) {
      try {
        server.runServer();
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (IOException e) {
        System.out.println("I/O error: " + e.getMessage());
      } catch (NullPointerException e) {
        System.out.println("Server is null!");
      }
    }
    System.out.println("Stopped");
    running = false;
  }

  private static void initShutdownHook() {
    final Thread shutdownThread = new Thread() {
      @Override
      public void run() {
        keepAlive = false;
        while (running) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdownThread);
  }
}
