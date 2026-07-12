package com.hrms.controller;

import com.hrms.dto.request.AnnouncementRequest;
import com.hrms.dto.request.CommentRequest;
import com.hrms.dto.response.AnnouncementResponse;
import com.hrms.dto.response.CommentResponse;
import com.hrms.service.AnnouncementService;
import com.hrms.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getFeed() {
        return ResponseEntity.ok(announcementService.getFeed(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(
                announcementService.createAnnouncement(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(
                announcementService.updateAnnouncement(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        announcementService.deleteAnnouncement(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(
                announcementService.getComments(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(
                announcementService.addComment(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        announcementService.deleteComment(authUtil.getCurrentCompanyId(), authUtil.getCurrentUserId(), id, commentId);
        return ResponseEntity.noContent().build();
    }
}
