package com.sep.vox.application.common.permission;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class QuestionCommandPermissionChecker {

    private final UserContextPort userContextPort;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserRepository userRepository;

    public QuestionCommandPermissionChecker(
            UserContextPort userContextPort,
            UserRoleQueryRepository userRoleQueryRepository,
            UserRepository userRepository) {
        this.userContextPort = userContextPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userRepository = userRepository;
    }

    public UserContext resolveCurrentUser() {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("Không tìm thấy người dùng"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Tài khoản không hoạt động");
        }
        var roleInfos = userRoleQueryRepository.findByUserIdWithRoleInfo(userId);
        List<String> roleCodes = roleInfos.stream()
            .map(r -> r.roleCode())
            .toList();
        UserRole role = UserRole.fromRoleCodes(roleCodes);
        return new UserContext(userId, role, user.getSchoolId(), roleCodes);
    }

    /**
     * Implements EDIT_QUESTION_CONTENT flowchart.
     */
    public void checkCanEditContent(Question question, QuestionTopic topic, QuestionBank bank, UserContext user) {
        // Locked check
        if (question.isLocked()) {
            throw new ForbiddenException("Câu hỏi đang bị khóa, không thể chỉnh sửa");
        }

        // Bank status check
        if (bank.getStatus() == QuestionBankStatus.ARCHIVED) {
            throw new ForbiddenException("Ngân hàng câu hỏi đã lưu trữ");
        }

        // Topic status check
        if (topic.getStatus() == QuestionTopicStatus.ARCHIVED) {
            throw new ForbiddenException("Chủ đề đã lưu trữ");
        }

        // Question status check
        if (question.getStatus() == QuestionStatus.ARCHIVED
                || question.getStatus() == QuestionStatus.PUBLISHED
                || question.getStatus() == QuestionStatus.SUBMITTED_FOR_REVIEW
                || question.getStatus() == QuestionStatus.APPROVED) {
            throw new ForbiddenException("Không thể chỉnh sửa câu hỏi ở trạng thái " + question.getStatus().name());
        }

        // Only DRAFT, REVISION_REQUESTED, REJECTED are editable
        if (question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED
                && question.getStatus() != QuestionStatus.REJECTED) {
            throw new ForbiddenException("Không thể chỉnh sửa câu hỏi ở trạng thái " + question.getStatus().name());
        }

        // Scope-based check
        switch (question.getScope()) {
            case QUESTION_BANK -> checkQuestionBankEditContent(question, bank, user);
            case CLASSROOM_ASSESSMENT -> throw new ForbiddenException("Chức năng chỉnh sửa câu hỏi đánh giá lớp học chưa được hỗ trợ");
            case CENTRAL_EXAM_DRAFT -> throw new ForbiddenException("Chức năng chỉnh sửa câu hỏi đề thi trung ương chưa được hỗ trợ");
            case CENTRAL_EXAM_PAPER -> throw new ForbiddenException("Chức năng chỉnh sửa câu hỏi bài thi trung ương chưa được hỗ trợ");
        }
    }

    /**
     * Implements REVIEW_QUESTION_ACTION flowchart.
     * Uses QuestionStatus as the target status for the transition.
     */
    public void checkCanReviewAction(Question question, QuestionTopic topic, QuestionBank bank,
                                     QuestionStatus targetStatus, UserContext user) {
        switch (targetStatus) {
            case SUBMITTED_FOR_REVIEW -> checkSubmitForReview(question, topic, bank, user);
            case REVISION_REQUESTED -> checkRequestRevision(question, bank, user);
            case APPROVED -> checkApprove(question, bank, user);
            case REJECTED -> checkReject(question, bank, user);
            case PUBLISHED -> checkPublish(question, topic, bank, user);
            case ARCHIVED -> checkArchive(question, bank, user);
            case DRAFT -> checkRestore(question, bank, user);
            default -> throw new ForbiddenException("Trạng thái đích không hợp lệ: " + targetStatus.name());
        }
    }

    // ==================== EDIT CONTENT HELPERS ====================

    private void checkQuestionBankEditContent(Question question, QuestionBank bank, UserContext user) {
        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép chỉnh sửa câu hỏi");
            case SYSTEM_ADMIN -> {
                // Admin can edit draft/revision content
                // Policy: allow admin direct content edit
            }
            case TEACHER -> {
                if (!question.getCreatedBy().equals(user.userId())) {
                    throw new ForbiddenException("Chỉ tác giả mới được chỉnh sửa nội dung câu hỏi");
                }
                // DRAFT and REVISION_REQUESTED are editable (already checked above)
                // REJECTED: policy allows edit rejected
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
                // School can edit draft/revision/rejected
            }
        }
    }

    // ==================== REVIEW ACTION HELPERS ====================

    private void checkSubmitForReview(Question question, QuestionTopic topic, QuestionBank bank, UserContext user) {
        // Status check
        if (question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED
                && question.getStatus() != QuestionStatus.REJECTED) {
            throw new ForbiddenException("Không thể gửi duyệt câu hỏi ở trạng thái " + question.getStatus().name());
        }

        // Bank/Topic not archived, question not locked
        checkBankTopicNotArchived(topic, bank);
        if (question.isLocked()) {
            throw new ForbiddenException("Câu hỏi đang bị khóa");
        }

        // Role check
        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép gửi duyệt câu hỏi");
            case TEACHER -> {
                if (!question.getCreatedBy().equals(user.userId())) {
                    throw new ForbiddenException("Chỉ tác giả mới được gửi duyệt câu hỏi");
                }
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    private void checkRequestRevision(Question question, QuestionBank bank, UserContext user) {
        // Only SUBMITTED_FOR_REVIEW can be revision-requested
        if (question.getStatus() != QuestionStatus.SUBMITTED_FOR_REVIEW) {
            throw new ForbiddenException("Chỉ có thể yêu cầu sửa đổi câu hỏi đang chờ duyệt");
        }

        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép yêu cầu sửa đổi");
            case TEACHER -> {
                // Teacher must be a reviewer (not author, same school, school-owned bank)
                if (question.getCreatedBy().equals(user.userId())) {
                    throw new ForbiddenException("Tác giả không thể yêu cầu sửa đổi câu hỏi của chính mình");
                }
                if (!isTeacherReviewer(question, bank, user)) {
                    throw new ForbiddenException("Bạn không phải người được giao duyệt câu hỏi này");
                }
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    private void checkApprove(Question question, QuestionBank bank, UserContext user) {
        // Only SUBMITTED_FOR_REVIEW can be approved
        if (question.getStatus() != QuestionStatus.SUBMITTED_FOR_REVIEW) {
            throw new ForbiddenException("Chỉ có thể phê duyệt câu hỏi đang chờ duyệt");
        }

        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép phê duyệt câu hỏi");
            case TEACHER -> {
                if (!isTeacherReviewer(question, bank, user)) {
                    throw new ForbiddenException("Bạn không phải người được giao duyệt câu hỏi này");
                }
                // Self-approve check: reviewer must not be author
                if (question.getCreatedBy().equals(user.userId())) {
                    throw new ForbiddenException("Tác giả không thể tự phê duyệt câu hỏi của mình");
                }
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    private void checkReject(Question question, QuestionBank bank, UserContext user) {
        // Only SUBMITTED_FOR_REVIEW can be rejected
        if (question.getStatus() != QuestionStatus.SUBMITTED_FOR_REVIEW) {
            throw new ForbiddenException("Chỉ có thể từ chối câu hỏi đang chờ duyệt");
        }

        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép từ chối câu hỏi");
            case TEACHER -> {
                if (!isTeacherReviewer(question, bank, user)) {
                    throw new ForbiddenException("Bạn không phải người được giao duyệt câu hỏi này");
                }
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    private void checkPublish(Question question, QuestionTopic topic, QuestionBank bank, UserContext user) {
        // Only APPROVED can be published
        if (question.getStatus() != QuestionStatus.APPROVED) {
            throw new ForbiddenException("Chỉ có thể xuất bản câu hỏi đã được phê duyệt");
        }

        // Bank/Topic not archived, question not locked
        checkBankTopicNotArchived(topic, bank);
        if (question.isLocked()) {
            throw new ForbiddenException("Câu hỏi đang bị khóa");
        }

        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép xuất bản câu hỏi");
            case TEACHER -> {
                // Teacher needs publisher permission - for now, only if they're the author
                // TODO: implement publisher permission check
                if (!question.getCreatedBy().equals(user.userId())) {
                    throw new ForbiddenException("Bạn không có quyền xuất bản câu hỏi này");
                }
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    private void checkArchive(Question question, QuestionBank bank, UserContext user) {
        // Already archived
        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new ForbiddenException("Câu hỏi đã được lưu trữ");
        }

        switch (user.role()) {
            case STUDENT -> throw new ForbiddenException("Học sinh không được phép lưu trữ câu hỏi");
            case TEACHER -> {
                // Teacher can archive if they're the author and question is not published
                if (!question.getCreatedBy().equals(user.userId())) {
                    throw new ForbiddenException("Chỉ tác giả mới được lưu trữ câu hỏi");
                }
                if (question.getStatus() == QuestionStatus.PUBLISHED) {
                    throw new ForbiddenException("Không thể lưu trữ câu hỏi đã xuất bản");
                }
            }
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    private void checkRestore(Question question, QuestionBank bank, UserContext user) {
        // Only ARCHIVED can be restored
        if (question.getStatus() != QuestionStatus.ARCHIVED) {
            throw new ForbiddenException("Chỉ có thể khôi phục câu hỏi đã lưu trữ");
        }

        switch (user.role()) {
            case STUDENT, TEACHER -> throw new ForbiddenException("Bạn không có quyền khôi phục câu hỏi");
            case SCHOOL_ADMIN -> {
                if (!isSchoolBankOwner(bank, user)) {
                    throw new ForbiddenException("Trường không sở hữu ngân hàng câu hỏi này");
                }
            }
            case SYSTEM_ADMIN -> { /* allowed */ }
        }
    }

    // ==================== UTILITY METHODS ====================

    private boolean isSchoolBankOwner(QuestionBank bank, UserContext user) {
        return bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
                && user.schoolId() != null
                && user.schoolId().equals(bank.getSchoolId());
    }

    private boolean isTeacherReviewer(Question question, QuestionBank bank, UserContext user) {
        // Reviewer rule: TEACHER, question submitted, not author, school-owned bank, same school
        return user.role() == UserRole.TEACHER
                && question.getStatus() == QuestionStatus.SUBMITTED_FOR_REVIEW
                && question.getVisibility() == QuestionVisibility.REVIEWER_ONLY
                && !question.getCreatedBy().equals(user.userId())
                && bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
                && user.schoolId() != null
                && user.schoolId().equals(bank.getSchoolId());
    }

    private void checkBankTopicNotArchived(QuestionTopic topic, QuestionBank bank) {
        if (bank.getStatus() == QuestionBankStatus.ARCHIVED) {
            throw new ForbiddenException("Ngân hàng câu hỏi đã lưu trữ");
        }
        if (topic.getStatus() == QuestionTopicStatus.ARCHIVED) {
            throw new ForbiddenException("Chủ đề đã lưu trữ");
        }
    }
}
