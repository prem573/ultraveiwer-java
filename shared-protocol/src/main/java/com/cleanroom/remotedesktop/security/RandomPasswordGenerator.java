package com.cleanroom.remotedesktop.security;

import java.security.SecureRandom;

public final class RandomPasswordGenerator {
    // Exclude confusing characters like 0, O, 1, l, I
    private static final char[] ALPHABET = "23456789abcdefghijkmnpqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomPasswordGenerator() {}

    public static String generatePassword() {
        return generatePassword(4);
    }

    public static String generatePassword(int length) {
        if (length < 4) length = 4;
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(result);
    }

    public static String generateRandomHostId() {
        // Generate a 9-digit number between 100 000 000 and 999 999 999
        int id = 100_000_000 + RANDOM.nextInt(900_000_000);
        return String.valueOf(id);
    }

    public static String formatHostId(String rawId) {
        if (rawId == null) return "";
        String digits = rawId.replaceAll("\\D", "");
        if (digits.length() == 9) {
            return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6, 9);
        }
        return rawId;
    }
}

