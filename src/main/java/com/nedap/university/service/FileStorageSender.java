package com.nedap.university.service;

import static com.nedap.university.protocol.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.protocol.FileStorageHeaderFlags.NACK;
import static com.nedap.university.service.FileStorageServiceHandler.HEADER_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.PAYLOAD_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.WINDOW_SIZE;

import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.protocol.FileStoragePacketDecoder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

  /**
   * Reads a file using a SeekableByteChannel and creates DatagramPackets to send.
   * LAR: Last Acknowledgement Received
   * @throws IOException
   */

  public void sendFile(DatagramSocket socket, Path filePath, long fileSize) throws IOException {
    int bytesRead;
    int sequenceNumber = 1;
    int LAR = 0;
    int progress = 0;
    int numOfPackets = (int) (fileSize / PAYLOAD_SIZE);
    int LPR = 0;
    boolean finalPacketSent = false;
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
    ByteBuffer packetBuffer = ByteBuffer.allocate(PAYLOAD_SIZE);
    HashMap<Integer, DatagramPacket> sendWindow = new HashMap<>();

    FileStorageProgressBar.initProgressBar(fileSize);

    while ((bytesRead = byteChannel.read(packetBuffer)) != -1) {
      byte[] payload;
      DatagramPacket packet;
      packetBuffer.flip();

      //create packets from bytes read from the file and send them
      if (bytesRead < PAYLOAD_SIZE) {
        payload = new byte[PAYLOAD_SIZE - packetBuffer.remaining()];
        System.arraycopy(packetBuffer.array(), 0, payload, 0, payload.length);
        packet = assembler.createPacket(payload, sequenceNumber,
            assembler.setFlags(FINAL));
        finalPacketSent = true;
      } else {
        packet = assembler.createPacket(packetBuffer.array(), sequenceNumber, 0);
      }
      socket.send(packet);
      sendWindow.put(sequenceNumber, packet);
      sequenceNumber++;
      packetBuffer.clear();

      progress = FileStorageProgressBar.updateProgressBar(sequenceNumber, numOfPackets,
          progress);

      //if sendWindow is full or all packets have been sent, receive acknowledgements and
      //remove them from the sendWindow
      while (sendWindow.size() >= WINDOW_SIZE || (finalPacketSent && !sendWindow.isEmpty())) {
        DatagramPacket ackPacket = assembler.createBufferPacket(HEADER_SIZE);
        socket.receive(ackPacket);
        int ackNumber = decoder.getSequenceNumber(ackPacket);
        if (decoder.hasFlag(ackPacket, NACK) && LPR != ackNumber) {
          socket.send(sendWindow.get(ackNumber));
        } else {
          for (int i = LAR + 1; i <= ackNumber; i++) {
            if (sendWindow.containsKey(i)) {
              sendWindow.remove(i);
              LAR = i;
            } else {
              break;
            }
          }
        }
      }
    }
    FileStorageProgressBar.finalizeProgressBar();
  }
}

