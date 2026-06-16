package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAdminReviewQueueQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

@Service
public class ViewAdminReviewQueueUseCase implements IUseCase<ViewAdminReviewQueueQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final UserContextPort userContextPort;

    public ViewAdminReviewQueueUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            UserContextPort userContextPort) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewAdminReviewQueueQuery input) {
        return questionReadQueryRepository.findAdminReviewQueue(
                userContextPort.getCurrentAuthenticatedUserId(),
                new PageRequest(input.page(), input.size()));
    }
}
