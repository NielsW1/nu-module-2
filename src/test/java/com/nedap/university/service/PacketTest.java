package com.nedap.university.service;


import com.nedap.university.packet.FileStorageHeaderFlags;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.xml.crypto.Data;
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
  private FileStoragePacketReader packetReader;
  private FileStorageServiceHandler serviceHandler;
  private FileStorageFileHandler fileHandler;
  public static final int PORT = 8080;
  public static final String HOSTNAME = "127.0.0.1";
  private InetAddress address;

  @BeforeEach
  public void setup() {
    try {
      address = InetAddress.getByName(HOSTNAME);
      packetAssembler = new FileStoragePacketAssembler();
      packetReader = new FileStoragePacketReader();
      serviceHandler = new FileStorageServiceHandler("bla");
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
  public void testPacketQueue() {
    try {
      byte[] fileBytes = fileHandler.getFileBytes("./example_files/large.pdf");
      Queue<DatagramPacket> packetQueue = packetAssembler.createPacketQueue(address, PORT, fileBytes);
      HashMap<Integer, byte[]> receivedPacketMap = new HashMap<>();

      System.out.println(packetQueue.size());

      for (DatagramPacket packet : packetQueue) {
        receivedPacketMap.put(packetReader.getSequenceNumber(packet), packetReader.getPayload(packet));
      }
      byte[] receivedFileBytes = fileHandler.getByteArrayFromMap(receivedPacketMap);
      fileHandler.writeBytesToFile(receivedFileBytes, "test.pdf");
      assertEquals(fileBytes.length, receivedFileBytes.length);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void hasFlagTest() {
    byte[] packet = new byte[1];
    packet = packetAssembler.addPacketHeader(packet, 0, packetAssembler.setFlags(Set.of(ACK)), 9);
    DatagramPacket dataPacket = new DatagramPacket(packet, packet.length, address, PORT);
    assertTrue(packetReader.hasFlag(dataPacket, ACK));
  }
}
