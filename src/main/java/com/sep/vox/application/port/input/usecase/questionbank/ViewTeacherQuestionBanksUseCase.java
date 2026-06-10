package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionBankReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewTeacherQuestionBanksUseCase implements IUseCase<ViewTeacherQuestionBanksQuery, PageResult<QuestionBankDto>> {

    private final QuestionBankReadQueryRepository questionBankReadQueryRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public ViewTeacherQuestionBanksUseCase(
            QuestionBankReadQueryRepository questionBankReadQueryRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.questionBankReadQueryRepository = questionBankReadQueryRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionBankDto> execute(ViewTeacherQuestionBanksQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        return questionBankReadQueryRepository.findTeacherQuestionBanks(
            userId, user.getSchoolId(), new PageRequest(input.page(), input.size()));
    }
}
