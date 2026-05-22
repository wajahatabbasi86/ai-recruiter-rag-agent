package com.airecruiter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Bias detection and mitigation layer.
 *
 * This is intentionally rule-based (not ML) for transparency and auditability.
 * Regulators prefer explainable rule-based filters over black-box ML bias detection.
 *
 * What this does:
 * - Detects if query contains protected characteristic filters (gender, age, race...)
 * - Strips protected attributes from AI-generated output text
 * - Logs bias detection events for audit trail
 *
 * What this does NOT do:
 * - It does not guarantee zero bias in model outputs (no system can)
 * - It is not a substitute for formal bias audits (required for EU AI Act)
 */
@Slf4j
@Service
public class BiasCheckerService {

    // Protected characteristics — queries containing these are flagged
    private static final List<Pattern> PROTECTED_QUERY_PATTERNS = List.of(
        Pattern.compile("(?i)\\b(male|female|man|woman|gender|he|she)\\b"),
        Pattern.compile("(?i)\\b(age|years old|young|old|senior citizen|millennial)\\b"),
        Pattern.compile("(?i)\\b(race|ethnicity|black|white|asian|hispanic|latino)\\b"),
        Pattern.compile("(?i)\\b(religion|muslim|christian|hindu|jewish)\\b"),
        Pattern.compile("(?i)\\b(nationality|citizen|immigrant|visa)\\b"),
        Pattern.compile("(?i)\\b(pregnant|disability|disabled|married|single)\\b")
    );

    // Patterns to strip from AI-generated output before returning to recruiter
    private static final List<Pattern> OUTPUT_STRIP_PATTERNS = List.of(
        Pattern.compile("(?i)(he|she|his|her|they)\\s+is\\s+a\\s+\\w+"),
        Pattern.compile("(?i)(candidate is|applicant is)\\s+(young|old|male|female|\\d+ years old)"),
        Pattern.compile("(?i)\\b(born in|age:|nationality:|citizenship:).*?(?=\\n|$)")
    );

    /**
     * Checks if a recruiter query contains protected characteristic filters.
     * Returns true if bias risk detected.
     */
    public boolean detectBiasInQuery(String query) {
        for (Pattern pattern : PROTECTED_QUERY_PATTERNS) {
            if (pattern.matcher(query).find()) {
                log.warn("Potential bias detected in query: protected characteristic pattern found");
                return true;
            }
        }
        return false;
    }

    /**
     * Strips protected characteristic mentions from AI-generated output.
     * Applied before sending response to recruiter.
     */
    public String sanitizeOutput(String aiOutput) {
        String sanitized = aiOutput;
        for (Pattern pattern : OUTPUT_STRIP_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
        }
        return sanitized;
    }

    /**
     * Generates a bias warning message to include in the API response
     * when a biased query is detected.
     */
    public String getBiasWarning() {
        return "Warning: Your query may reference protected characteristics. " +
               "Results have been filtered to focus on skills and experience only. " +
               "All AI-assisted decisions should be reviewed by a human recruiter.";
    }
}
