package com.hrms.service;

import com.hrms.dto.request.AnnouncementRequest;
import com.hrms.dto.request.CommentRequest;
import com.hrms.dto.response.AnnouncementResponse;
import com.hrms.dto.response.CommentResponse;
import com.hrms.entity.Announcement;
import com.hrms.entity.AnnouncementComment;
import com.hrms.entity.Company;
import com.hrms.entity.Employee;
import com.hrms.enums.UserRole;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AnnouncementCommentRepository;
import com.hrms.repository.AnnouncementRepository;
import com.hrms.repository.CompanyRepository;
import com.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCommentRepository commentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public AnnouncementResponse createAnnouncement(Long companyId, Long userId, AnnouncementRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        Employee author = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        boolean requestedPin = Boolean.TRUE.equals(request.getIsPinned());
        if (requestedPin && !isManagerOrAdmin(author)) {
            throw new BadRequestException("Only managers and admins can pin announcements");
        }

        Announcement announcement = Announcement.builder()
                .company(company)
                .author(author)
                .title(request.getTitle())
                .body(request.getBody())
                .isPinned(requestedPin)
                .build();
        announcement = announcementRepository.save(announcement);

        return toResponse(announcement, author);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long companyId, Long userId, Long announcementId, AnnouncementRequest request) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        Announcement announcement = announcementRepository.findByIdAndCompanyId(announcementId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        if (!canManage(announcement.getAuthor(), requester)) {
            throw new BadRequestException("You can only edit your own announcements");
        }

        if (Boolean.TRUE.equals(request.getIsPinned()) && !isManagerOrAdmin(requester)) {
            throw new BadRequestException("Only managers and admins can pin announcements");
        }

        announcement.setTitle(request.getTitle());
        announcement.setBody(request.getBody());
        if (request.getIsPinned() != null) {
            announcement.setIsPinned(request.getIsPinned());
        }
        announcement = announcementRepository.save(announcement);

        return toResponse(announcement, requester);
    }

    @Transactional
    public void deleteAnnouncement(Long companyId, Long userId, Long announcementId) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        Announcement announcement = announcementRepository.findByIdAndCompanyId(announcementId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        if (!canManage(announcement.getAuthor(), requester)) {
            throw new BadRequestException("You can only delete your own announcements");
        }

        announcementRepository.delete(announcement);
    }

    public List<AnnouncementResponse> getFeed(Long companyId, Long userId) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return announcementRepository.findByCompanyIdOrderByIsPinnedDescCreatedAtDesc(companyId).stream()
                .map(a -> toResponse(a, requester))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(Long companyId, Long userId, Long announcementId, CommentRequest request) {
        Employee author = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        Announcement announcement = announcementRepository.findByIdAndCompanyId(announcementId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        AnnouncementComment comment = AnnouncementComment.builder()
                .announcement(announcement)
                .author(author)
                .body(request.getBody())
                .build();
        comment = commentRepository.save(comment);

        return toResponse(comment, author);
    }

    public List<CommentResponse> getComments(Long companyId, Long userId, Long announcementId) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        announcementRepository.findByIdAndCompanyId(announcementId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        return commentRepository.findByAnnouncementIdOrderByCreatedAtAsc(announcementId).stream()
                .map(c -> toResponse(c, requester))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long companyId, Long userId, Long announcementId, Long commentId) {
        Employee requester = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        announcementRepository.findByIdAndCompanyId(announcementId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        AnnouncementComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getAnnouncement().getId().equals(announcementId)) {
            throw new ResourceNotFoundException("Comment not found");
        }
        if (!canManage(comment.getAuthor(), requester)) {
            throw new BadRequestException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private boolean canManage(Employee author, Employee requester) {
        return author.getId().equals(requester.getId()) || isManagerOrAdmin(requester);
    }

    private boolean isManagerOrAdmin(Employee employee) {
        UserRole role = employee.getUser().getRole();
        return role == UserRole.ROLE_ADMIN || role == UserRole.ROLE_MANAGER;
    }

    private AnnouncementResponse toResponse(Announcement a, Employee requester) {
        Employee author = a.getAuthor();
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .body(a.getBody())
                .isPinned(a.getIsPinned())
                .authorId(author.getId())
                .authorName(author.getFirstName() + " " + author.getLastName())
                .authorJobTitle(author.getJobTitle())
                .commentCount(commentRepository.countByAnnouncementId(a.getId()))
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .canManage(canManage(author, requester))
                .build();
    }

    private CommentResponse toResponse(AnnouncementComment c, Employee requester) {
        Employee author = c.getAuthor();
        return CommentResponse.builder()
                .id(c.getId())
                .body(c.getBody())
                .authorId(author.getId())
                .authorName(author.getFirstName() + " " + author.getLastName())
                .createdAt(c.getCreatedAt())
                .canManage(canManage(author, requester))
                .build();
    }
}
