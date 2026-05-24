package com.aegis.sign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AegisSignApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisSignApplication.class, args);
    }
}
