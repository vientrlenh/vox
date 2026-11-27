package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.QuestionBankStatsDto;
import com.sep.vox.application.query.repository.QuestionBankStatsQueryRepository;

/** Statistics of questions & question banks of the current school -- used by the school admin dashboard. */
@Service
@RequiredArgsConstructor
public class ViewQuestionBankStatsUseCase implements IUseCase<Void, QuestionBankStatsDto> {

    private final QuestionBankStatsQueryRepository questionBankStatsQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public QuestionBankStatsDto execute(Void input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();
        return questionBankStatsQueryRepository.countForSchool(schoolId);
    }
}
