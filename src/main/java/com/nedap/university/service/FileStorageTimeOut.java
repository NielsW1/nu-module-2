package com.nedap.university.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

public class FileStorageTimeOut extends TimerTask {
  private DatagramSocket socket;
  private DatagramPacket packet;

  public FileStorageTimeOut(DatagramSocket socket, DatagramPacket packet) {
    this.socket = socket;
    this.packet = packet;
  }

  @Override
  public void run() {
    try {
      socket.send(packet);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
