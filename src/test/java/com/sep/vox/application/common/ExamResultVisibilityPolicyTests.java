package com.sep.vox.application.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;

/**
 * Luật "học sinh được xem bài ở trạng thái nào" chỉ được phát biểu ở đúng một chỗ, nên
 * đây là chỗ duy nhất khoá nó lại. Ca quan trọng nhất là {@code PENDING_REVIEW}: trước
 * đợt này học sinh xem được bài chưa ai soát, và {@code PASSED}/{@code FAILED} bị bỏ sót
 * khiến bài đã chốt lại bị giấu khỏi chính chủ.
 */
class ExamResultVisibilityPolicyTests {

    private static final EnumSet<ExamCandidateResultStatus> VISIBLE = EnumSet.of(
        ExamCandidateResultStatus.RELEASED,
        ExamCandidateResultStatus.FINAL,
        ExamCandidateResultStatus.PASSED,
        ExamCandidateResultStatus.FAILED,
        ExamCandidateResultStatus.INVALID
    );

    @Test
    void should_show_result_to_candidate_when_released() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.RELEASED)).isTrue();
    }

    @Test
    void should_show_result_to_candidate_when_final() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.FINAL)).isTrue();
    }

    @Test
    void should_show_result_to_candidate_when_passed() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.PASSED)).isTrue();
    }

    @Test
    void should_show_result_to_candidate_when_failed() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.FAILED)).isTrue();
    }

    @Test
    void should_show_result_to_candidate_when_invalid() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.INVALID)).isTrue();
    }

    @Test
    void should_hide_result_from_candidate_when_pending_review() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.PENDING_REVIEW)).isFalse();
    }

    @Test
    void should_hide_result_from_candidate_when_appealed() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.APPEALED)).isFalse();
    }

    @Test
    void should_hide_result_from_candidate_when_re_grading() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.RE_GRADING)).isFalse();
    }

    @Test
    void should_hide_result_from_candidate_when_retake_required() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(ExamCandidateResultStatus.RETAKE_REQUIRED)).isFalse();
    }

    @Test
    void should_hide_result_when_status_is_missing() {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(null)).isFalse();
    }

    /**
     * Quét toàn enum: giá trị mới thêm vào mà quên xếp loại thì test này đỏ ngay, kể cả
     * khi ai đó đã "chữa" lỗi biên dịch bằng một nhánh default cho xong.
     */
    @ParameterizedTest
    @EnumSource(ExamCandidateResultStatus.class)
    void should_classify_every_result_status(ExamCandidateResultStatus status) {
        assertThat(ExamResultVisibilityPolicy.isVisibleToCandidate(status))
            .isEqualTo(VISIBLE.contains(status));
    }
}
