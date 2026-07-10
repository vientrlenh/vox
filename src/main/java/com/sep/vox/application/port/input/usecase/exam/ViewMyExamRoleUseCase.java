package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyExamRoleQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.ExamMemberRepository;

@Service
public class ViewMyExamRoleUseCase implements IUseCase<ViewMyExamRoleQuery, String> {

    private final ExamMemberRepository examMemberRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamRoleUseCase(ExamMemberRepository examMemberRepository, UserContextPort userContextPort) {
        this.examMemberRepository = examMemberRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(ViewMyExamRoleQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        return examMemberRepository.findByExamIdAndUserId(input.examId(), currentUserId)
            .map(member -> member.getRole().name())
            .orElse(null);
    }
}
