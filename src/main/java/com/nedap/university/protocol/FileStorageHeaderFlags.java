package com.nedap.university.protocol;

/**
 * Flags used in the custom transport header.
 * ERROR: Denotes occurrence of an error. The actual error will be in the payload (0 = false, 1 = true)
 * FINAL: Denotes final packet of file (0 = false, 1 = true)
 * NACK: Used for negative acknowledgement packets (0 = false, 1 = true)
 * ACK: Used for acknowledgement packets (0 = false, 1 = true)
 * RETRIEVE: Initiates a file retrieve (from server) request (0 = false, 1 = true)
 * SEND: Initiates a file send (to server) request (0 = false, 1 = true)
 */

public enum FileStorageHeaderFlags {
  ERROR, FINAL, NACK, ACK, RETRIEVE, SEND
}

