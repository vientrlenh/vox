package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingAssessmentOptionInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Danh sách kỳ thi cho bộ lọc của hàng đợi giáo viên — CHỈ những kỳ thi người gọi đang
 * hoặc đã có phân công, không phải mọi kỳ thi của trường.
 *
 * <p>Không đi qua lối đọc kỳ thi chung được: ở đó điều kiện nhìn thấy là system admin /
 * school admin / thành viên kỳ thi / kỳ thi đã đóng, mà giáo viên chấm bài không phải
 * thành viên kỳ thi tập trung — quyền của họ đến từ chính dòng phân công. Nên phạm vi
 * dropdown cũng phải suy ra từ đó, và nó tự đóng đúng bằng tập bài họ được giao.
 *
 * <p>CHỈ kỳ thi tập trung, khoá cứng như {@link ViewMyGradingTasksUseCase}: hai chỗ phải
 * lọc trên cùng một tập, nếu không dropdown liệt kê ra kỳ thi mà hàng đợi trả về rỗng.
 */
@Service
public class ViewMyGradingAssessmentsUseCase implements IUseCase<Void, List<GradingAssessmentOptionInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewMyGradingAssessmentsUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingAssessmentOptionInfo> execute(Void input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        return assessmentGradingQueryRepository.findAssessmentsWithTasksByTeacherId(
            currentUserId, AssessmentKind.CENTRALIZED.name());
    }
}
