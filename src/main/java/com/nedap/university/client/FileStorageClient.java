package com.nedap.university.client;

import com.nedap.university.server.FileStorageServer;
import com.nedap.university.service.exceptions.FileException;
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
      clientHandler = new FileStorageClientHandler(FileStorageServer.PI_HOSTNAME, FileStorageServer.PI_PORT);
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
            System.out.println("Invalid input: " + inputLine);
            continue;
          }
          if (command == 3) {
            System.out.println("Exiting client.....");
            break;
          }
          try {
            switch (command) {
              case 1:
                System.out.println(
                    "Enter the path to the file (e.g /users/user/documents/file.pdf):");
                if (input.hasNextLine()) {
                  clientHandler.sendFile(input.nextLine());
                }
                break;
              case 2:
                System.out.println("Enter the name of the file you want to retrieve:");
                if (input.hasNextLine()) {
                  clientHandler.retrieveFile(input.nextLine());
                }
                break;
              default:
                System.out.println("Invalid input: " + command);
                continue;
            }
          } catch (IOException | FileException e) {
            System.out.println("I/O error: " + e.getMessage() + "\n");
            continue;
          }
          System.out.println("Would you like to send/retrieve another file? (y/n)");
          if (input.hasNextLine()) {
            inputLine = input.nextLine();
            if (inputLine.contains("y")) {
            } else {
              System.out.println("Exiting client.....");
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
      System.out.println("I/O error: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    new FileStorageClient();
  }
}
