package com.aegis.sign.domain.service;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

public class BenchmarkOcrTest {

    @Test
    public void benchmarkOcr() throws Exception {
        OcrExtractorService service = new OcrExtractorService(null, null);
        Method parseFieldsMethod = OcrExtractorService.class.getDeclaredMethod("parseFields", String.class, Map.class);
        parseFieldsMethod.setAccessible(true);

        String sampleText = "P<FRAOUEST<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                            "1234567890FRA9001011M2512314<<<<<<<<<<<<<<00\n" +
                            "Some other text to parse L898902C3 950101 301231";

        // Warmup
        for (int i = 0; i < 10000; i++) {
            Map<String, String> fields = new HashMap<>();
            parseFieldsMethod.invoke(service, sampleText, fields);
        }

        int iterations = 100000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Map<String, String> fields = new HashMap<>();
            parseFieldsMethod.invoke(service, sampleText, fields);
        }
        long end = System.nanoTime();

        System.out.println("====== BENCHMARK START ======");
        System.out.println("Time taken for " + iterations + " iterations: " + (end - start) / 1_000_000.0 + " ms");
        System.out.println("====== BENCHMARK END ======");
    }
}
