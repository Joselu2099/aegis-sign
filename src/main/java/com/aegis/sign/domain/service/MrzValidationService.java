package com.aegis.sign.domain.service;

import org.springframework.stereotype.Service;

/**
 * Service for MRZ (Machine Readable Zone) validation according to ICAO Doc 9303.
 */
@Service
public class MrzValidationService {

    /**
     * Validates MRZ checksum for a given field and its check digit.
     * 
     * @param field The MRZ field text.
     * @param checkDigit The expected check digit character.
     * @return true if valid, false otherwise.
     */
    public boolean validateChecksum(String field, char checkDigit) {
        if (field == null || !Character.isDigit(checkDigit)) {
            return false;
        }
        int expected = Character.getNumericValue(checkDigit);
        return calculateChecksum(field) == expected;
    }

    /**
     * Calculates the ICAO Doc 9303 checksum for a given string.
     * Weighting figures: 7, 3, 1 repeats.
     * Sum modulo 10.
     * 
     * @param input The MRZ string.
     * @return The calculated checksum (0-9).
     */
    public int calculateChecksum(String input) {
        int[] weights = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int value = getValue(c);
            sum += value * weights[i % 3];
        }
        return sum % 10;
    }

    private int getValue(char c) {
        if (c == '<') {
            return 0;
        }
        if (Character.isDigit(c)) {
            return c - '0';
        }
        if (Character.isLetter(c)) {
            return Character.toUpperCase(c) - 'A' + 10;
        }
        return 0;
    }
}
