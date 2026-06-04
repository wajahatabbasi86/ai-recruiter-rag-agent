package com.airecruiter.service;

import com.airecruiter.dto.ResumeMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for extracting text from uploaded files and parsing structured metadata using LLM.
 * Uses Apache Tika for file parsing and Ollama for LLM-based extraction.
 *
 * @author AI Recruiter Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExtractionService {

    private final ChatModel chatModel;

    /**
     * Extracts raw text from an uploaded file (PDF, DOCX, TXT) using Apache Tika.
     *
     * @param file the uploaded file
     * @return extracted plain text
     * @throws IOException   if file reading fails
     * @throws TikaException if Tika parsing fails
     */
    public String extractTextFromFile(MultipartFile file) throws IOException, TikaException {
        log.debug("Extracting text from file: {}", file.getOriginalFilename());

        BodyContentHandler handler = new BodyContentHandler(-1); // -1 = no limit
        Metadata metadata = new Metadata();
        AutoDetectParser parser = new AutoDetectParser();

        try (InputStream inputStream = file.getInputStream()) {
            parser.parse(inputStream, handler, metadata);
        } catch (SAXException saxException) {
            throw new TikaException("SAX parsing error: " + saxException.getMessage(), saxException);
        }

        String extractedText = handler.toString().trim();
        log.debug("Extracted {} characters from file: {}", extractedText.length(), file.getOriginalFilename());

        return extractedText;
    }

    /**
     * Extracts structured metadata from raw resume text using Ollama LLM.
     * Returns a best-effort DTO; falls back gracefully on LLM errors.
     *
     * @param rawResumeText the raw resume text
     * @return structured resume metadata DTO
     */
    public ResumeMetadataDto extractStructuredMetadata(String rawResumeText) {
        log.debug("Extracting structured metadata from resume text of {} chars", rawResumeText.length());

        String prompt = buildExtractionPrompt(rawResumeText);

        try {
            String llmResponse = chatModel.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getText();

            return parseJsonResponse(llmResponse);

        } catch (Exception exception) {
            log.error("LLM extraction failed, returning empty metadata: {}", exception.getMessage());
            return ResumeMetadataDto.builder().build();
        }
    }

    /**
     * Builds the LLM prompt for structured extraction.
     *
     * @param resumeText the resume text
     * @return prompt string
     */
    private String buildExtractionPrompt(String resumeText) {
        return """
                Extract structured information from the following resume text and return ONLY valid JSON.
                Do not include any explanation or markdown — return raw JSON only.

                JSON schema:
                {
                  "extractedEmail": "string or null",
                  "extractedPhone": "string or null",
                  "linkedInProfileUrl": "string or null",
                  "technicalSkills": "comma-separated skills or null",
                  "totalExperienceYears": integer or null,
                  "previousCompanies": "comma-separated company names or null",
                  "jobTitlesHeld": "comma-separated job titles or null",
                  "educationLevel": "highest degree e.g. Bachelor, Master, PhD or null",
                  "universityName": "string or null",
                  "fieldsOfStudy": "comma-separated fields or null",
                  "currentLocation": "city, country or null",
                  "country": "country name or null",
                  "willingToRelocate": boolean or null,
                  "tools": "comma-separated tools or null",
                  "frameworks": "comma-separated frameworks or null",
                  "databases": "comma-separated databases or null",
                  "latestCompanyName": "string or null",
                  "latestJobTitle": "string or null",
                  "latestRoleEndDate": "Present or date string or null",
                  "currentEmploymentStatus": "Employed or Unemployed or null"
                }

                Resume text:
                """ + resumeText;
    }

    /**
     * Parses the LLM JSON response into a ResumeMetadataDto.
     * Falls back gracefully if JSON is malformed.
     *
     * @param jsonResponse the raw LLM JSON string
     * @return populated DTO
     */
    private ResumeMetadataDto parseJsonResponse(String jsonResponse) {
        try {
            // Strip markdown fences if present
            String cleaned = jsonResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cleaned, ResumeMetadataDto.class);

        } catch (Exception exception) {
            log.warn("Could not parse LLM JSON response, returning empty metadata. Response: {}",
                    jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            return ResumeMetadataDto.builder().build();
        }
    }
}