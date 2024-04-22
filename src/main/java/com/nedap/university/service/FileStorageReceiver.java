package com.nedap.university.service;

import static com.nedap.university.protocol.FileStorageHeaderFlags.ACK;
import static com.nedap.university.protocol.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.protocol.FileStorageHeaderFlags.NACK;
import static com.nedap.university.service.FileStorageServiceHandler.PAYLOAD_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.PACKET_SIZE;
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
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class FileStorageReceiver {
  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;

  public FileStorageReceiver(FileStoragePacketAssembler assembler, FileStoragePacketDecoder decoder) {
    this.assembler = assembler;
    this.decoder = decoder;
  }

  /**
   * Receives DatagramPackets and writes them to a file using a SeekableByteChannel.
   * Some abbreviations:
   * LFR: Last Frame Received
   * LAF: Largest Accepted Frame
   * NEF: Next Expected Frame
   * @throws IOException
   */

  public void receiveFile(DatagramSocket socket, Path filePath, long fileSize) throws IOException {
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath,
        Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    HashMap<Integer, DatagramPacket> receiveWindow = new HashMap<>();
    int LFR = 0;
    boolean finalPacket = false;
    int numOfPackets = (int) (fileSize / PAYLOAD_SIZE);
    long startTime = System.currentTimeMillis();
    Timer timer = new Timer();
    FileStorageTimeOut timeout = null;

    while (!finalPacket) {
      int LAF = LFR + WINDOW_SIZE;
      int NEF = LFR + 1;
      DatagramPacket packet = assembler.createBufferPacket(PACKET_SIZE);
      socket.receive(packet);

      int sequenceNumber = decoder.getSequenceNumber(packet);
      if (!decoder.verifyChecksum(packet)) {
        socket.send(assembler.createAckPacket(sequenceNumber, NACK));
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

            FileStorageProgressBar.updateProgressBar(i, numOfPackets, startTime);
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
    }
    System.out.println();
    byteChannel.close();
  }
}