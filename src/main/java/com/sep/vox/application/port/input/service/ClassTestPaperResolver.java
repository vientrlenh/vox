package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;

/**
 * Chọn mã đề để thao tác trên bài kiểm tra trên lớp.
 *
 * <p>Từ khi giáo viên soạn được nhiều mã đề cho một bài trên lớp, "lấy đề đầu tiên" không còn là
 * phép đoán an toàn: nó sẽ sửa nhầm mã đề mà người dùng không hề biết. Nên bỏ trống {@code paperId}
 * chỉ hợp lệ khi bài có đúng một mã đề.
 */
@Service
public class ClassTestPaperResolver {

    private final AssessmentPaperRepository assessmentPaperRepository;

    public ClassTestPaperResolver(AssessmentPaperRepository assessmentPaperRepository) {
        this.assessmentPaperRepository = assessmentPaperRepository;
    }

    public AssessmentPaper resolve(UUID assessmentId, UUID requestedPaperId) {
        var papers = assessmentPaperRepository.findByAssessmentId(assessmentId);
        if (papers.isEmpty()) {
            throw new ResourceNotFoundException("Bài kiểm tra chưa có mã đề nào");
        }
        if (requestedPaperId != null) {
            return papers.stream()
                .filter(paper -> paper.getId().equals(requestedPaperId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã đề trong bài kiểm tra này"));
        }
        if (papers.size() > 1) {
            throw new IllegalStateException("Bài kiểm tra có nhiều mã đề, phải chỉ rõ mã đề cần thao tác");
        }
        return papers.get(0);
    }
}
