package com.nedap.university.service;

import static com.nedap.university.protocol.FileStorageHeaderFlags.ACK;
import static com.nedap.university.protocol.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.protocol.FileStorageHeaderFlags.NACK;
import static com.nedap.university.service.FileStorageServiceHandler.PACKET_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.PAYLOAD_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.WINDOW_SIZE;

import com.nedap.university.protocol.FileStoragePacketAssembler;
import com.nedap.university.protocol.FileStoragePacketDecoder;
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
import java.util.Set;
import java.util.Timer;

public class FileStorageReceiver {

  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;

  public FileStorageReceiver(FileStoragePacketAssembler assembler,
      FileStoragePacketDecoder decoder) {
    this.assembler = assembler;
    this.decoder = decoder;
  }

  /**
   * Receives DatagramPackets and writes them to a file using a SeekableByteChannel.
   * Some abbreviations:
   * LFR: Last Frame Received
   * LAF: Largest Accepted Frame
   * NEF: Next Expected Frame
   */

  public void receiveFile(DatagramSocket socket, Path filePath, long fileSize, boolean log)
      throws IOException {
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath,
        Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    HashMap<Integer, DatagramPacket> receiveWindow = new HashMap<>();
    int LFR = 0;
    int timeOutCounter = 0;
    boolean finalPacket = false;
    int numOfPackets = (int) (fileSize / PAYLOAD_SIZE);
    int retransmits = 0;
    long startTime = System.currentTimeMillis();
    Timer timer = new Timer();
    FileStorageTimeOut timeout = null;

    while (!finalPacket || !receiveWindow.isEmpty()) {
      int LAF = LFR + WINDOW_SIZE;
      int NEF = LFR + 1;

      socket.setSoTimeout(100);
      try {
        DatagramPacket packet = assembler.createBufferPacket(PACKET_SIZE);
        socket.receive(packet);
        timeOutCounter = 0;

        int sequenceNumber = decoder.getSequenceNumber(packet);
        if (!decoder.verifyChecksum(packet)) {
          socket.send(assembler.createAckPacket(sequenceNumber, NACK));
          continue;
        }
        if (receiveWindow.containsKey(sequenceNumber)) {
          socket.send(assembler.createAckPacket(LFR, ACK));
          continue;
        }
        if (sequenceNumber == NEF) {
          if (timeout != null) {
            timeout.cancel();
            timeout = null;
          }
          receiveWindow.put(sequenceNumber, packet);
          for (int i = NEF; i <= LAF; i++) {
            if (receiveWindow.containsKey(i)) {
              if (decoder.hasFlag(receiveWindow.get(i), FINAL)) {
                finalPacket = true;
              }
              ByteBuffer packetBuffer = ByteBuffer.wrap(decoder.getPayload(receiveWindow.get(i)));
              byteChannel.write(packetBuffer);
              receiveWindow.remove(i);
              LFR = i;

              if (!log) {
                FileStorageProgressBar.updateProgressBar(i, numOfPackets, startTime, retransmits);
              }

            } else {
              break;
            }
          }
          socket.send(assembler.createAckPacket(LFR, ACK));

        } else if (sequenceNumber > NEF && sequenceNumber <= LAF) {
          receiveWindow.put(sequenceNumber, packet);
          if (timeout == null) {
            timeout = new FileStorageTimeOut(socket, assembler.createAckPacket(NEF, NACK));
            timer.scheduleAtFixedRate(timeout, 0, 1000);
          }
        }
      } catch (SocketTimeoutException e) {
        socket.send(assembler.createAckPacket(NEF, NACK));
        retransmits++;
        if (timeOutCounter++ > 100) {
          if (timeout != null) {
            timeout.cancel();
          }
          throw new IOException("Receive timed out");
        }
      }
    }
    System.out.println();
    socket.setSoTimeout(0);
    byteChannel.close();
  }
}