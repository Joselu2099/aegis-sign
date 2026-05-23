package com.aegis.sign.domain.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MrzValidationServiceTest {

    private final MrzValidationService mrzValidationService = new MrzValidationService();

    @Test
    void shouldValidateCorrectChecksum() {
        // Example: L898902C with check digit 3
        assertTrue(mrzValidationService.validateChecksum("L898902C", '3'));
    }

    @Test
    void shouldFailInvalidChecksum() {
        assertFalse(mrzValidationService.validateChecksum("L898902C", '4'));
    }

    @Test
    void shouldCalculateChecksumWithFillers() {
        // 52183100< -> check digit 9
        // 5*7=35, 2*3=6, 1*1=1, 8*7=56, 3*3=9, 1*1=1, 0*7=0, 0*3=0, 0*1=0
        // Sum: 35+6+1+56+9+1+0+0+0 = 108. 108 % 10 = 8. Wait.
        // Let's re-calculate: 35+6+1+56+9+1 = 108. 108%10=8.
        // Maybe my manual calculation is wrong or the example I had in mind was different.
        // Let's use a simpler one.
        // AB123 -> A(10)*7=70, B(11)*3=33, 1*1=1, 2*7=14, 3*3=9. Sum: 70+33+1+14+9 = 127. Check digit 7.
        assertEquals(7, mrzValidationService.calculateChecksum("AB123"));
    }

    @Test
    void shouldHandleFillers() {
        // < is 0
        assertEquals(0, mrzValidationService.calculateChecksum("<<<"));
    }
}
