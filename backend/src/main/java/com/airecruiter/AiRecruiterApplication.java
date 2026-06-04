package com.airecruiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the AI Recruiter RAG Agent application.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@SpringBootApplication
@EnableAsync
public class AiRecruiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRecruiterApplication.class, args);
    }
}