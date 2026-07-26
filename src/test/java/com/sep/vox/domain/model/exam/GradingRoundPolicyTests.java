package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Ma trận 4 vòng × 4 hành động — trái tim của bản rework. Mọi use case chấm bài đều
 * hỏi lớp này, nên một ô sai ở đây là sai ở cả bốn use case cùng lúc.
 */
class GradingRoundPolicyTests {

    // ---- vòng nào nhận bài ở trạng thái nào --------------------------------

    static Stream<Arguments> assignableCombinations() {
        return Stream.of(
            Arguments.of(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW),
            Arguments.of(GradingRoundType.SPOT_CHECK, ExamCandidateResultStatus.RELEASED),
            Arguments.of(GradingRoundType.REMEDIATION, ExamCandidateResultStatus.INVALID),
            Arguments.of(GradingRoundType.APPEAL, ExamCandidateResultStatus.APPEALED),
            Arguments.of(GradingRoundType.APPEAL, ExamCandidateResultStatus.RE_GRADING)
        );
    }

    @ParameterizedTest(name = "{0} nhận bài {1}")
    @MethodSource("assignableCombinations")
    void should_accept_the_status_its_round_is_meant_for(
            GradingRoundType roundType, ExamCandidateResultStatus status) {
        assertThat(GradingRoundPolicy.isAssignable(roundType, status)).isTrue();
    }

    @Test
    void should_not_let_a_round_pick_up_a_result_from_another_round() {
        // Bài đã công bố không quay lại vòng chấm lần đầu; bài chờ chấm chưa có gì
        // để hậu kiểm. Nhầm hai chiều này là nhầm nguy hiểm nhất của thiết kế gộp.
        assertThat(GradingRoundPolicy.isAssignable(
            GradingRoundType.INITIAL, ExamCandidateResultStatus.RELEASED)).isFalse();
        assertThat(GradingRoundPolicy.isAssignable(
            GradingRoundType.SPOT_CHECK, ExamCandidateResultStatus.PENDING_REVIEW)).isFalse();
        assertThat(GradingRoundPolicy.isAssignable(
            GradingRoundType.REMEDIATION, ExamCandidateResultStatus.RELEASED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(GradingRoundType.class)
    void should_reject_a_null_status(GradingRoundType roundType) {
        assertThat(GradingRoundPolicy.isAssignable(roundType, null)).isFalse();
    }

    // ---- hành động nào hợp lệ với vòng nào ---------------------------------

    @Test
    void should_only_allow_clear_invalid_in_the_remediation_round() {
        assertThat(GradingRoundPolicy.isAllowed(
            GradingRoundType.REMEDIATION, GradingOutcome.CLEARED_INVALID)).isTrue();
        assertThat(GradingRoundPolicy.isAllowed(
            GradingRoundType.INITIAL, GradingOutcome.CLEARED_INVALID)).isFalse();
        assertThat(GradingRoundPolicy.isAllowed(
            GradingRoundType.SPOT_CHECK, GradingOutcome.CLEARED_INVALID)).isFalse();
        assertThat(GradingRoundPolicy.isAllowed(
            GradingRoundType.APPEAL, GradingOutcome.CLEARED_INVALID)).isFalse();
    }

    @Test
    void should_not_allow_regrading_a_result_that_is_still_invalid() {
        // Ở REMEDIATION chỉ có hai lựa chọn: xác nhận vi phạm, hoặc gỡ vô hiệu rồi
        // mới chấm. Cho chấm thẳng ở đây là bỏ qua bước gỡ blocked_at của thí sinh.
        assertThat(GradingRoundPolicy.isAllowed(
            GradingRoundType.REMEDIATION, GradingOutcome.REGRADED)).isFalse();
    }

    @Test
    void should_not_allow_invalidating_during_an_appeal() {
        // Phúc khảo là để soi lại ĐIỂM. Phát hiện vi phạm lúc đó đi đường khác
        // (buộc kết thúc / vô hiệu ở cấp kỳ thi), không lẫn vào phán quyết của đơn.
        assertThat(GradingRoundPolicy.isAllowed(
            GradingRoundType.APPEAL, GradingOutcome.INVALIDATED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(GradingRoundType.class)
    void should_allow_declining_in_every_round(GradingRoundType roundType) {
        // Giáo viên quen biết thí sinh có thể xảy ra ở bất kỳ vòng nào.
        assertThat(GradingRoundPolicy.isAllowed(roundType, GradingOutcome.DECLINED)).isTrue();
    }

    // ---- trạng thái bài sau hành động --------------------------------------

    @Test
    void should_release_the_result_when_the_first_round_ends() {
        // Cả UPHELD lẫn REGRADED đều công bố — không còn bước admin release riêng.
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.INITIAL, GradingOutcome.UPHELD))
            .isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.INITIAL, GradingOutcome.REGRADED))
            .isEqualTo(ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_keep_a_spot_checked_result_untouched() {
        // null = giữ nguyên. Bài đang RELEASED phải ở RELEASED suốt quá trình hậu
        // kiểm, nếu không học sinh sẽ thấy điểm biến mất rồi hiện lại.
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.SPOT_CHECK, GradingOutcome.UPHELD))
            .isNull();
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.SPOT_CHECK, GradingOutcome.REGRADED))
            .isNull();
    }

    @Test
    void should_invalidate_from_both_grading_rounds() {
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.INITIAL, GradingOutcome.INVALIDATED))
            .isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.SPOT_CHECK, GradingOutcome.INVALIDATED))
            .isEqualTo(ExamCandidateResultStatus.INVALID);
    }

    @Test
    void should_send_a_cleared_result_back_to_pending_review() {
        assertThat(GradingRoundPolicy.resultStatusAfter(
            GradingRoundType.REMEDIATION, GradingOutcome.CLEARED_INVALID))
            .isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
    }

    @Test
    void should_keep_a_confirmed_violation_invalid() {
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.REMEDIATION, GradingOutcome.UPHELD))
            .isNull();
    }

    @Test
    void should_release_the_result_after_an_appeal_regardless_of_outcome() {
        // Kết quả quay về RELEASED chứ không phải FINAL: học sinh còn phúc khảo lại
        // được trong hạn mức.
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.APPEAL, GradingOutcome.UPHELD))
            .isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(GradingRoundPolicy.resultStatusAfter(GradingRoundType.APPEAL, GradingOutcome.REGRADED))
            .isEqualTo(ExamCandidateResultStatus.RELEASED);
    }

    @ParameterizedTest
    @EnumSource(GradingRoundType.class)
    void should_leave_the_result_alone_when_the_assignment_is_declined(GradingRoundType roundType) {
        assertThat(GradingRoundPolicy.resultStatusAfter(roundType, GradingOutcome.DECLINED)).isNull();
    }

    @Test
    void should_refuse_to_compute_a_status_for_an_illegal_combination() {
        assertThatThrownBy(() -> GradingRoundPolicy.resultStatusAfter(
            GradingRoundType.APPEAL, GradingOutcome.INVALIDATED))
            .isInstanceOf(IllegalStateException.class);
    }

    // ---- dữ liệu bắt buộc kèm hành động ------------------------------------

    @Test
    void should_require_a_reason_for_every_irreversible_judgement() {
        // Ba hành động này là thứ bị mang ra tranh chấp về sau; thiếu lý do là
        // không giải trình được. Bản cũ để INVALIDATE trống lý do — đó là lỗ hổng.
        assertThat(GradingRoundPolicy.requiresReason(GradingOutcome.INVALIDATED)).isTrue();
        assertThat(GradingRoundPolicy.requiresReason(GradingOutcome.CLEARED_INVALID)).isTrue();
        assertThat(GradingRoundPolicy.requiresReason(GradingOutcome.DECLINED)).isTrue();
        assertThat(GradingRoundPolicy.requiresReason(GradingOutcome.UPHELD)).isFalse();
        assertThat(GradingRoundPolicy.requiresReason(GradingOutcome.REGRADED)).isFalse();
    }

    @Test
    void should_require_scores_only_when_regrading() {
        assertThat(GradingRoundPolicy.requiresScores(GradingOutcome.REGRADED)).isTrue();
        assertThat(GradingRoundPolicy.requiresScores(GradingOutcome.UPHELD)).isFalse();
    }
}
