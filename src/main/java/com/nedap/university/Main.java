package com.nedap.university;

import com.nedap.university.server.QuoteServer;
import java.io.IOException;
import java.net.SocketException;

public class Main {

  private static boolean keepAlive = true;
  private static boolean running = false;

  private Main() {}

  public static void main(String[] args) {
    QuoteServer server = null;
    try {
      server = new QuoteServer(8080);
      server.loadQuotesFromFile("/home/pi/quotes.txt");
    } catch (SocketException e) {
      System.out.println("Socket error: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("I/O error: " + e.getMessage());
    }
    running = true;
    System.out.println("Hello, Nedap University!");

   initShutdownHook();

    while (keepAlive) {
      try {
        System.out.println("Running service...");
        server.service();
//        Thread.sleep(1000);
//      } catch (InterruptedException) {
//        Thread.currentThread().interrupt();
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
