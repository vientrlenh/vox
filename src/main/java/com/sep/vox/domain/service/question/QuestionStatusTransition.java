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
 * {@code failed[].reason} của response bulk. Vì màn hình duyệt hàng loạt hay từ chối hàng chục câu
 * cùng lúc, mỗi lý do phải tự nó nói được <em>vì sao câu này bị bỏ qua</em> (đang ở trạng thái nào,
 * cần trạng thái nào) chứ không chỉ "không hợp lệ" — nếu không người dùng không biết phải sửa gì.
 * Kèm theo là {@link RejectionCode} để client gom nhóm các câu cùng lý do mà không phải so khớp
 * chuỗi tiếng Việt.
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

    /**
     * Mã lý do ổn định cho client. Dùng để gom nhóm / dịch lại phía UI; text tiếng Việt trong
     * {@link Rejection#reason()} có thể đổi mà không làm hỏng client.
     */
    public enum RejectionCode {
        /** Không tìm thấy câu hỏi (chỉ dùng ở đường đi hàng loạt). */
        QUESTION_NOT_FOUND,
        /** Không tìm thấy ngân hàng câu hỏi của câu này. */
        QUESTION_BANK_NOT_FOUND,
        /** Action gửi lên không nằm trong danh sách hỗ trợ. */
        INVALID_ACTION,
        /** Không đủ quyền trên ngân hàng / câu hỏi. */
        NO_PERMISSION,
        /** Tác giả tự duyệt bài của chính mình. */
        SELF_REVIEW,
        /** Action chỉ dành cho quản trị viên của ngân hàng câu hỏi. */
        ADMIN_ONLY,
        /** Trạng thái hiện tại không cho phép action. */
        INVALID_STATUS,
        /** Action bắt buộc phải có lý do đi kèm. */
        NOTE_REQUIRED,
        /** Khóa một câu đã bị khóa. */
        ALREADY_LOCKED,
        /** Mở khóa một câu chưa bị khóa. */
        NOT_LOCKED
    }

    public record Rejection(RejectionKind kind, RejectionCode code, String reason) {
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
            return invalidAction();
        }

        var owner = Objects.equals(actor.userId(), question.getCreatedBy());
        var systemAdminOnSystemBank = actor.systemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        var schoolAdminOnSchoolBank = bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
            && actor.schoolAdmin()
            && actor.schoolId() != null
            && actor.schoolId().equals(bank.getSchoolId());

        return switch (action) {
            case "SUBMIT" -> firstRejection(
                owner || editorCollaborator
                    ? null
                    : forbidden(RejectionCode.NO_PERMISSION, action,
                        "bạn không phải người tạo hoặc người cộng tác có quyền sửa câu hỏi này"),
                requireStatus(question.getStatus(), action,
                    QuestionStatus.DRAFT, QuestionStatus.REVISION_REQUESTED));
            case "APPROVE" -> firstRejection(
                reviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner, action),
                requireStatus(question.getStatus(), action, QuestionStatus.SUBMITTED_FOR_REVIEW));
            case "REJECT" -> firstRejection(
                reviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner, action),
                requireStatus(question.getStatus(), action, QuestionStatus.SUBMITTED_FOR_REVIEW),
                requireNote(note, action));
            case "REQUEST_REVISION" -> firstRejection(
                reviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner, action),
                requireStatus(question.getStatus(), action, QuestionStatus.SUBMITTED_FOR_REVIEW),
                requireNote(note, action));
            case "PUBLISH" -> {
                // Xuất bản lại một câu đã lưu trữ là thao tác của admin; xuất bản lần đầu thì
                // người sở hữu / collaborator cũng làm được, miễn câu đã được duyệt.
                if (question.getStatus() == QuestionStatus.ARCHIVED) {
                    yield adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, action);
                }
                var statusRejection = requireStatus(question.getStatus(), action, QuestionStatus.APPROVED);
                if (statusRejection != null) {
                    yield statusRejection;
                }
                yield owner || editorCollaborator || systemAdminOnSystemBank || schoolAdminOnSchoolBank
                    ? null
                    : forbidden(RejectionCode.NO_PERMISSION, action, "bạn không có quyền xuất bản câu hỏi này");
            }
            case "ARCHIVE" -> firstRejection(
                adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, action),
                requireStatus(question.getStatus(), action, QuestionStatus.PUBLISHED));
            case "REOPEN" -> firstRejection(
                adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, action),
                requireStatus(question.getStatus(), action, QuestionStatus.ARCHIVED));
            case "LOCK" -> {
                var permission = adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, action);
                if (permission != null) {
                    yield permission;
                }
                yield question.isLocked()
                    ? new Rejection(RejectionKind.INVALID_STATE, RejectionCode.ALREADY_LOCKED,
                        prefix(action) + "câu hỏi đã bị khóa")
                    : null;
            }
            case "UNLOCK" -> {
                var permission = adminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, action);
                if (permission != null) {
                    yield permission;
                }
                yield question.isLocked()
                    ? null
                    : new Rejection(RejectionKind.INVALID_STATE, RejectionCode.NOT_LOCKED,
                        prefix(action) + "câu hỏi chưa bị khóa");
            }
            default -> invalidAction();
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

    /** Nhãn tiếng Việt của trạng thái — khớp với nhãn trên UI để hai bên không nói khác nhau. */
    public static String statusLabel(QuestionStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case SUBMITTED_FOR_REVIEW -> "Chờ duyệt";
            case REVISION_REQUESTED -> "Yêu cầu sửa";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
            case PUBLISHED -> "Đã xuất bản";
            case ARCHIVED -> "Lưu trữ";
        };
    }

    private static Rejection firstRejection(Rejection... rejections) {
        for (var rejection : rejections) {
            if (rejection != null) {
                return rejection;
            }
        }
        return null;
    }

    private static Rejection adminPermission(
            boolean systemAdminOnSystemBank, boolean schoolAdminOnSchoolBank, String action) {
        return systemAdminOnSystemBank || schoolAdminOnSchoolBank
            ? null
            : forbidden(RejectionCode.ADMIN_ONLY, action,
                "chỉ quản trị viên của ngân hàng câu hỏi mới thực hiện được");
    }

    /**
     * Người tự viết câu hỏi không được tự duyệt bài của mình, kể cả khi họ là collaborator
     * có quyền sửa — chỉ admin hoặc một collaborator khác mới review được.
     */
    private static Rejection reviewPermission(
            boolean systemAdminOnSystemBank,
            boolean schoolAdminOnSchoolBank,
            boolean editorCollaborator,
            boolean owner,
            String action) {
        if (systemAdminOnSystemBank || schoolAdminOnSchoolBank) {
            return null;
        }
        if (editorCollaborator && !owner) {
            return null;
        }
        // Tách riêng trường hợp tác giả tự duyệt: đây là lý do bị từ chối phổ biến nhất trên màn
        // hình duyệt hàng loạt, và "bạn không có quyền" khiến người dùng tưởng bị thiếu vai trò.
        return owner
            ? forbidden(RejectionCode.SELF_REVIEW, action, "bạn là người tạo câu hỏi này, cần người khác duyệt")
            : forbidden(RejectionCode.NO_PERMISSION, action, "bạn không có quyền duyệt câu hỏi trong ngân hàng này");
    }

    private static Rejection requireStatus(QuestionStatus actual, String action, QuestionStatus... allowed) {
        for (var status : allowed) {
            if (actual == status) {
                return null;
            }
        }
        return new Rejection(RejectionKind.INVALID_STATE, RejectionCode.INVALID_STATUS,
            prefix(action) + "câu hỏi đang ở trạng thái \"" + statusLabel(actual)
                + "\", thao tác này chỉ áp dụng cho câu hỏi ở trạng thái " + allowedLabels(allowed));
    }

    private static Rejection requireNote(String note, String action) {
        return note == null || note.isBlank()
            ? new Rejection(RejectionKind.INVALID_STATE, RejectionCode.NOTE_REQUIRED,
                prefix(action) + "thao tác này bắt buộc phải nhập lý do")
            : null;
    }

    private static Rejection forbidden(RejectionCode code, String action, String detail) {
        return new Rejection(RejectionKind.FORBIDDEN, code, prefix(action) + detail);
    }

    private static Rejection invalidAction() {
        return new Rejection(RejectionKind.INVALID_STATE, RejectionCode.INVALID_ACTION, "Action không hợp lệ");
    }

    private static String prefix(String action) {
        return "Không thể " + actionLabel(action) + ": ";
    }

    private static String actionLabel(String action) {
        return switch (action) {
            case "SUBMIT" -> "gửi duyệt";
            case "APPROVE" -> "duyệt";
            case "REJECT" -> "từ chối";
            case "REQUEST_REVISION" -> "yêu cầu chỉnh sửa";
            case "PUBLISH" -> "xuất bản";
            case "ARCHIVE" -> "lưu trữ";
            case "REOPEN" -> "mở lại";
            case "LOCK" -> "khóa";
            case "UNLOCK" -> "mở khóa";
            default -> action;
        };
    }

    private static String allowedLabels(QuestionStatus... allowed) {
        var labels = new StringBuilder();
        for (var index = 0; index < allowed.length; index++) {
            if (index > 0) {
                labels.append(index == allowed.length - 1 ? " hoặc " : ", ");
            }
            labels.append('"').append(statusLabel(allowed[index])).append('"');
        }
        return labels.toString();
    }
}
