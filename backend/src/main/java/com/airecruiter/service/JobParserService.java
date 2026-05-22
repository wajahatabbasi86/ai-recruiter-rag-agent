package com.airecruiter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses job description files (PDF, DOCX, TXT) and splits them into chunks.
 *
 * Job descriptions tend to have different section names than resumes
 * (Requirements, Responsibilities, Benefits, etc.) so we use a separate
 * pattern list while sharing the same chunking strategy.
 */
@Slf4j
@Service
public class JobParserService {

    private final Tika tika = new Tika();

    private static final int MAX_CHUNK_SIZE = 800;

    private static final Pattern SECTION_PATTERN = Pattern.compile(
        "(?i)\\n\\s*(REQUIREMENTS|RESPONSIBILITIES|QUALIFICATIONS|BENEFITS|" +
        "ABOUT|OVERVIEW|SKILLS|EXPERIENCE|EDUCATION|WHAT YOU|WHO YOU|" +
        "NICE TO HAVE|MUST HAVE|ROLE|DUTIES|COMPENSATION)\\s*\\n",
        Pattern.MULTILINE
    );

    public String extractText(MultipartFile file) throws IOException, TikaException {
        log.debug("Parsing job description file: {}", file.getOriginalFilename());
        String text = tika.parseToString(file.getInputStream());
        log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
        return text;
    }

    /**
     * Also accepts plain text (e.g. pasted from a form).
     */
    public String extractText(String plainText) {
        return plainText;
    }

    public List<JobChunk> chunk(String rawText) {
        List<JobChunk> chunks = new ArrayList<>();

        String[] sections = SECTION_PATTERN.split(rawText);
        String[] headers = extractSectionHeaders(rawText);

        for (int i = 0; i < sections.length; i++) {
            String sectionText = sections[i].trim();
            String sectionName = (i < headers.length) ? headers[i] : "GENERAL";

            if (sectionText.isBlank()) continue;

            if (sectionText.length() <= MAX_CHUNK_SIZE) {
                chunks.add(new JobChunk(sectionName, sectionText));
            } else {
                String[] paragraphs = sectionText.split("\\n\\n+");
                for (String para : paragraphs) {
                    if (!para.isBlank()) {
                        chunks.add(new JobChunk(sectionName, para.trim()));
                    }
                }
            }
        }

        if (chunks.isEmpty()) {
            String[] paragraphs = rawText.split("\\n\\n+");
            for (String para : paragraphs) {
                if (!para.isBlank()) {
                    chunks.add(new JobChunk("GENERAL", para.trim()));
                }
            }
        }

        log.debug("Job description split into {} chunks", chunks.size());
        return chunks;
    }

    private String[] extractSectionHeaders(String text) {
        List<String> headers = new ArrayList<>();
        var matcher = SECTION_PATTERN.matcher(text);
        while (matcher.find()) {
            headers.add(matcher.group(1).toUpperCase());
        }
        return headers.toArray(new String[0]);
    }

    public record JobChunk(String section, String text) {
        public String toEmbeddingText() {
            return "Job section: " + section + "\n" + text;
        }
    }
}
