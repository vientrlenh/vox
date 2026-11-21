package com.sep.vox.domain.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.service.question.QuestionStatusTransition.Actor;
import com.sep.vox.domain.service.question.QuestionStatusTransition.RejectionCode;
import com.sep.vox.domain.service.question.QuestionStatusTransition.RejectionKind;

/**
 * Quy tắc chuyển trạng thái câu hỏi.
 *
 * <p>Bất biến quan trọng nhất được kiểm ở đây: hàm này KHÔNG BAO GIỜ ném exception. Đường đi cập
 * nhật hàng loạt dựa vào đúng điều đó để xử lý được câu lỗi rồi đi tiếp mà không làm hỏng
 * transaction dùng chung.
 */
class QuestionStatusTransitionTests {

    private final UUID ownerId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID bankId = UUID.randomUUID();

    @Test
    void should_allow_the_owner_to_submit_a_draft() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.DRAFT), systemBank(), false, owner(), "SUBMIT", null);

        assertThat(rejection).isNull();
    }

    @Test
    void should_allow_the_owner_to_submit_a_question_sent_back_for_revision() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.REVISION_REQUESTED), systemBank(), false, owner(), "SUBMIT", null);

        assertThat(rejection).isNull();
    }

    @Test
    void should_reject_submit_from_a_stranger() {
        QuestionStatusTransition.Actor stranger = new Actor(UUID.randomUUID(), schoolId, false, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.DRAFT), systemBank(), false, stranger, "SUBMIT", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.FORBIDDEN);
        assertThat(rejection.code()).isEqualTo(RejectionCode.NO_PERMISSION);
    }

    @Test
    void should_reject_submit_when_the_question_is_already_under_review() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), systemBank(), false, owner(), "SUBMIT", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.INVALID_STATE);
        assertThat(rejection.code()).isEqualTo(RejectionCode.INVALID_STATUS);
    }

    /**
     * The reason must say by itself why this question was skipped: which status it is in and which
     * status it needs. The bulk review screen shows this string directly for dozens of questions at
     * once, so a generic message such as "invalid status" is useless to the user.
     */
    @Test
    void should_name_the_current_and_required_status_when_the_status_blocks_the_action() {
        QuestionStatusTransition.Actor systemAdmin = new Actor(UUID.randomUUID(), null, true, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.APPROVED), systemBank(), false, systemAdmin, "APPROVE", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.reason()).isEqualTo(
            "Không thể duyệt: câu hỏi đang ở trạng thái \"Đã duyệt\", "
                + "thao tác này chỉ áp dụng cho câu hỏi ở trạng thái \"Chờ duyệt\"");
    }

    @Test
    void should_list_every_allowed_status_when_more_than_one_is_accepted() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.PUBLISHED), systemBank(), false, owner(), "SUBMIT", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.reason()).isEqualTo(
            "Không thể gửi duyệt: câu hỏi đang ở trạng thái \"Đã xuất bản\", "
                + "thao tác này chỉ áp dụng cho câu hỏi ở trạng thái \"Bản nháp\" hoặc \"Yêu cầu sửa\"");
    }

    /**
     * Approving your own work is a review hole -- block it even when the author is a collaborator
     * with edit permission.
     */
    @Test
    void should_reject_approve_from_the_author_even_when_they_can_edit() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), systemBank(), true, owner(), "APPROVE", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.FORBIDDEN);
        assertThat(rejection.code()).isEqualTo(RejectionCode.SELF_REVIEW);
        assertThat(rejection.reason())
            .isEqualTo("Không thể duyệt: bạn là người tạo câu hỏi này, cần người khác duyệt");
    }

    @Test
    void should_allow_another_editor_collaborator_to_approve() {
        QuestionStatusTransition.Actor reviewer = new Actor(UUID.randomUUID(), schoolId, false, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), systemBank(), true, reviewer, "APPROVE", null);

        assertThat(rejection).isNull();
    }

    @Test
    void should_allow_a_system_admin_to_approve_on_a_system_bank() {
        QuestionStatusTransition.Actor systemAdmin = new Actor(UUID.randomUUID(), null, true, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), systemBank(), false, systemAdmin, "APPROVE", null);

        assertThat(rejection).isNull();
    }

    @Test
    void should_reject_a_school_admin_reviewing_a_bank_of_another_school() {
        QuestionStatusTransition.Actor schoolAdmin = new Actor(UUID.randomUUID(), UUID.randomUUID(), false, true);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), schoolBank(), false, schoolAdmin, "APPROVE", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.FORBIDDEN);
        assertThat(rejection.code()).isEqualTo(RejectionCode.NO_PERMISSION);
        assertThat(rejection.reason())
            .isEqualTo("Không thể duyệt: bạn không có quyền duyệt câu hỏi trong ngân hàng này");
    }

    @Test
    void should_allow_a_school_admin_to_approve_on_their_own_school_bank() {
        QuestionStatusTransition.Actor schoolAdmin = new Actor(UUID.randomUUID(), schoolId, false, true);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), schoolBank(), false, schoolAdmin, "APPROVE", null);

        assertThat(rejection).isNull();
    }

    @Test
    void should_require_a_note_when_rejecting() {
        QuestionStatusTransition.Actor systemAdmin = new Actor(UUID.randomUUID(), null, true, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), systemBank(), false, systemAdmin, "REJECT", "  ");

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.INVALID_STATE);
        assertThat(rejection.code()).isEqualTo(RejectionCode.NOTE_REQUIRED);
        assertThat(rejection.reason()).isEqualTo("Không thể từ chối: thao tác này bắt buộc phải nhập lý do");
    }

    @Test
    void should_require_a_note_when_requesting_revision() {
        QuestionStatusTransition.Actor systemAdmin = new Actor(UUID.randomUUID(), null, true, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.SUBMITTED_FOR_REVIEW), systemBank(), false, systemAdmin, "REQUEST_REVISION", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.reason())
            .isEqualTo("Không thể yêu cầu chỉnh sửa: thao tác này bắt buộc phải nhập lý do");
    }

    @Test
    void should_allow_the_owner_to_publish_an_approved_question() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.APPROVED), systemBank(), false, owner(), "PUBLISH", null);

        assertThat(rejection).isNull();
    }

    /**
     * Putting an archived question back into circulation is an admin action, not an author action.
     */
    @Test
    void should_reject_the_owner_republishing_an_archived_question() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.ARCHIVED), systemBank(), false, owner(), "PUBLISH", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.FORBIDDEN);
        assertThat(rejection.code()).isEqualTo(RejectionCode.ADMIN_ONLY);
    }

    @Test
    void should_reject_archive_from_a_non_admin() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.PUBLISHED), systemBank(), true, owner(), "ARCHIVE", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.FORBIDDEN);
        assertThat(rejection.code()).isEqualTo(RejectionCode.ADMIN_ONLY);
        assertThat(rejection.reason()).isEqualTo(
            "Không thể lưu trữ: chỉ quản trị viên của ngân hàng câu hỏi mới thực hiện được");
    }

    @Test
    void should_reject_locking_a_question_that_is_already_locked() {
        QuestionStatusTransition.Actor systemAdmin = new Actor(UUID.randomUUID(), null, true, false);
        Question question = question(QuestionStatus.PUBLISHED);
        question.setLocked(true);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question, systemBank(), false, systemAdmin, "LOCK", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.code()).isEqualTo(RejectionCode.ALREADY_LOCKED);
        assertThat(rejection.reason()).isEqualTo("Không thể khóa: câu hỏi đã bị khóa");
    }

    @Test
    void should_reject_unlocking_a_question_that_is_not_locked() {
        QuestionStatusTransition.Actor systemAdmin = new Actor(UUID.randomUUID(), null, true, false);

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.PUBLISHED), systemBank(), false, systemAdmin, "UNLOCK", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.code()).isEqualTo(RejectionCode.NOT_LOCKED);
        assertThat(rejection.reason()).isEqualTo("Không thể mở khóa: câu hỏi chưa bị khóa");
    }

    @Test
    void should_report_an_unknown_action_instead_of_throwing() {
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question(QuestionStatus.DRAFT), systemBank(), false, owner(), "DEMOLISH", null);

        assertThat(rejection).isNotNull();
        assertThat(rejection.kind()).isEqualTo(RejectionKind.INVALID_STATE);
        assertThat(rejection.code()).isEqualTo(RejectionCode.INVALID_ACTION);
        assertThat(rejection.reason()).isEqualTo("Action không hợp lệ");
    }

    @Test
    void should_move_the_question_to_the_target_status_when_applied() {
        Question question = question(QuestionStatus.DRAFT);

        QuestionStatusTransition.apply(question, "SUBMIT");

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW);
    }

    @Test
    void should_only_flip_the_lock_flag_for_lock_actions() {
        Question question = question(QuestionStatus.PUBLISHED);

        QuestionStatusTransition.apply(question, "LOCK");

        assertThat(question.isLocked()).isTrue();
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
    }

    private Actor owner() {
        return new Actor(ownerId, schoolId, false, false);
    }

    private Question question(QuestionStatus status) {
        return Question.builder()
                .id(UUID.randomUUID())
                .questionBankId(bankId)
                .questionTopicId(UUID.randomUUID())
                .code("Q-1")
                .instructionText(null)
                .questionText("Câu hỏi")
                .promptText(null)
                .preparationText(null)
                .type(QuestionType.SHORT_ANSWER)
                .preparationTimeSeconds(10)
                .minResponseSeconds(10)
                .maxResponseSeconds(60)
                .sharing(QuestionSharing.PRIVATE)
                .sourceQuestionId(null)
                .locked(false)
                .confidentiality(QuestionConfidentiality.OPEN)
                .securePoolId(null)
                .status(status)
                .createdBy(ownerId.toString())
                .updatedBy(ownerId.toString())
                .build();
    }

    private QuestionBank systemBank() {
        return bank(QuestionBankOwnerType.SYSTEM, null);
    }

    private QuestionBank schoolBank() {
        return bank(QuestionBankOwnerType.SCHOOL, schoolId);
    }

    private QuestionBank bank(QuestionBankOwnerType ownerType, UUID bankSchoolId) {
        return QuestionBank.builder()
                .id(bankId)
                .languageId(UUID.randomUUID())
                .schoolId(bankSchoolId)
                .code("BANK-1")
                .name("Ngân hàng")
                .description(null)
                .ownerType(ownerType)
                .status(QuestionBankStatus.PUBLISHED)
                .createdBy(ownerId.toString())
                .updatedBy(ownerId.toString())
                .build();
    }
}
