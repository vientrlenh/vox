package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UnblockExamCandidateCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class UnblockExamCandidateUseCase implements IUseCase<UnblockExamCandidateCommand, ExamCandidateDto> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionModerationAccessService moderationAccessService;

    public UnblockExamCandidateUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamSessionModerationAccessService moderationAccessService) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.moderationAccessService = moderationAccessService;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(UnblockExamCandidateCommand input) {
        var candidate = examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        var exam = examRepository.findById(candidate.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của thí sinh"));

        moderationAccessService.authorize(exam, candidate);

        var now = OffsetDateTime.now();
        candidate.setBlockedAt(null);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(moderationAccessService.getCurrentUserId());
        return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
    }
}
