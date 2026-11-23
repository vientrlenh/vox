package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator;
import com.sep.vox.application.response.input.assessmentgrading.GradingPreviewResponse;

/**
 * Tính thử tổng điểm cho bộ điểm giáo viên đang nhập, KHÔNG ghi gì.
 *
 * <p>Tồn tại vì công thức tổng của BE là weighted hai tầng
 * ({@code section = Σ(itemScore × paperItem.weight)}, rồi
 * {@code total = Σ(section × section.weight) / Σ(section.weight)}, làm tròn 2 chữ
 * số) — FE không tự tính khớp được, và tuyệt đối không được sửa calculator cho
 * khớp FE vì nó dùng chung với luồng AI lẫn phúc khảo.
 *
 * <p>Là POST nhưng chỉ đọc: body quá lớn cho một GraphQL query. {@code readOnly}
 * là hàng rào cuối — validate và quy đổi điểm đi chung đường với
 * {@code RegradeResultUseCase} nên tổng ở đây bằng đúng tổng khi nộp.
 */
@Service
@RequiredArgsConstructor
public class PreviewGradingUseCase implements IUseCase<SubmitGradingCommand, GradingPreviewResponse> {

    private final AssessmentSessionResultCalculator assessmentSessionResultCalculator;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingItemScoreResolver gradingItemScoreResolver;

    @Override
    @Transactional(readOnly = true)
    public GradingPreviewResponse execute(SubmitGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        AssessmentGradingAccessService.GradingContext context = assessmentGradingAccessService.load(command.assignmentId());
        assessmentGradingAccessService.authorizeAssignedTeacher(context, currentUserId);

        // Preview cho phép chấm dở để xem tổng chạy dần — KHÔNG bắt phủ đủ như /grade.
        List<GradingItemScoreResolver.ResolvedItem> resolvedItems = gradingItemScoreResolver.resolve(context, command, false);
        Map<UUID, BigDecimal> overrides = new LinkedHashMap<UUID, BigDecimal>();
        resolvedItems.forEach(item -> overrides.put(item.responseId(), item.itemScore()));

        AssessmentSessionResultCalculator.PreviewedAssessmentSessionResult previewed = assessmentSessionResultCalculator.preview(
            context.candidateResult().getSessionId(), overrides);

        return new GradingPreviewResponse(
            previewed.totalScore(),
            previewed.resultBandName(),
            previewed.items().stream()
                .map(item -> new GradingPreviewResponse.ItemScore(item.paperItemId(), item.itemScore()))
                .toList()
        );
    }
}
