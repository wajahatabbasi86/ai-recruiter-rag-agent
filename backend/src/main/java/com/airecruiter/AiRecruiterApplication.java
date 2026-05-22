package com.airecruiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync   // For non-blocking embedding generation
public class AiRecruiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRecruiterApplication.class, args);
    }
}
