package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class UpdateQuestionAssetsUseCase implements IUseCase<UpdateQuestionAssetsCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public UpdateQuestionAssetsUseCase(
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionAssetsCommand input) {
        var user = permissionChecker.resolveCurrentUser();

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var topic = questionTopicRepository.findById(question.getQuestionTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề"));
        var bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        permissionChecker.checkCanEditContent(question, topic, bank, user);

        // Delete existing assets
        questionAssetRepository.deleteByQuestionId(input.questionId());

        // Create new assets
        OffsetDateTime now = OffsetDateTime.now();
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
        question.setUpdatedBy(user.userId());
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question.getId());
    }
}
