package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.QuestionBankStatsDto;
import com.sep.vox.application.projection.repository.QuestionBankStatsQueryRepository;

/** Thống kê câu hỏi & ngân hàng câu hỏi của trường hiện tại — dùng cho dashboard school admin. */
@Service
public class ViewQuestionBankStatsUseCase implements IUseCase<Void, QuestionBankStatsDto> {

    private final QuestionBankStatsQueryRepository questionBankStatsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewQuestionBankStatsUseCase(
            QuestionBankStatsQueryRepository questionBankStatsQueryRepository,
            UserContextPort userContextPort) {
        this.questionBankStatsQueryRepository = questionBankStatsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankStatsDto execute(Void input) {
        UUID schoolId = userContextPort.getCurrentSchoolId()
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return questionBankStatsQueryRepository.countForSchool(schoolId);
    }
}
