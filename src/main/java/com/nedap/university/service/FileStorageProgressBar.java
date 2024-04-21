package com.nedap.university.service;

import static com.nedap.university.service.FileStorageServiceHandler.PAYLOAD_SIZE;

import java.util.Collections;

public class FileStorageProgressBar {

  public static void updateProgressBar(int sequenceNumber, int numOfPackets, long startTime) {
    int percentage = (sequenceNumber * 100 / numOfPackets);
    long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
    float downloadSpeed = timeElapsed == 0 ? 0
        : (float) (((long) sequenceNumber * PAYLOAD_SIZE) / timeElapsed) / 1000;
    StringBuilder progressBar = new StringBuilder(125);
    progressBar
        .append("\r")
        .append(String.format("%d%% |", percentage))
        .append(String.join("", Collections.nCopies(percentage, "■")))
        .append(String.join("", Collections.nCopies(100 - percentage, " ")))
        .append(downloadSpeed > 1000 ? String.format("| %.2f MB/sec", downloadSpeed / 1000) :
            String.format("| %.2f KB/sec", downloadSpeed));
    System.out.print(progressBar);
  }
}
