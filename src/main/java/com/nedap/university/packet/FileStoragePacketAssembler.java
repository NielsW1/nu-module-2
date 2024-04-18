package com.nedap.university.packet;

import static com.nedap.university.packet.FileStorageHeaderFlags.ACK;
import static com.nedap.university.packet.FileStorageHeaderFlags.ERROR;
import static com.nedap.university.packet.FileStorageHeaderFlags.FINAL;
import static com.nedap.university.packet.FileStorageHeaderFlags.RETRIEVE;
import static com.nedap.university.packet.FileStorageHeaderFlags.SEND;

import com.nedap.university.service.FileStorageServiceHandler;
import java.net.DatagramPacket;
import java.util.Set;

public class FileStoragePacketAssembler {
  int PAYLOAD_SIZE;
  int HEADER_SIZE;

  private FileStorageServiceHandler serviceHandler;

  public FileStoragePacketAssembler(FileStorageServiceHandler serviceHandler) {
    PAYLOAD_SIZE = FileStorageServiceHandler.PAYLOAD_SIZE;
    HEADER_SIZE = FileStorageServiceHandler.HEADER_SIZE;
    this.serviceHandler = serviceHandler;
  }

  public DatagramPacket createPacket(byte[] payload, int sequenceNumber, int flags) {
    byte[] packet = addPacketHeader(payload, sequenceNumber, flags, payload.length);
    return new DatagramPacket(packet, packet.length, serviceHandler.getAddress(),
        serviceHandler.getPort());
  }

  public DatagramPacket createAckPacket(int sequenceNumber) {
    return createPacket(new byte[1], sequenceNumber, setFlags(ACK));
  }

  public DatagramPacket createSendFilePacket(long fileSize, byte[] fileName, int flags) {
    byte[] fileSizeArray = getFileSizeByteArray(fileSize);
    byte[] packetWithFileSize = new byte[fileName.length + 8];
    System.arraycopy(fileSizeArray, 0, packetWithFileSize, 0, fileSizeArray.length);
    System.arraycopy(fileName, 0, packetWithFileSize, 8, fileName.length);
    return createPacket(packetWithFileSize, 0, flags);
  }

  public DatagramPacket createBufferPacket(int bufferSize) {
    return new DatagramPacket(new byte[bufferSize], bufferSize);
  }

  public byte[] addPacketHeader(byte[] packet, int sequenceNumber, int flags, int payloadSize) {
    byte[] packetWithHeader = new byte[packet.length + HEADER_SIZE];

    packetWithHeader[0] = (byte) (sequenceNumber >> 24 & 0xff);
    packetWithHeader[1] = (byte) (sequenceNumber >> 16 & 0xff);
    packetWithHeader[2] = (byte) (sequenceNumber >> 8 & 0xff);
    packetWithHeader[3] = (byte) (sequenceNumber & 0xff);
    packetWithHeader[4] = (byte) (payloadSize >> 8 & 0xff);
    packetWithHeader[5] = (byte) (payloadSize & 0xff);
    packetWithHeader[6] = (byte) flags;
    System.arraycopy(packet, 0, packetWithHeader, HEADER_SIZE, packet.length);
    return packetWithHeader;
  }

  public int setFlags(FileStorageHeaderFlags flag) {
    return setFlags(Set.of(flag));
  }

  public int setFlags(Set<FileStorageHeaderFlags> flags) {
    int flagByte = 0;

    for (FileStorageHeaderFlags flag : flags) {
      if (flag == ERROR) {
        flagByte |= 1 << 4;
      }
      if (flag == FINAL) {
        flagByte |= 1 << 3;
      }
      if (flag == ACK) {
        flagByte |= 1 << 2;
      }
      if (flag == RETRIEVE) {
        flagByte |= 1 << 1;
      }
      if (flag == SEND) {
        flagByte |= 1;
      }
    }
    return flagByte;
  }

  public byte[] getFileSizeByteArray(long fileSize) {
    byte[] fileSizeArray = new byte[8];
    for (int i = 1; i < 9; i++) {
      fileSizeArray[i - 1] = (byte) (((fileSize) >>> ((8 - i) * 8)) & 0xff);
    }
    return fileSizeArray;
  }

}
