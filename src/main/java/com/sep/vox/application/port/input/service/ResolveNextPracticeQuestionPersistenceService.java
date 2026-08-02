package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.repository.personalization.PracticePaperItemRepository;
import com.sep.vox.domain.repository.personalization.StudentQuestionExposureRepository;

/** Phase 3 (ghi kết quả) của ResolveNextPracticeQuestionUseCase -- bean riêng vì self-invocation
 * trong cùng class sẽ bỏ qua proxy AOP, @Transactional sẽ không có tác dụng nếu gọi qua this. */
@Service
public class ResolveNextPracticeQuestionPersistenceService {

    private final PracticePaperItemRepository paperItemRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;

    public ResolveNextPracticeQuestionPersistenceService(
            PracticePaperItemRepository paperItemRepository,
            StudentQuestionExposureRepository studentQuestionExposureRepository) {
        this.paperItemRepository = paperItemRepository;
        this.studentQuestionExposureRepository = studentQuestionExposureRepository;
    }

    @Transactional
    public void persist(
            UUID studentId,
            UUID practicePaperId,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        paperItemRepository.save(new PracticePaperItem(
            UUID.randomUUID(),
            practicePaperId,
            selection.question().getId(),
            selection.slot(),
            selection.criterion(),
            selection.subAttribute(),
            selection.targetRank()
        ));
        studentQuestionExposureRepository.recordExposure(studentId, selection.question().getId());
    }
}
