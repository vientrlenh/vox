package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.StudentQuestionExposureRepository;

/** Phase 3 (write the result) of ResolveNextPracticeQuestionUseCase -- a separate bean because self-invocation
 * within the same class bypasses the AOP proxy, so @Transactional would have no effect if called through this. */
@Service
@RequiredArgsConstructor
public class ResolveNextPracticeQuestionPersistenceService {

    private final PracticePaperItemRepository paperItemRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;

    @Transactional
    public void persist(
            UUID studentId,
            UUID practicePaperId,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        paperItemRepository.save(PracticePaperItem.forSlot(
                practicePaperId,
                selection.question().getId(),
                selection.slot(),
                selection.criterion(),
                selection.subAttribute(),
                selection.targetRank()));
        studentQuestionExposureRepository.recordExposure(studentId, selection.question().getId());
    }
}
