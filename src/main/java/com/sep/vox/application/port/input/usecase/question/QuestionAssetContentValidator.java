package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionAssetType;

/**
 * Kiểm tra một asset có đủ nội dung để AI làm việc được không.
 *
 * <p><b>Vì sao bắt buộc</b>: Python KHÔNG hề nhìn thấy tấm ảnh hay nghe đoạn audio --
 * {@code QuestionAssetContext} chỉ mang {@code {type, transcript, description, alt_text}}, toàn
 * chữ. Và nhánh AI tự phân tích asset đang tắt (auto-publish bị bỏ ở cả Create lẫn Update), nên
 * những dòng chữ này là <b>toàn bộ</b> hiểu biết của AI về tài nguyên.
 *
 * <p>Để trống thì mỗi lượt hỏi, prompt nhận đúng chuỗi {@code "Asset details: unavailable"} --
 * follow-up ra câu chung chung không bám được chi tiết nào, và {@code ValidityNode} chấm on-topic
 * mà không biết đề nói về cái gì. Hỏng lặng lẽ: không lỗi, không log, chỉ là điểm sai.
 *
 * <p>Đặt ở một chỗ thay vì chép vào cả Create lẫn Update: hai bản chép sẽ lệch nhau, và lệch ở
 * đây nghĩa là sửa được asset thành trạng thái mà tạo mới thì bị chặn.
 */
public final class QuestionAssetContentValidator {

    private QuestionAssetContentValidator() {
    }

    public static void validate(
            QuestionAssetType type,
            String url,
            String transcript,
            String description,
            String altText) {
        if (type == QuestionAssetType.TEXT_PASSAGE) {
            if (isBlank(transcript)) {
                throw new IllegalArgumentException("Nội dung đoạn văn không được để trống");
            }
            return;
        }

        if (isBlank(url)) {
            throw new IllegalArgumentException("URL tài nguyên không được để trống");
        }

        if (type == QuestionAssetType.IMAGE) {
            if (isBlank(description) && isBlank(altText)) {
                throw new IllegalArgumentException(
                    "Ảnh phải có mô tả nội dung (hoặc alt text). AI không nhìn thấy ảnh — "
                        + "nó chỉ biết ảnh qua phần mô tả này, để trống thì AI sẽ hỏi và chấm chung chung.");
            }
            return;
        }

        if (isBlank(transcript)) {
            throw new IllegalArgumentException(
                "Audio/video phải có transcript. AI không nghe được tệp — "
                    + "nó chỉ biết nội dung qua transcript này, để trống thì AI sẽ hỏi và chấm chung chung.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
