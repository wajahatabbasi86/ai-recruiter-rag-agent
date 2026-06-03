package com.airecruiter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ResumeParserService {

    private final Tika tika = new Tika();

    private static final int MAX_CHUNK_SIZE = 1200; // increased for better context

    //  Fixed: broader pattern covering real resume headers
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(?im)^\\s*(PROFESSIONAL EXPERIENCE|WORK EXPERIENCE|EXPERIENCE|" +
                    "TECHNICAL SKILLS|SKILLS|EDUCATION|PROFILE SUMMARY|SUMMARY|OBJECTIVE|" +
                    "CERTIFICATIONS?(\\s*&\\s*TRAINING)?|PROJECTS|ACHIEVEMENTS|" +
                    "LANGUAGES|CONTACT|PROFESSIONAL SUMMARY)\\s*$"
    );

    public String extractText(MultipartFile file) throws IOException, TikaException {
        log.debug("Parsing file: {} ({})", file.getOriginalFilename(), file.getContentType());
        String text = tika.parseToString(file.getInputStream());
        log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
        return text;
    }

    public List<ResumeChunk> chunk(String rawText) {
        List<ResumeChunk> chunks = new ArrayList<>();

        // Fixed: find all header positions first
        List<int[]> headerPositions = new ArrayList<>(); // [start, end, groupStart]
        List<String> headerNames = new ArrayList<>();

        Matcher matcher = SECTION_PATTERN.matcher(rawText);
        while (matcher.find()) {
            headerPositions.add(new int[]{matcher.start(), matcher.end()});
            headerNames.add(matcher.group(1).toUpperCase().trim());
        }

        if (headerPositions.isEmpty()) {
            // No headers found — chunk by paragraph
            log.debug("No section headers found — chunking by paragraph");
            String[] paragraphs = rawText.split("\n\n+");
            for (String para : paragraphs) {
                if (!para.isBlank() && para.trim().length() > 30) {
                    chunks.add(new ResumeChunk("GENERAL", para.trim()));
                }
            }
            log.debug("Resume split into {} chunks", chunks.size());
            return chunks;
        }

        //  Fixed: text BEFORE first header → label as SUMMARY/GENERAL
        String beforeFirst = rawText.substring(0, headerPositions.get(0)[0]).trim();
        if (!beforeFirst.isBlank() && beforeFirst.length() > 30) {
            chunks.add(new ResumeChunk("GENERAL", beforeFirst));
        }

        //  Fixed: each section's text starts AFTER its header, ends BEFORE next header
        for (int i = 0; i < headerPositions.size(); i++) {
            String sectionName = headerNames.get(i);
            int textStart = headerPositions.get(i)[1]; // end of header line
            int textEnd = (i + 1 < headerPositions.size())
                    ? headerPositions.get(i + 1)[0]        // start of next header
                    : rawText.length();                      // end of document

            String sectionText = rawText.substring(textStart, textEnd).trim();

            if (sectionText.isBlank() || sectionText.length() < 20) continue;

            if (sectionText.length() <= MAX_CHUNK_SIZE) {
                chunks.add(new ResumeChunk(sectionName, sectionText));
            } else {
                // Split large sections into paragraphs
                String[] paragraphs = sectionText.split("\n\n+");
                for (String para : paragraphs) {
                    if (!para.isBlank() && para.trim().length() > 30) {
                        chunks.add(new ResumeChunk(sectionName, para.trim()));
                    }
                }
            }
        }

        log.debug("Resume split into {} chunks", chunks.size());
        return chunks;
    }

    public record ResumeChunk(String section, String text) {
        public String toEmbeddingText() {
            return "Section: " + section + "\n" + text;
        }
    }
}