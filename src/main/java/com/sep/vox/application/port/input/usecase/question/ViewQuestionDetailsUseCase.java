package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionViewPermissionQuery;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ViewQuestionDetailsUseCase implements IUseCase<ViewQuestionDetailsQuery, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionViewPermissionQuery permissionQuery;

    public ViewQuestionDetailsUseCase(QuestionRepository questionRepository,
            QuestionViewPermissionQuery permissionQuery) {
        this.questionRepository = questionRepository;
        this.permissionQuery = permissionQuery;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDto execute(ViewQuestionDetailsQuery input) {
        if (!permissionQuery.canViewQuestionDetail(input.id())) {
            throw new ForbiddenException("Không có quyền xem câu hỏi này");
        }

        var question = questionRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        return QuestionDtoMapper.toDto(question);
    }
}
