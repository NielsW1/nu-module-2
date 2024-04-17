package com.nedap.university.service;


import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.MODE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PacketTest {
  private FileStoragePacketAssembler packetAssembler;
  private FileStoragePacketDecoder packetReader;
  private FileStorageServiceHandler serviceHandler;
  private FileStorageFileHandler fileHandler;
  public static final int PORT = 8080;
  public static final String HOSTNAME = "127.0.0.1";
  private InetAddress address;

  @BeforeEach
  public void setup() {
    try {
      address = InetAddress.getByName(HOSTNAME);
      packetReader = new FileStoragePacketDecoder();
      serviceHandler = new FileStorageServiceHandler("bla");
      packetAssembler = new FileStoragePacketAssembler(serviceHandler);
      fileHandler = new FileStorageFileHandler("./example_files");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void setFlagsTest() {
    int flags = packetAssembler.setFlags(Set.of(FINAL, ACK, MODE));
    assertEquals(7, flags);
    flags = packetAssembler.setFlags(Set.of(FINAL, MODE));
    assertEquals(5, flags);
    flags = packetAssembler.setFlags(new HashSet<>());
    assertEquals(0, flags);
  }

  @Test
  public void hasFlagTest() {
    byte[] packet = new byte[1];
    packet = packetAssembler.addPacketHeader(packet, 0, packetAssembler.setFlags(Set.of(ACK)), 9);
    DatagramPacket dataPacket = new DatagramPacket(packet, packet.length, address, PORT);
    assertTrue(packetReader.hasFlag(dataPacket, ACK));
  }

  @Test
  public void regexTest() {
    String[] splitFileName = "thisisafile213(2).pdf".split("[.]|(\\([0-9]+\\))");
    System.out.println(Arrays.toString(splitFileName));
    System.out.println(String.format("|%100s|", " "));
  }
}
