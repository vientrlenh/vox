package com.sep.vox.domain.service.exam;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Loại bài mà một màn chấm bài đang nói tới.
 *
 * <p>Kỳ thi tập trung và bài kiểm tra trên lớp là hai luồng chấm khác nhau — nhà trường
 * điều phối cái thứ nhất, giáo viên tạo bài tự chấm cái thứ hai — nên mỗi màn phải chốt
 * loại bài của mình. Client không nói gì thì hiểu là {@link ExamKind#CENTRALIZED}: bảng
 * điều phối của nhà trường là chỗ duy nhất chưa truyền tham số này, và bỏ trống ở đó
 * chính là lỗi trộn hai loại bài vào một danh sách.
 *
 * <p>Đặt ở {@code application/common} thay vì viết lại trong từng use case: mặc định là
 * một luật nghiệp vụ, viết ở hai nơi thì sớm muộn hai nơi lệch nhau.
 */
public final class GradingScopeKind {

    private GradingScopeKind() {
    }

    /**
     * @param value tên {@link ExamKind} lấy từ client, có thể null/rỗng
     * @return tên loại bài để lọc read model
     * @throws IllegalArgumentException khi chuỗi không phải một {@link ExamKind} — trả về
     *         mặc định lúc này là im lặng đưa nhầm dữ liệu của loại bài kia cho người dùng
     */
    public static String orCentralized(String value) {
        if (value == null || value.isBlank()) {
            return ExamKind.CENTRALIZED.name();
        }
        return ExamKind.valueOf(value.trim()).name();
    }
}
