package com.hrms.controller;

import com.hrms.dto.response.EmployeeDocumentResponse;
import com.hrms.entity.EmployeeDocument;
import com.hrms.service.DocumentService;
import com.hrms.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final AuthUtil authUtil;

    @GetMapping("/me")
    public ResponseEntity<List<EmployeeDocumentResponse>> myDocuments() {
        return ResponseEntity.ok(documentService.getMyDocuments(authUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        boolean privileged = authUtil.isPrivileged();
        EmployeeDocument document = documentService.getForDownload(authUtil.getCurrentCompanyId(), id, authUtil.getCurrentUserId(), privileged);

        File file = new File(document.getFilePath());
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
