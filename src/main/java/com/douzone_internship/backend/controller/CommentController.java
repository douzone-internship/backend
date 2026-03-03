package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.CommentRequestDTO;
import com.douzone_internship.backend.dto.request.CommentUpdateDTO;
import com.douzone_internship.backend.dto.response.CommentResponseDTO;
import com.douzone_internship.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/comments")
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "로그인한 사용자가 병원에 댓글과 평점을 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<CommentResponseDTO> add(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CommentRequestDTO dto) {
        return ResponseEntity.ok(commentService.addComment(user.getUsername(), dto));
    }

    @Operation(summary = "병원별 댓글 목록 조회", description = "병원명과 진료코드로 댓글 목록을 조회합니다. 비로그인 시 작성자명이 익명 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> list(
            @Parameter(description = "병원명", required = true) @RequestParam String hospitalName,
            @Parameter(description = "진료 코드", required = true) @RequestParam String clinicCode,
            @AuthenticationPrincipal UserDetails user) {
        String email = (user != null) ? user.getUsername() : null;
        return ResponseEntity.ok(commentService.getComments(hospitalName, clinicCode, email));
    }

    @Operation(summary = "내 댓글 목록 조회", description = "로그인한 사용자가 작성한 댓글 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/my")
    public ResponseEntity<List<CommentResponseDTO>> myComments(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(commentService.getMyComments(user.getUsername()));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글의 내용과 평점을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 댓글만 수정 가능"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponseDTO> update(
            @AuthenticationPrincipal UserDetails user,
            @Parameter(description = "댓글 ID") @PathVariable UUID id,
            @Valid @RequestBody CommentUpdateDTO dto) {
        return ResponseEntity.ok(commentService.updateComment(user.getUsername(), id, dto));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 댓글만 삭제 가능"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @Parameter(description = "댓글 ID") @PathVariable UUID id) {
        commentService.deleteComment(user.getUsername(), id);
        return ResponseEntity.ok().build();
    }
}
