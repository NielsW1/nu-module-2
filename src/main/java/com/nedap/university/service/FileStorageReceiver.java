package com.nedap.university.service;

import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.service.FileStorageServiceHandler.PAYLOAD_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.HEADER_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.PACKET_SIZE;
import static com.nedap.university.service.FileStorageServiceHandler.WINDOW_SIZE;
import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class FileStorageReceiver {
  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;

  public FileStorageReceiver(FileStoragePacketAssembler assembler, FileStoragePacketDecoder decoder) {
    this.assembler = assembler;
    this.decoder = decoder;
  }

  public void receiveFile(DatagramSocket socket, Path filePath, long fileSize) throws IOException {
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath,
        Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
    HashMap<Integer, DatagramPacket> receiveWindow = new HashMap<>();
    int LFR = 0;
    boolean finalPacket = false;
    int numOfPackets = (int) (fileSize / PAYLOAD_SIZE);
    int progress = 0;
    FileStorageProgressBar.initProgressBar(fileSize);

    while (!finalPacket) {
      int LAF = LFR + WINDOW_SIZE;
      DatagramPacket packet = assembler.createBufferPacket(PACKET_SIZE);
      socket.receive(packet);

      int sequenceNumber = decoder.getSequenceNumber(packet);
      if (sequenceNumber > LFR && sequenceNumber <= LAF) {
        receiveWindow.put(sequenceNumber, packet);
        for (int i = LFR + 1; i <= LAF; i++) {
          if (receiveWindow.containsKey(i)) {
            ByteBuffer packetBuffer = ByteBuffer.wrap(decoder.getPayload(packet));
            byteChannel.position((long) sequenceNumber * PAYLOAD_SIZE);
            byteChannel.write(packetBuffer);
            if (decoder.hasFlag(receiveWindow.get(i), FINAL)) {
              finalPacket = true;
              byteChannel.close();
              socket.send(assembler.createAckPacket(i));
            }
            receiveWindow.remove(i);
            LFR = i;
            progress = FileStorageProgressBar.updateProgressBar(sequenceNumber, numOfPackets,
                progress);
          } else {
            socket.send(assembler.createAckPacket(LFR));
            System.out.println("Sending ack: " + LFR);
            break;
          }
        }
      }
    }
    FileStorageProgressBar.finalizeProgressBar();
  }
}
