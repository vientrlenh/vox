package com.sep.vox.domain.service.question;

import java.util.Objects;
import java.util.UUID;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionStatus;

/**
 * Quy tắc chuyển trạng thái câu hỏi — kiểm tra quyền và trạng thái hợp lệ cho từng action.
 *
 * <p>Cố ý là class thuần static, không dependency, KHÔNG ném exception: mọi lý do từ chối được
 * <em>trả về</em> dưới dạng {@link Rejection}. Đây là điểm mấu chốt chứ không phải sở thích code.
 * Đường đi cập nhật hàng loạt phải xử lý được từng câu hỏi lỗi rồi đi tiếp; nếu quy tắc này nằm
 * trong một bean được Spring proxy và ném exception, transaction interceptor sẽ đánh dấu
 * transaction của use case bulk là rollback-only ngay lần đầu có câu lỗi — vòng lặp bắt được
 * exception và chạy tiếp bình thường, nhưng đến lúc commit thì nổ {@code UnexpectedRollbackException}
 * (HTTP 500) và cuốn theo cả những câu đã cập nhật thành công. Không có proxy thì không thể tái
 * phát sinh lỗi đó.
 *
 * <p>Chuỗi thông báo ở đây là văn bản hướng tới người dùng: chúng được trả thẳng cho client trong
 * {@code failed[].reason} của response bulk.
 */
public final class QuestionStatusTransition {

    /**
     * Thông báo cho dữ liệu không tồn tại — dùng chung để endpoint đơn và endpoint hàng loạt nói
     * cùng một câu, dù một bên ném exception còn bên kia gom vào {@code failed[]}.
     */
    public static final String QUESTION_NOT_FOUND = "Không tìm thấy câu hỏi";
    public static final String QUESTION_BANK_NOT_FOUND = "Không tìm thấy ngân hàng câu hỏi";

    private QuestionStatusTransition() {
    }

    /**
     * Bối cảnh của người thực hiện, đã resolve sẵn một lần cho cả batch.
     *
     * @param schoolAdmin đã loại trừ system admin (giống hệt cách use case tính trước đây)
     */
    public record Actor(UUID userId, UUID schoolId, boolean systemAdmin, boolean schoolAdmin) {
    }

    /** Phân loại lý do từ chối để tầng application map ra đúng HTTP status cho endpoint đơn. */
    public enum RejectionKind {
        FORBIDDEN, INVALID_STATE
    }

    public record Rejection(RejectionKind kind, String reason) {
    }

    /**
     * Kiểm tra một action có được phép trên câu hỏi đã tồn tại hay không.
     *
     * <p>{@code question} và {@code bank} phải khác {@code null}: "dữ liệu có tồn tại không" là
     * việc của tầng gọi (nó mới biết nên ném {@code NotFoundException} hay gom vào {@code failed[]}),
     * còn ở đây chỉ còn thuần quy tắc nghiệp vụ.
     *
     * @return {@code null} nếu action được phép, ngược lại là lý do từ chối.
     */
    public static Rejection rejectionFor(
            Question question,
            QuestionBank bank,
            boolean editorCollaborator,
            Actor actor,
            String action,
            String note) {
        if (action == null) {
            return new Rejection(RejectionKind.INVALID_STATE, "Action không hợp lệ");
        }

        var owner = Objects.equals(actor.userId(), question.getCreatedBy());
        var systemAdminOnSystemBank = actor.systemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        var schoolAdminOnSchoolBank = bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
            && actor.schoolAdmin()
            && actor.schoolId() != null
            && actor.schoolId().equals(bank.getSchoolId());

        return switch (action) {
            case "SUBMIT" -> {
                if (!owner && !editorCollaborator) {
                    yield forbidden();
                }
                if (question.getStatus() != QuestionStatus.DRAFT
                        && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
                    yield new Rejection(RejectionKind.INVALID_STATE,
                        "Chỉ được submit khi câu hỏi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
                }
                yield null;
            }
            case "APPROVE" -> firstRejection(
                reviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner),
                requireStatus(question.getStatus(), QuestionStatus.SUBMITTED_FOR_REVIEW));
            case "REJECT" -> firstRejection(
                reviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner),
                requireStatus(question.getStatus(), QuestionStatus.SUBMITTED_FOR_REVIEW),
                requireNote(note, "REJECT"));
            case "REQUEST_REVISION" -> firstRejection(
                reviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner),
                requireStatus(question.getStatus(), QuestionStatus.SUBMITTED_FOR_REVIEW),
                requireNote(note, "REQUEST_REVISION"));
            case "PUBLISH" -> {
                // Xuất bản lại một câu đã lưu trữ là thao tác của admin; xuất bản lần đầu thì
                // người sở hữu / collaborator cũng làm được, miễn câu đã được duyệt.
                if (question.getStatus() == QuestionStatus.ARCHIVED) {
                    yield adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                }
                var statusRejection = requireStatus(question.getStatus(), QuestionStatus.APPROVED);
                if (statusRejection != null) {
                    yield statusRejection;
                }
                yield owner || editorCollaborator || systemAdminOnSystemBank || schoolAdminOnSchoolBank
                    ? null
                    : forbidden();
            }
            case "ARCHIVE" -> firstRejection(
                adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank),
                requireStatus(question.getStatus(), QuestionStatus.PUBLISHED));
            case "REOPEN" -> firstRejection(
                adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank),
                requireStatus(question.getStatus(), QuestionStatus.ARCHIVED));
            case "LOCK" -> {
                var permission = adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                if (permission != null) {
                    yield permission;
                }
                yield question.isLocked()
                    ? new Rejection(RejectionKind.INVALID_STATE, "Câu hỏi đã bị khóa")
                    : null;
            }
            case "UNLOCK" -> {
                var permission = adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                if (permission != null) {
                    yield permission;
                }
                yield question.isLocked()
                    ? null
                    : new Rejection(RejectionKind.INVALID_STATE, "Câu hỏi chưa bị khóa");
            }
            default -> new Rejection(RejectionKind.INVALID_STATE, "Action không hợp lệ");
        };
    }

    /**
     * Áp dụng action lên câu hỏi. Chỉ gọi khi {@link #rejectionFor} đã trả {@code null} —
     * hàm này không kiểm tra lại quyền hay trạng thái.
     */
    public static void apply(Question question, String action) {
        switch (action) {
            case "SUBMIT" -> question.setStatus(QuestionStatus.SUBMITTED_FOR_REVIEW);
            case "APPROVE" -> question.setStatus(QuestionStatus.APPROVED);
            case "REJECT" -> question.setStatus(QuestionStatus.REJECTED);
            case "REQUEST_REVISION" -> question.setStatus(QuestionStatus.REVISION_REQUESTED);
            case "PUBLISH" -> question.setStatus(QuestionStatus.PUBLISHED);
            case "ARCHIVE" -> question.setStatus(QuestionStatus.ARCHIVED);
            case "REOPEN" -> question.setStatus(QuestionStatus.DRAFT);
            case "LOCK" -> question.setLocked(true);
            case "UNLOCK" -> question.setLocked(false);
            default -> throw new IllegalArgumentException("Action không hợp lệ: " + action);
        }
    }

    private static Rejection firstRejection(Rejection... rejections) {
        for (var rejection : rejections) {
            if (rejection != null) {
                return rejection;
            }
        }
        return null;
    }

    private static Rejection adminPermission(boolean systemAdminOnSystemBank, boolean schoolAdminOnSchoolBank) {
        return systemAdminOnSystemBank || schoolAdminOnSchoolBank ? null : forbidden();
    }

    /**
     * Người tự viết câu hỏi không được tự duyệt bài của mình, kể cả khi họ là collaborator
     * có quyền sửa — chỉ admin hoặc một collaborator khác mới review được.
     */
    private static Rejection reviewPermission(
            boolean systemAdminOnSystemBank,
            boolean schoolAdminOnSchoolBank,
            boolean editorCollaborator,
            boolean owner) {
        if (systemAdminOnSystemBank || schoolAdminOnSchoolBank) {
            return null;
        }
        if (editorCollaborator && !owner) {
            return null;
        }
        return forbidden();
    }

    private static Rejection requireStatus(QuestionStatus actual, QuestionStatus expected) {
        return actual == expected
            ? null
            : new Rejection(RejectionKind.INVALID_STATE, "Trạng thái câu hỏi hiện tại không hợp lệ cho action này");
    }

    private static Rejection requireNote(String note, String action) {
        return note == null || note.isBlank()
            ? new Rejection(RejectionKind.INVALID_STATE, "Action " + action + " bắt buộc phải có note")
            : null;
    }

    private static Rejection forbidden() {
        return new Rejection(RejectionKind.FORBIDDEN, "Quyền truy cập bị từ chối");
    }
}
