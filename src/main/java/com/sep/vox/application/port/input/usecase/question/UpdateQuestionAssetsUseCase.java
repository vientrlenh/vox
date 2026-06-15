package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
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

        var existingAssets = questionAssetRepository.findByQuestionId(input.questionId());
        if (existingAssets.isEmpty()) {
            throw new NotFoundException("Cau hoi chua co tai san de cap nhat");
        }

        ensureUniqueOrders(input);

        var existingById = existingAssets.stream()
            .filter(asset -> asset.getId() != null)
            .collect(Collectors.toMap(QuestionAsset::getId, Function.identity()));
        var incomingIds = input.assets().stream()
            .map(UpdateQuestionAssetsCommand.AssetItem::id)
            .filter(id -> id != null)
            .toList();
        if (incomingIds.size() != new HashSet<>(incomingIds).size()) {
            throw new DuplicatedException("Danh sach tai san co id bi trung lap");
        }
        var unknownIds = incomingIds.stream()
            .filter(id -> !existingById.containsKey(id))
            .toList();
        if (!unknownIds.isEmpty()) {
            throw new NotFoundException("Khong tim thay tai san cau hoi de cap nhat");
        }

        var incomingIdSet = new HashSet<>(incomingIds);
        existingAssets.stream()
            .filter(asset -> asset.getId() != null && !incomingIdSet.contains(asset.getId()))
            .forEach(asset -> questionAssetRepository.deleteById(asset.getId()));

        var tempOrderBase = Math.max(existingAssets.size(), input.assets().size()) + 10_000;
        var retainedAssets = new ArrayList<QuestionAsset>();
        for (int index = 0; index < input.assets().size(); index++) {
            var item = input.assets().get(index);
            if (item.id() == null) {
                continue;
            }
            var asset = existingById.get(item.id());
            asset.setOrder(tempOrderBase + index);
            retainedAssets.add(asset);
        }
        if (!retainedAssets.isEmpty()) {
            questionAssetRepository.saveAll(retainedAssets);
        }
        questionAssetRepository.flush();

        var now = OffsetDateTime.now();
        var assetsToSave = new ArrayList<QuestionAsset>();
        for (var item : input.assets()) {
            if (item.id() != null) {
                var asset = existingById.get(item.id());
                asset.setTitle(item.title());
                asset.setDurationSeconds(item.durationSeconds());
                asset.setAltText(item.altText());
                asset.setType(QuestionAssetType.valueOf(item.type()));
                asset.setUrl(item.url());
                asset.setTranscript(item.transcript());
                asset.setDescription(item.description());
                asset.setOrder(item.order());
                assetsToSave.add(asset);
                continue;
            }
            assetsToSave.add(new QuestionAsset(
                input.questionId(),
                item.title(),
                item.durationSeconds(),
                item.altText(),
                QuestionAssetType.valueOf(item.type()),
                item.url(),
                item.transcript(),
                item.description(),
                item.order()
            ));
        }
        questionAssetRepository.saveAll(assetsToSave);

        question.setUpdatedAt(now);
        question.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question.getId());
    }

    private void ensureUniqueOrders(UpdateQuestionAssetsCommand input) {
        var orders = input.assets().stream()
            .map(UpdateQuestionAssetsCommand.AssetItem::order)
            .toList();
        if (orders.size() != new HashSet<>(orders).size()) {
            throw new DuplicatedException("Danh sach tai san co thu tu bi trung lap");
        }
    }
}
