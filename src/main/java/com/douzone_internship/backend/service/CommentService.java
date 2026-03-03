package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.request.CommentRequestDTO;
import com.douzone_internship.backend.dto.request.CommentUpdateDTO;
import com.douzone_internship.backend.dto.response.CommentResponseDTO;
import com.douzone_internship.backend.exceptions.ResourceNotFoundException;
import com.douzone_internship.backend.exceptions.UnauthorizedException;
import com.douzone_internship.backend.repository.CommentRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final UsersRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponseDTO addComment(String userEmail, CommentRequestDTO request) {
        Users user = findUserByEmail(userEmail);

        Comment comment = Comment.builder()
                .comment(request.comment())
                .score(request.score())
                .clinicCode(request.clinicCode())
                .hospitalName(request.hospitalName())
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return CommentResponseDTO.from(savedComment, true);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(String hospitalName, String clinicCode, String currentEmail) {
        List<Comment> comments = commentRepository
                .findByHospitalNameAndClinicCodeOrderByCreatedAtDesc(hospitalName, clinicCode);

        UUID currentUserId = null;
        if (currentEmail != null) {
            currentUserId = userRepository.findByEmail(currentEmail)
                    .map(Users::getId).orElse(null);
        }

        UUID finalId = currentUserId;
        return comments.stream()
                .map(c -> CommentResponseDTO.from(c,
                        finalId != null && c.getUser().getId().equals(finalId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getMyComments(String email) {
        Users user = findUserByEmail(email);

        return commentRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(CommentResponseDTO::fromMy)
                .toList();
    }

    @Transactional
    public CommentResponseDTO updateComment(String userEmail, UUID commentId, CommentUpdateDTO request) {
        Users user = findUserByEmail(userEmail);
        Comment comment = findCommentById(commentId);
        verifyOwnership(comment, user);

        comment.update(request.comment(), request.score());
        return CommentResponseDTO.from(comment, true);
    }

    @Transactional
    public void deleteComment(String userEmail, UUID commentId) {
        Users user = findUserByEmail(userEmail);
        Comment comment = findCommentById(commentId);
        verifyOwnership(comment, user);

        commentRepository.delete(comment);
    }

    private Users findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Comment findCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다."));
    }

    private void verifyOwnership(Comment comment, Users user) {
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("본인의 댓글만 수정/삭제할 수 있습니다.");
        }
    }
}
