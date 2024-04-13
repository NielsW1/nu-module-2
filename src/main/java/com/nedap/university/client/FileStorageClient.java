package com.nedap.university.client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class FileStorageClient {
  private FileStorageClientHandler clientHandler;

  public FileStorageClient() {
    runFileStorageClient();
  }

  public void runFileStorageClient(){
    Scanner input = new Scanner(System.in);

    try {
      clientHandler = new FileStorageClientHandler();
      System.out.println("Welcome to Niels' Raspberry Pi file storage system!");

      while (true) {
        System.out.println("""
            Enter one of the following commands:
            1 ......... Send a file to the Pi
            2 ......... Retrieve a file from the Pi
            3 ......... Exit the client""");

        String inputLine;
        if (input.hasNextLine()) {
          inputLine = input.nextLine();
          int command;

          try {
            command = Integer.parseInt(inputLine);
          } catch (NumberFormatException e) {
            System.out.println("Invalid input!");
            continue;
          }
          if (command == 3) {
            System.out.println("Goodbye!");
            break;
          }
          switch(command) {
            case 1:
              System.out.println("Enter the path to the file (e.g /users/user/documents/file.pdf)");
              if (input.hasNextLine()) {
                inputLine = input.nextLine();
                clientHandler.sendFile(inputLine);
              }
              break;
            case 2:
              clientHandler.retrieveFile();
              break;
            default:
              System.out.println("Invalid input!");
              continue;
          }
          System.out.println("Would you like to send/retrieve another file? (y/n)");
          if (input.hasNextLine()) {
            inputLine = input.nextLine();
            if (inputLine.contains("y")) {
            } else {
              System.out.println("Goodbye!");
              break;
            }
          }
        }
      }
    } catch (UnknownHostException e) {
      System.out.println("Unknown host: " + e.getMessage());
    } catch (SocketException e) {
      System.out.println("Socket error: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("I/O error:" + e.getMessage());
    }
  }

  public static void main(String[] args) {
    new FileStorageClient();
  }
}
