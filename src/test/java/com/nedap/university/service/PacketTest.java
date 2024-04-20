package com.nedap.university.service;


import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.protocol.FileStoragePacketDecoder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.nedap.university.protocol.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.protocol.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.protocol.FileStorageHeaderFlags.SEND;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.nedap.university.protocol.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.protocol.FileStorageHeaderFlags.ACK;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PacketTest {
  private FileStoragePacketAssembler assembler;
  private FileStoragePacketDecoder decoder;
  private FileStorageServiceHandler serviceHandler;
  private FileStorageFileHandler fileHandler;
  public static final int PORT = 8080;
  public static final String HOSTNAME = "127.0.0.1";
  private InetAddress address;

  @BeforeEach
  public void setup() {
    try {
      address = InetAddress.getByName(HOSTNAME);
      decoder = new FileStoragePacketDecoder();
      serviceHandler = new FileStorageServiceHandler("./example_files");
      serviceHandler.setAddressAndPort(address, PORT);
      assembler = new FileStoragePacketAssembler(serviceHandler);
      fileHandler = new FileStorageFileHandler("./example_files");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void assemblerDecoderTest() {
    byte[] payload = "Ash nazg durbatuluk, ash nazg gimbatul, ash nazg thrakatuluk, agh burzum ishi krimpatul".getBytes();
    DatagramPacket packet = assembler.createPacket(payload, 1337, assembler.setFlags(Set.of(FINAL, ACK)));
    assertArrayEquals(payload, decoder.getPayload(packet));
    assertEquals(payload.length, decoder.getPayloadSize(packet));
    assertEquals(1337, decoder.getSequenceNumber(packet));
    assertTrue(decoder.hasFlag(packet, FINAL));
    assertTrue(decoder.hasFlag(packet, ACK));
    assertFalse(decoder.hasFlag(packet, SEND));
    assertFalse(decoder.hasFlag(packet, RETRIEVE));
    assertFalse(decoder.hasFlag(packet, ERROR));
    assertTrue(decoder.verifyChecksum(packet));
  }

  @Test
  public void ackPacketTest() {
    DatagramPacket packet = assembler.createAckPacket(1337, ACK);
    assertEquals(1337, decoder.getSequenceNumber(packet));
    assertTrue(decoder.hasFlag(packet, ACK));
    assertArrayEquals(new byte[1], decoder.getPayload(packet));
  }

  @Test
  public void createSendRequestTest() {
    try {
      long fileSize = Files.size(Paths.get("./example_files/large.pdf"));
      DatagramPacket packet = assembler.createRequestPacket(fileSize, "large.pdf".getBytes(), assembler.setFlags(SEND));
      assertEquals("large.pdf", decoder.getFileName(packet));
      assertEquals(fileSize, decoder.getFileSize(packet));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void setFlagsTest() {
    int flags = assembler.setFlags(Set.of(FINAL, ACK, SEND));
    assertEquals(21, flags);
    flags = assembler.setFlags(Set.of(FINAL, SEND));
    assertEquals(17, flags);
    flags = assembler.setFlags(new HashSet<>());
    assertEquals(0, flags);
  }
}
