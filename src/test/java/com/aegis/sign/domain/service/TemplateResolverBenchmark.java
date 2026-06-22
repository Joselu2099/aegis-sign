package com.aegis.sign.domain.service;

public class TemplateResolverBenchmark {
    public static void main(String[] args) {
        TemplateResolver resolver = new TemplateResolver();

        // Warmup
        for (int i = 0; i < 10000; i++) {
            try {
                resolver.resolve("sample-contract");
            } catch (Exception e) {}
        }

        long start = System.nanoTime();
        int iterations = 100000;
        for (int i = 0; i < iterations; i++) {
            resolver.resolve("sample-contract");
        }
        long end = System.nanoTime();

        double avgTimeNs = (double)(end - start) / iterations;
        System.out.println("Average time per resolve (ns): " + avgTimeNs);
    }
}
