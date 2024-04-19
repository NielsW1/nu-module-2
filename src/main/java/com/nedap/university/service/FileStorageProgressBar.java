package com.nedap.university.service;

public class FileStorageProgressBar {

  public static void initProgressBar(long fileSize) {
    System.out.println("Sending " + fileSize + " bytes");
    System.out.println("|0%" + new String(new char[94]).replace("\0", " ") + "100%|");
    System.out.print("|");
  }

  public static int updateProgressBar(int sequenceNumber, int numOfPackets, int progress) {
    int curProgress = (int) ((double) sequenceNumber / numOfPackets * 100);
    if (curProgress > progress) {
      for (int p = 0; p < curProgress - progress; p++) {
        System.out.print("■");
      }
    }
    return curProgress;
  }

  public static void finalizeProgressBar() {
    System.out.print("|\n");
  }
}
