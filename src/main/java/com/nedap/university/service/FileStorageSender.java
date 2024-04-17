package com.nedap.university.service;

import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;

import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import com.nedap.university.packet.FileStoragePacketSender;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class FileStorageSender {
  int PAYLOAD_SIZE;
  int HEADER_SIZE;
  int WINDOW_SIZE;
  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;

  public FileStorageSender(FileStoragePacketAssembler assembler, FileStoragePacketDecoder decoder) {
    this.assembler = assembler;
    this.decoder = decoder;
    PAYLOAD_SIZE = FileStorageServiceHandler.PAYLOAD_SIZE;
    HEADER_SIZE = FileStorageServiceHandler.HEADER_SIZE;
    WINDOW_SIZE = FileStorageServiceHandler.WINDOW_SIZE;
  }

  public void sendFile(DatagramSocket socket, Path filePath) throws IOException {
    long fileSize = Files.size(filePath);
    HashMap<Integer, FileStoragePacketSender> sendWindow = new HashMap<>();
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
    ByteBuffer packetBuffer = ByteBuffer.allocate(PAYLOAD_SIZE);
    int bytesRead;
    int sequenceNumber = 1;
    int progress = (int) (sequenceNumber / (fileSize / PAYLOAD_SIZE));
    initProgressBar(fileSize);

    while ((bytesRead = byteChannel.read(packetBuffer)) != -1) {
      byte[] payload;
      DatagramPacket packet;
      if (bytesRead < PAYLOAD_SIZE) {
        payload = new byte[PAYLOAD_SIZE - packetBuffer.remaining()];
        System.arraycopy(packetBuffer.array(), 0, payload, 0, payload.length);
        packet = assembler.createPacket(payload, sequenceNumber,
            assembler.setFlags(FINAL));
      } else {
        payload = packetBuffer.array();
        packet = assembler.createPacket(payload, sequenceNumber, 0);
      }
      FileStoragePacketSender sender = new FileStoragePacketSender(socket, packet);
      sender.start();
      sendWindow.put(sequenceNumber++, sender);
      updateProgressBar(fileSize, sequenceNumber);

      if (sendWindow.size() >= WINDOW_SIZE) {
        socket.setSoTimeout(10);
        while (true) {
          DatagramPacket ackPacket = assembler.createBufferPacket(1);
          try {
            socket.receive(ackPacket);
            int ackNumber = decoder.getSequenceNumber(ackPacket);
            if (sendWindow.containsKey(ackNumber) && decoder.hasFlag(ackPacket, ACK)) {
              sendWindow.get(ackNumber).interrupt();
              sendWindow.remove(ackNumber);
            }
          } catch (SocketTimeoutException ignored) {
            break;
          }
        }
      }
    }
  }

  public void initProgressBar(long fileSize) {
    System.out.println("Sending..." + fileSize + " bytes");
    System.out.println("0\\%|%100s|100\\%");
  }

  public void updateProgressBar(long fileSize, int sequenceNumber) {
    int progress = (int) (sequenceNumber / (fileSize / PAYLOAD_SIZE));
  }
}
