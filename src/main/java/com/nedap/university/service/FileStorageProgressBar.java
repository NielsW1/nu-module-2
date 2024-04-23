package com.nedap.university.service;

import static com.nedap.university.service.FileStorageServiceHandler.PAYLOAD_SIZE;

import java.sql.Time;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class FileStorageProgressBar {

  public static void updateProgressBar(int sequenceNumber, int numOfPackets, long startTime) {
    int percentage = (sequenceNumber * 100 / numOfPackets);
    long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
    float downloadSpeed = timeElapsed == 0 ? 0
        : (float) (((long) sequenceNumber * PAYLOAD_SIZE) / timeElapsed) / 1000;
    String progressBar = "\r"
        + String.format("%d%% [", percentage)
        + String.join("", Collections.nCopies(percentage, "■"))
        + String.join("", Collections.nCopies(100 - percentage, " "))
        + (downloadSpeed > 1000 ? String.format("] %.2f MB/sec", downloadSpeed / 1000) :
        String.format("] %.2f KB/sec", downloadSpeed))
        + String.format(" %02d:%02d:%02d", timeElapsed / 3600, timeElapsed % 3600 / 60,
        timeElapsed % 3600 % 60);
    System.out.print(progressBar);
  }
}
