package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpdateQuestionAssetsUseCase implements IUseCase<UpdateQuestionAssetsCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionPermissionQuery permissionQuery;
    private final UserContextPort userContextPort;

    public UpdateQuestionAssetsUseCase(
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionPermissionQuery permissionQuery,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.permissionQuery = permissionQuery;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionAssetsCommand input) {
        if (!permissionQuery.canEditContent(input.questionId())) {
            throw new ForbiddenException("Khong co quyen chinh sua tai san cau hoi");
        }

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi"));

        if (questionAssetRepository.findByQuestionId(input.questionId()).isEmpty()) {
            throw new NotFoundException("Cau hoi chua co tai san de cap nhat");
        }

        questionAssetRepository.deleteByQuestionId(input.questionId());

        var now = OffsetDateTime.now();
        var newAssets = new ArrayList<QuestionAsset>();
        for (var item : input.assets()) {
            var asset = new QuestionAsset(
                UUID.randomUUID(),
                input.questionId(),
                item.title(),
                item.durationSeconds(),
                item.altText(),
                QuestionAssetType.valueOf(item.type()),
                item.url(),
                item.transcript(),
                item.description(),
                item.order()
            );
            newAssets.add(asset);
        }
        questionAssetRepository.saveAll(newAssets);

        question.setUpdatedAt(now);
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question.getId());
    }
}
