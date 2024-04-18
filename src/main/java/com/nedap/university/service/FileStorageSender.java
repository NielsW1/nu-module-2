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

  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;

  public FileStorageSender(FileStoragePacketAssembler assembler, FileStoragePacketDecoder decoder) {
    this.assembler = assembler;
    this.decoder = decoder;
  }

  public void sendFile(DatagramSocket socket, Path filePath, int PAYLOAD_SIZE, int WINDOW_SIZE,
      long fileSize) throws IOException {
    HashMap<Integer, FileStoragePacketSender> sendWindow = new HashMap<>();
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
    ByteBuffer packetBuffer = ByteBuffer.allocate(PAYLOAD_SIZE);
    int bytesRead;
    int sequenceNumber = 1;
    int progress = 0;
    initProgressBar(fileSize);

    while ((bytesRead = byteChannel.read(packetBuffer)) != -1) {
      byte[] payload;
      DatagramPacket packet;
      packetBuffer.flip();
      if (bytesRead < PAYLOAD_SIZE) {
        payload = new byte[PAYLOAD_SIZE - packetBuffer.remaining()];
        System.arraycopy(packetBuffer.array(), 0, payload, 0, payload.length);
        packet = assembler.createPacket(payload, sequenceNumber,
            assembler.setFlags(FINAL));
      } else {
        packet = assembler.createPacket(packetBuffer.array(), sequenceNumber, 0);
      }
      FileStoragePacketSender sender = new FileStoragePacketSender(socket, packet);
      sender.start();
      sendWindow.put(sequenceNumber, sender);
      int curProgress = (int) ((sequenceNumber++ / (fileSize / PAYLOAD_SIZE)) * 100);
      if (curProgress > progress) {
        updateProgressBar();
        progress = curProgress;
      }
      packetBuffer.clear();

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
    finalizeProgressBar();
  }

  public void initProgressBar(long fileSize) {
    System.out.println("Sending..." + fileSize + " bytes");
    System.out.println("|0%" + new String(new char[94]).replace("\0", " ") + "100%|");
    System.out.print("|");
  }

  public void updateProgressBar() {
    System.out.print("■");
  }

  public void finalizeProgressBar() {
    System.out.print("|\n");
  }
}
