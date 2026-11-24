package com.sep.vox.interfaces.rest.mapper;

import java.util.Locale;
import java.util.Set;

import com.sep.vox.application.port.input.query.assessment.ViewMyAssessmentsQuery;
import com.sep.vox.domain.model.assessment.AssessmentKind;

public final class ViewMyAssessmentsQueryMapper {

    /**
     * There is only one time field worth sorting by on the student screen: the assessment date. It takes the whole string
     * {@code "assessmentDate,desc"} so the FE can share the familiar sort syntax, but does not extend
     * to other fields because the response has no other time field.
     */
    private static final String SORT_FIELD = "assessmentDate";

    private static final Set<String> ALLOWED_STATUSES = Set.of("upcoming", "in_progress", "completed");

    private ViewMyAssessmentsQueryMapper() {
    }

    /**
     * {@code page}/{@code size} đã được {@code PageArguments.validate} kiểm ở controller, nên ở đây
     * không kiểm lại: một luật hai chỗ giữ là cách chắc chắn để hai chỗ trôi khỏi nhau.
     */
    public static ViewMyAssessmentsQuery fromRequest(AssessmentKind kind, String status, int page, int size, String sort) {
        return new ViewMyAssessmentsQuery(kind, normalizeStatus(status), page, size, parseSortDescending(sort));
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái bài thi không hợp lệ");
        }
        return normalized;
    }

    private static boolean parseSortDescending(String sort) {
        if (sort == null || sort.isBlank()) {
            return true;
        }

        String[] parts = sort.trim().split(",");
        if (parts.length > 2 || !SORT_FIELD.equalsIgnoreCase(parts[0].trim())) {
            throw new IllegalArgumentException("Tham số sắp xếp không hợp lệ");
        }
        if (parts.length == 1) {
            return true;
        }

        String direction = parts[1].trim().toLowerCase(Locale.ROOT);
        if ("desc".equals(direction)) {
            return true;
        }
        if ("asc".equals(direction)) {
            return false;
        }
        throw new IllegalArgumentException("Tham số sắp xếp không hợp lệ");
    }
}
