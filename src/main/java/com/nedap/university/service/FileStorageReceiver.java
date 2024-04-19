package com.nedap.university.service;

import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.packet.FileStorageHeaderFlags.NACK;
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
      int NEF = LFR + 1;
      DatagramPacket packet = assembler.createBufferPacket(PACKET_SIZE);
      socket.receive(packet);

      int sequenceNumber = decoder.getSequenceNumber(packet);
      if (sequenceNumber == NEF) {
        receiveWindow.put(sequenceNumber, packet);
        for (int i = NEF; i <= LAF; i++) {
          if (receiveWindow.containsKey(i)) {
            ByteBuffer packetBuffer = ByteBuffer.wrap(decoder.getPayload(receiveWindow.get(i)));
            byteChannel.position((long) i * PAYLOAD_SIZE);
            byteChannel.write(packetBuffer);
            receiveWindow.remove(i);
            LFR = i;
            progress = FileStorageProgressBar.updateProgressBar(i, numOfPackets, progress);
            if (decoder.hasFlag(packet, FINAL)) {
              finalPacket = true;
            }

          } else {
            break;
          }
        }
        socket.send(assembler.createAckPacket(LFR, ACK));
      } else if (sequenceNumber > NEF){
        receiveWindow.put(sequenceNumber, packet);
        socket.send(assembler.createAckPacket(NEF, NACK));
      }
    }
    byteChannel.close();
    FileStorageProgressBar.finalizeProgressBar();
  }
}
