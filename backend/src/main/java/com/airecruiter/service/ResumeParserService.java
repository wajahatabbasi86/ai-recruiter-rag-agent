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
 * Parses resume files (PDF, DOCX, TXT) and splits them into semantic chunks.
 *
 * WHY CHUNKING MATTERS:
 * Embedding an entire 3-page resume as one vector loses precision.
 * A query for "Python skills" should match the Skills section specifically,
 * not get diluted by the candidate's education section.
 *
 * Strategy: Split by section headers, then by paragraph if sections are large.
 */
@Slf4j
@Service
public class ResumeParserService {

    private final Tika tika = new Tika();

    // Max characters per chunk before we split further
    private static final int MAX_CHUNK_SIZE = 800;

    // Section header patterns commonly found in resumes
    private static final Pattern SECTION_PATTERN = Pattern.compile(
        "(?i)\\n\\s*(EXPERIENCE|EDUCATION|SKILLS|SUMMARY|OBJECTIVE|" +
        "PROJECTS|CERTIFICATIONS|ACHIEVEMENTS|LANGUAGES|CONTACT)\\s*\\n",
        Pattern.MULTILINE
    );

    /**
     * Extracts raw text from uploaded file using Apache Tika.
     * Supports: PDF, DOCX, DOC, TXT, RTF
     */
    public String extractText(MultipartFile file) throws IOException, TikaException {
        log.debug("Parsing file: {} ({})", file.getOriginalFilename(), file.getContentType());
        String text = tika.parseToString(file.getInputStream());
        log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
        return text;
    }

    /**
     * Splits resume text into meaningful chunks for embedding.
     *
     * Each chunk gets its own vector in Qdrant so queries match
     * the most relevant section, not just the best overall resume.
     *
     * Returns list of (sectionName, chunkText) pairs.
     */
    public List<ResumeChunk> chunk(String rawText) {
        List<ResumeChunk> chunks = new ArrayList<>();

        // Split by section headers
        String[] sections = SECTION_PATTERN.split(rawText);
        String[] headers = extractSectionHeaders(rawText);

        for (int i = 0; i < sections.length; i++) {
            String sectionText = sections[i].trim();
            String sectionName = (i < headers.length) ? headers[i] : "GENERAL";

            if (sectionText.isBlank()) continue;

            // If section is small enough, keep as one chunk
            if (sectionText.length() <= MAX_CHUNK_SIZE) {
                chunks.add(new ResumeChunk(sectionName, sectionText));
            } else {
                // Split large sections into paragraph-level chunks
                String[] paragraphs = sectionText.split("\n\n+");
                for (String para : paragraphs) {
                    if (!para.isBlank()) {
                        chunks.add(new ResumeChunk(sectionName, para.trim()));
                    }
                }
            }
        }

        // If no section headers found, chunk by paragraph
        if (chunks.isEmpty()) {
            log.debug("No section headers found — chunking by paragraph");
            String[] paragraphs = rawText.split("\n\n+");
            for (String para : paragraphs) {
                if (!para.isBlank()) {
                    chunks.add(new ResumeChunk("GENERAL", para.trim()));
                }
            }
        }

        log.debug("Resume split into {} chunks", chunks.size());
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

    /**
     * Simple value object for a resume chunk.
     */
    public record ResumeChunk(String section, String text) {
        public String toEmbeddingText() {
            // Prepend section name so embedding model has context
            return "Section: " + section + "\n" + text;
        }
    }
}
