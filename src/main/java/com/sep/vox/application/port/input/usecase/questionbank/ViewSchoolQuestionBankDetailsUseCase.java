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
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolQuestionBankDetailsUseCase implements IUseCase<ViewQuestionBankDetailsQuery, QuestionBankDto> {

    private final QuestionBankReadQueryRepository questionBankReadQueryRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolQuestionBankDetailsUseCase(
            QuestionBankReadQueryRepository questionBankReadQueryRepository,
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.questionBankReadQueryRepository = questionBankReadQueryRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankDto execute(ViewQuestionBankDetailsQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));
        return questionBankReadQueryRepository.findSchoolQuestionBank(input.id(), getSchoolId(user.getId()))
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÃ¢n hÃ ng cÃ¢u há»i"));
    }

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new IllegalStateException("Nguoi dung hien tai khong thuoc truong nao"));
    }
}
