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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 단위 테스트")
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UsersRepository userRepository;

    private Users testUser;
    private Users otherUser;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("테스트유저")
                .build();

        otherUser = Users.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .name("다른유저")
                .build();

        testComment = Comment.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .hospitalName("서울대학교병원")
                .clinicCode("C001")
                .comment("좋은 병원입니다")
                .score(5)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("댓글 작성")
    class AddComment {

        @Test
        @DisplayName("성공 — 댓글이 정상적으로 저장된다")
        void success() {
            CommentRequestDTO request = CommentRequestDTO.builder()
                    .hospitalName("서울대학교병원")
                    .clinicCode("C001")
                    .comment("좋은 병원입니다")
                    .score(5)
                    .build();

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(commentRepository.save(any(Comment.class))).willReturn(testComment);

            CommentResponseDTO result = commentService.addComment("test@example.com", request);

            assertThat(result).isNotNull();
            assertThat(result.comment()).isEqualTo("좋은 병원입니다");
            assertThat(result.score()).isEqualTo(5);
            assertThat(result.userName()).isEqualTo("테스트유저");
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 사용자")
        void failUserNotFound() {
            CommentRequestDTO request = CommentRequestDTO.builder()
                    .hospitalName("서울대학교병원")
                    .clinicCode("C001")
                    .comment("좋은 병원입니다")
                    .score(5)
                    .build();

            given(userRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.addComment("unknown@example.com", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("성공 — 본인 댓글이 수정된다")
        void success() {
            CommentUpdateDTO request = CommentUpdateDTO.builder()
                    .comment("수정된 댓글")
                    .score(4)
                    .build();

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));

            CommentResponseDTO result = commentService.updateComment("test@example.com", testComment.getId(), request);

            assertThat(result.comment()).isEqualTo("수정된 댓글");
            assertThat(result.score()).isEqualTo(4);
        }

        @Test
        @DisplayName("실패 — 다른 사용자의 댓글 수정 시도")
        void failUnauthorized() {
            CommentUpdateDTO request = CommentUpdateDTO.builder()
                    .comment("수정된 댓글")
                    .score(4)
                    .build();

            given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
            given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment("other@example.com", testComment.getId(), request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 댓글")
        void failCommentNotFound() {
            UUID fakeId = UUID.randomUUID();
            CommentUpdateDTO request = CommentUpdateDTO.builder()
                    .comment("수정된 댓글")
                    .score(4)
                    .build();

            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(commentRepository.findById(fakeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment("test@example.com", fakeId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("성공 — 본인 댓글이 삭제된다")
        void success() {
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));

            commentService.deleteComment("test@example.com", testComment.getId());

            verify(commentRepository).delete(testComment);
        }

        @Test
        @DisplayName("실패 — 다른 사용자의 댓글 삭제 시도")
        void failUnauthorized() {
            given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
            given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.deleteComment("other@example.com", testComment.getId()))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("내 댓글 조회")
    class GetMyComments {

        @Test
        @DisplayName("성공 — 본인의 댓글 목록이 반환된다")
        void success() {
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(commentRepository.findByUserOrderByCreatedAtDesc(testUser)).willReturn(List.of(testComment));

            List<CommentResponseDTO> result = commentService.getMyComments("test@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).hospitalName()).isEqualTo("서울대학교병원");
        }
    }

    @Nested
    @DisplayName("병원별 댓글 조회")
    class GetComments {

        @Test
        @DisplayName("성공 — 본인 댓글에는 userName이 포함된다")
        void successWithOwnerName() {
            given(commentRepository.findByHospitalNameAndClinicCodeOrderByCreatedAtDesc("서울대학교병원", "C001"))
                    .willReturn(List.of(testComment));
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

            List<CommentResponseDTO> result = commentService.getComments("서울대학교병원", "C001", "test@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).userName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("성공 — 비로그인 시 userName이 null이다")
        void successAnonymous() {
            given(commentRepository.findByHospitalNameAndClinicCodeOrderByCreatedAtDesc("서울대학교병원", "C001"))
                    .willReturn(List.of(testComment));

            List<CommentResponseDTO> result = commentService.getComments("서울대학교병원", "C001", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).userName()).isNull();
        }
    }
}
