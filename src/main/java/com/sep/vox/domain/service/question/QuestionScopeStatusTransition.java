package com.sep.vox.domain.service.question;

/**
 * Luật đổi trạng thái dùng chung cho NGÂN HÀNG và CHỦ ĐỀ câu hỏi.
 *
 * <p>Hai thực thể có cùng bộ trạng thái ({@code DRAFT / PUBLISHED / ARCHIVED}) và cùng luật, chỉ
 * khác kiểu enum. Nhận vào TÊN trạng thái thay vì enum để một hàm phục vụ được cả hai — đổi lại
 * mất kiểm tra kiểu, nên bên gọi luôn truyền {@code status.name()} chứ đừng dựng chuỗi bằng tay.
 *
 * <p>Hàm THUẦN, trả về lý do từ chối thay vì ném exception — bắt buộc cho luồng hàng loạt. Nếu ném,
 * mục đầu tiên bị từ chối sẽ làm Spring đánh dấu transaction rollback-only và nuốt luôn những mục
 * đã cập nhật thành công ở vòng lặp trước đó; {@code BulkUpdateQuestionStatusUseCase} đã dính đúng
 * cái bẫy này và javadoc của nó ghi lại đầy đủ.
 */
public final class QuestionScopeStatusTransition {

    public enum RejectionCode {
        NOT_FOUND,
        NO_PERMISSION,
        INVALID_STATUS,
        INVALID_ACTION
    }

    public record Rejection(RejectionCode code, String reason) {
    }

    public static final String NOT_FOUND_BANK = "Không tìm thấy ngân hàng câu hỏi";
    public static final String NOT_FOUND_TOPIC = "Không tìm thấy chủ đề câu hỏi";
    public static final String NO_PERMISSION = "Bạn không có quyền đổi trạng thái mục này";

    private QuestionScopeStatusTransition() {
    }

    /**
     * @param currentStatusName {@code status.name()} của mục đang xét
     * @param action {@code PUBLISH} hoặc {@code ARCHIVE}, đã chuẩn hoá hoa
     * @param label "ngân hàng câu hỏi" / "chủ đề câu hỏi", chỉ để ghép câu thông báo
     * @return {@code null} nếu được phép đổi
     */
    public static Rejection rejectionFor(String currentStatusName, String action, String label) {
        return switch (action == null ? "" : action) {
            case "PUBLISH" -> "DRAFT".equals(currentStatusName)
                ? null
                : new Rejection(
                    RejectionCode.INVALID_STATUS,
                    "Chỉ publish được " + label + " đang ở trạng thái DRAFT (hiện tại: "
                        + currentStatusName + ")");
            // Lưu trữ được từ mọi trạng thái, khớp đúng nhánh ARCHIVE của luồng đổi từng mục.
            case "ARCHIVE" -> null;
            default -> new Rejection(RejectionCode.INVALID_ACTION, "Action không hợp lệ");
        };
    }

    /** Tên trạng thái sau khi áp dụng {@code action}. Chỉ gọi khi {@link #rejectionFor} trả null. */
    public static String nextStatusName(String action) {
        return "PUBLISH".equals(action) ? "PUBLISHED" : "ARCHIVED";
    }
}
