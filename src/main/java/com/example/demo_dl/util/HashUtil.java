package com.example.demo_dl.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * Generate SHA-256 hash from input string
     * Based on CmdFileHash.getHash() from hellodl sample
     *
     * @param input the input string to hash
     * @return hex-encoded SHA-256 hash
     */
    public static String generateSha256Hash(String input) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = sha256.digest(input.getBytes());
            return String.format("%064x", new BigInteger(1, hashBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
