package com.nedap.university.packet;

import com.nedap.university.service.FileStorageServiceHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FileStoragePacketSender extends Thread {
  private DatagramSocket socket;
  private DatagramPacket packet;

  public FileStoragePacketSender(DatagramSocket socket, DatagramPacket packet) {
    this.packet = packet;
    this.socket = socket;
  }

  @Override
  public void run() {
    while (true) {
      try {
        socket.send(packet);
        Thread.sleep(500);
      } catch (IOException | InterruptedException ignored) {
        break;
      }
    }
  }
}
