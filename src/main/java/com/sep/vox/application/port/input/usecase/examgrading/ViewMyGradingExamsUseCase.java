package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingExamOptionInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamKind;

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
public class ViewMyGradingExamsUseCase implements IUseCase<Void, List<GradingExamOptionInfo>> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewMyGradingExamsUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingExamOptionInfo> execute(Void input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        return examGradingQueryRepository.findExamsWithTasksByTeacherId(
            currentUserId, ExamKind.CENTRALIZED.name());
    }
}
