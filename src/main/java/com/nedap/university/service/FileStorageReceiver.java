package com.nedap.university.service;

import com.nedap.university.packet.FileStoragePacketAssembler;
import com.nedap.university.packet.FileStoragePacketDecoder;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class FileStorageReceiver {
  int PAYLOAD_SIZE;
  int HEADER_SIZE;
  int WINDOW_SIZE;
  private final FileStoragePacketAssembler assembler;
  private final FileStoragePacketDecoder decoder;

  public FileStorageReceiver(FileStoragePacketAssembler assembler, FileStoragePacketDecoder decoder) {
    this.assembler = assembler;
    this.decoder = decoder;
    PAYLOAD_SIZE = FileStorageServiceHandler.PAYLOAD_SIZE;
    HEADER_SIZE = FileStorageServiceHandler.HEADER_SIZE;
    WINDOW_SIZE = FileStorageServiceHandler.WINDOW_SIZE;
  }

  public String receiveFile(DatagramSocket socket, Path filePath, long fileSize) throws IOException {
    SeekableByteChannel byteChannel = Files.newByteChannel(filePath,
        Set.of(StandardOpenOption.APPEND, StandardOpenOption.CREATE));
    ByteBuffer packetBuffer = ByteBuffer.allocate(PAYLOAD_SIZE);
    boolean finalPacket = false;

    while (!finalPacket) {

    }
    return "placeholder";
  }

}
