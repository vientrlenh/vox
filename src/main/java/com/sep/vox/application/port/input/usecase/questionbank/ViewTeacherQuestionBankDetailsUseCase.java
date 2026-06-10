package com.sep.vox.application.port.input.usecase.questionbank;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionBankReadQueryRepository;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewTeacherQuestionBankDetailsUseCase implements IUseCase<ViewQuestionBankDetailsQuery, QuestionBankDto> {

    private final QuestionBankReadQueryRepository questionBankReadQueryRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public ViewTeacherQuestionBankDetailsUseCase(
            QuestionBankReadQueryRepository questionBankReadQueryRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.questionBankReadQueryRepository = questionBankReadQueryRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankDto execute(ViewQuestionBankDetailsQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        return questionBankReadQueryRepository.findTeacherQuestionBank(input.id(), userId, user.getSchoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));
    }
}
