package com.nedap.university.packet;

/**
 * Flags used in the custom packet header.
 * ERROR: Set to 1 whenever an error occurs. The actual error will be the payload
 * FINAL: Denotes final packet of file (0 = false, 1 = true)
 * ACK: Used for acknowledgement packets (0 = false, 1 = true)
 * MODE: Used by the server to differentiate between send and retrieve mode (0 = send, 1 = retrieve)
 */
public enum FileStorageHeaderFlags {
  ERROR, FINAL, ACK, RETRIEVE, SEND
}


