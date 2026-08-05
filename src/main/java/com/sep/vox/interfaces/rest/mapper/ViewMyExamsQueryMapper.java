package com.sep.vox.interfaces.rest.mapper;

import java.util.Locale;
import java.util.Set;

import com.sep.vox.application.port.input.query.ViewMyExamsQuery;
import com.sep.vox.domain.model.exam.ExamKind;

public final class ViewMyExamsQueryMapper {

    /**
     * Chỉ có một mốc thời gian đáng để sắp xếp ở màn học sinh: ngày thi. Nhận nguyên chuỗi
     * {@code "examDate,desc"} để FE dùng chung cú pháp sort quen thuộc, nhưng không mở rộng
     * sang field khác vì response không có mốc nào khác.
     */
    private static final String SORT_FIELD = "examDate";

    private static final Set<String> ALLOWED_STATUSES = Set.of("upcoming", "in_progress", "completed");

    private ViewMyExamsQueryMapper() {
    }

    public static ViewMyExamsQuery fromRequest(ExamKind kind, String status, int page, int size, String sort) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }

        return new ViewMyExamsQuery(kind, normalizeStatus(status), page, size, parseSortDescending(sort));
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        var normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái bài thi không hợp lệ");
        }
        return normalized;
    }

    private static boolean parseSortDescending(String sort) {
        if (sort == null || sort.isBlank()) {
            return true;
        }

        var parts = sort.trim().split(",");
        if (parts.length > 2 || !SORT_FIELD.equalsIgnoreCase(parts[0].trim())) {
            throw new IllegalArgumentException("Tham số sắp xếp không hợp lệ");
        }
        if (parts.length == 1) {
            return true;
        }

        var direction = parts[1].trim().toLowerCase(Locale.ROOT);
        if ("desc".equals(direction)) {
            return true;
        }
        if ("asc".equals(direction)) {
            return false;
        }
        throw new IllegalArgumentException("Tham số sắp xếp không hợp lệ");
    }
}
