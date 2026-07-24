package com.sep.vox.application.port.input.usecase.examgrading;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.GradingItemScoreResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.application.response.input.examgrading.GradingPreviewResponse;

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
 * {@code SubmitGradingUseCase} nên tổng ở đây bằng đúng tổng khi nộp.
 */
@Service
public class PreviewGradingUseCase implements IUseCase<SubmitGradingCommand, GradingPreviewResponse> {

    private final ExamSessionResultCalculator examSessionResultCalculator;
    private final ExamGradingAccessService examGradingAccessService;
    private final GradingItemScoreResolver gradingItemScoreResolver;

    public PreviewGradingUseCase(
            ExamSessionResultCalculator examSessionResultCalculator,
            ExamGradingAccessService examGradingAccessService,
            GradingItemScoreResolver gradingItemScoreResolver) {
        this.examSessionResultCalculator = examSessionResultCalculator;
        this.examGradingAccessService = examGradingAccessService;
        this.gradingItemScoreResolver = gradingItemScoreResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingPreviewResponse execute(SubmitGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var context = examGradingAccessService.load(command.assignmentId());
        examGradingAccessService.authorizeAssignedTeacher(context, currentUserId);

        // Preview cho phép chấm dở để xem tổng chạy dần — KHÔNG bắt phủ đủ như /grade.
        var resolvedItems = gradingItemScoreResolver.resolve(context, command, false);
        var overrides = new LinkedHashMap<UUID, BigDecimal>();
        resolvedItems.forEach(item -> overrides.put(item.responseId(), item.itemScore()));

        var previewed = examSessionResultCalculator.preview(
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
