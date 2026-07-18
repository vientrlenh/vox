package com.sep.vox.application.port.input.usecase.examcandidateresult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyExamResultsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamCandidateResultDto;
import com.sep.vox.domain.mapper.ExamCandidateResultDtoMapper;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;

@Service
public class ViewMyExamResultsUseCase implements IUseCase<ViewMyExamResultsQuery, PageResult<ExamCandidateResultDto>> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamResultsUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            UserContextPort userContextPort) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamCandidateResultDto> execute(ViewMyExamResultsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var result = examCandidateResultRepository.findByStudentId(currentUserId, input.page(), input.size());
        return ExamCandidateResultDtoMapper.toDtoPage(result);
    }
}
