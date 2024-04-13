package com.nedap.university.service;


import com.nedap.university.packet.FileStoragePacketAssembler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PacketTest {
  private FileStoragePacketAssembler packetAssembler;
  public static final int PORT = 8080;
  public static final String HOSTNAME = "127.0.0.1";
  private InetAddress address;

  @BeforeEach
  public void setup() {
    try {
      address = InetAddress.getByName(HOSTNAME);
    } catch (IOException e) {
      e.printStackTrace();
    }
    packetAssembler = new FileStoragePacketAssembler();
  }

  @Test
  public void testPacketQueue(){
    try {
      byte[] fileBytes = packetAssembler.getFileBytes("./example_files/tiny.pdf");
      Queue<DatagramPacket> queue = packetAssembler.createPacketQueue(address, PORT,"./example_files/tiny.pdf");
      System.out.println("array");
      System.out.println(Arrays.toString(fileBytes));
      System.out.println("array");
      for (DatagramPacket packet : queue) {
        System.out.println(Arrays.toString(packet.getData()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
