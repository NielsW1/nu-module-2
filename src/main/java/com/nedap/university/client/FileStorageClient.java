package com.nedap.university.client;

import static com.nedap.university.protocol.FileStorageHeaderFlags.REPLACE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;

import com.nedap.university.protocol.FileStorageHeaderFlags;
import com.nedap.university.server.FileStorageServer;
import com.nedap.university.service.exceptions.FileException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class FileStorageClient {

  private FileStorageClientHandler clientHandler;

  public FileStorageClient() {
    runFileStorageClient();
  }

  public void runFileStorageClient() {
    Scanner input = new Scanner(System.in);

    try {
      clientHandler = new FileStorageClientHandler(FileStorageServer.PI_HOSTNAME,
          FileStorageServer.PI_PORT);
      System.out.println("Welcome to Niels' Raspberry Pi file storage system!");

      while (true) {
        System.out.println("""
            Enter one of the following commands (1-6):
            1 ...<SEND>..... Send a file to the Pi
            2 ...<REPLACE>.. Send and replace a file on the Pi
            3 ...<RETRIEVE>. Retrieve a file from the Pi
            4 ...<DELETE>... Delete a file from the Pi
            5 ...<LIST>..... Receive a list of all files currently stored on the Pi
            6 ...<EXIT>..... Exit the client""");

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
          if (command == 6) {
            closeClient();
            break;
          }
          try {
            switch (command) {
              case 1:
                System.out.println(
                    "Enter the path to the file (e.g /users/user/documents/file.pdf):");
                if (input.hasNextLine()) {
                  clientHandler.sendReplaceFile(input.nextLine(), SEND);
                }
                break;

              case 2:
                System.out.println("Enter the path to the file (e.g /users/user/documents/file.pdf):");
                if (input.hasNextLine()) {
                  clientHandler.sendReplaceFile(input.nextLine(), REPLACE);
                }
                break;

              case 3:
                System.out.println("Enter the name of the file you want to retrieve:");
                if (input.hasNextLine()) {
                  clientHandler.retrieveFile(input.nextLine());
                }
                break;

              case 4:
                System.out.println("Enter the name of the file you want to delete:");
                String fileName;
                if (input.hasNextLine()) {
                  fileName = input.nextLine();
                  System.out.println("Are you sure you want to delete: " + fileName + "? (y/n)");
                  if (input.hasNextLine()) {
                    if (input.nextLine().contains("y")) {
                      clientHandler.deleteFile(fileName);
                    }
                  }
                }
                break;

              case 5:
                System.out.println("Currently stored files: ");
                clientHandler.listOfFiles();
                break;

              default:
                System.out.println("Invalid input: " + command);
                continue;
            }
          } catch (FileException | IOException e) {
            System.out.println("I/O error: " + e.getMessage() + "\n");
            continue;
          } catch (InterruptedException e) {
            System.out.println("Interrupt error: " + e.getMessage() + "\n");
          }
          System.out.println("Would you like to send/retrieve another file? (y/n)");
          if (input.hasNextLine()) {
            inputLine = input.nextLine();
            if (inputLine.contains("y")) {
            } else {
              closeClient();
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

  public void closeClient() {
    clientHandler.closeSocket();
    System.out.println("Exiting client.....");
  }

  public static void main(String[] args) {
    new FileStorageClient();
  }
}
