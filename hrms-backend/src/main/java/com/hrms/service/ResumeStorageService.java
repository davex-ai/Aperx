package com.hrms.service;

import com.hrms.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Service
public class ResumeStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.storage.documents-dir}")
    private String documentsDir;

    public StoredResume store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A resume file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Resume file must be under 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Resume must be a PDF or Word (.docx) document");
        }

        String extension = contentType.equals("application/pdf") ? ".pdf" : ".docx";

        try {
            Path dir = Path.of(documentsDir, "resumes");
            Files.createDirectories(dir);

            byte[] randomBytes = new byte[16];
            random.nextBytes(randomBytes);
            String uniqueName = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes) + extension;

            Path target = dir.resolve(uniqueName);
            file.transferTo(target);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume" + extension;

            return new StoredResume(target.toString(), originalName, contentType);
        } catch (IOException e) {
            throw new BadRequestException("Failed to save resume file: " + e.getMessage());
        }
    }

    public record StoredResume(String filePath, String originalFileName, String contentType) {}
}
