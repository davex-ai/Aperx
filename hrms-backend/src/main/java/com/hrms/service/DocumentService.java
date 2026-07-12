package com.hrms.service;

import com.hrms.dto.response.EmployeeDocumentResponse;
import com.hrms.entity.Employee;
import com.hrms.entity.EmployeeDocument;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeDocumentRepository;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;

    public List<EmployeeDocumentResponse> getMyDocuments(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return documentRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeDocument getForDownload(Long companyId, Long documentId, Long requestingUserId, boolean isPrivileged) {
        EmployeeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!document.getEmployee().getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Document not found");
        }

        if (!isPrivileged) {
            Employee employee = employeeRepository.findByUserId(requestingUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
            if (!document.getEmployee().getId().equals(employee.getId())) {
                throw new ResourceNotFoundException("Document not found");
            }
        }
        return document;
    }

    private EmployeeDocumentResponse toResponse(EmployeeDocument doc) {
        return EmployeeDocumentResponse.builder()
                .id(doc.getId())
                .type(doc.getType())
                .title(doc.getTitle())
                .downloadUrl("/documents/" + doc.getId() + "/download")
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
