package com.sep.vox.interfaces.rest.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.query.GetTurnUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.examturn.GetTurnUploadUrlUseCase;
import com.sep.vox.application.response.input.examturn.TurnUploadUrlResponse;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

/**
 * Cấp URL upload audio lượt THI cho Python, song song với
 * {@code /internal/practice-sessions/{id}/turns/{turnOrder}/upload-url} bên luyện tập.
 *
 * <p>Vì sao cần bản nội bộ trong khi đã có {@code /api/v1/exam-turns/upload-url}: route kia gắn
 * {@code @PreAuthorize("hasRole('STUDENT')")} nên chỉ máy học sinh gọi được. Từ nay audio lượt
 * thi do Python upload chứ không phải WPF -- đúng mô hình đã chọn cho luyện tập, nơi
 * {@code practice_session_client} ghi rõ lý do bác bỏ mô hình cũ: nó phụ thuộc đường mạng của
 * chính học sinh, còn pod lên S3 thì nằm trong cùng AWS.
 *
 * <p>Cả hai route dùng CHUNG {@link GetTurnUploadUrlUseCase}, nên khoá S3 và TTL không thể lệch
 * nhau giữa hai đường.
 *
 * <p>Bảo vệ bằng {@code X-Internal-Secret}
 * ({@code com.sep.vox.infrastructure.filter.PracticeInternalSecretFilter}), không phải JWT.
 */
@RestController
@RequestMapping("/internal/exam-turns")
public class ExamTurnInternalController {

    private final GetTurnUploadUrlUseCase getTurnUploadUrlUseCase;

    public ExamTurnInternalController(GetTurnUploadUrlUseCase getTurnUploadUrlUseCase) {
        this.getTurnUploadUrlUseCase = getTurnUploadUrlUseCase;
    }

    @GetMapping("/{attemptAnswerId}/turns/{turnOrder}/upload-url")
    public ApiResponse<TurnUploadUrlResponse> turnUploadUrl(
            @PathVariable UUID attemptAnswerId,
            @PathVariable int turnOrder) {
        return ApiResponse.success(
            "OK",
            getTurnUploadUrlUseCase.execute(new GetTurnUploadUrlQuery(attemptAnswerId, turnOrder))
        );
    }
}
