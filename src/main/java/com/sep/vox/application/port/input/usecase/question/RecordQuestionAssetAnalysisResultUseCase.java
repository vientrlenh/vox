package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionAsset;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.QuestionAssetRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordQuestionAssetAnalysisResultUseCase
        implements IUseCase<RecordQuestionAssetAnalysisResultUseCase.Command, Void> {

    private final QuestionAssetRepository questionAssetRepository;

    @Override
    @Transactional
    public Void execute(Command input) {
        QuestionAsset asset = questionAssetRepository.findById(input.assetId()).orElse(null);
        if (asset == null) {
            // Legitimate race: the asset may have been deleted or the question replaced (cloned to another draft)
            // between publishing the request and the Python service returning the result -- not an error worth
            // a retry or the dead-letter topic.
            log.warn("Skipping the asset analysis result -- assetId={} no longer found", input.assetId());
            return null;
        }

        if (fillBlankFields(asset, input)) {
            questionAssetRepository.save(asset);
        }
        return null;
    }

    /**
     * Fills only the fields that are CURRENTLY blank -- content a person typed or AI wrote earlier always wins
     * and is never overwritten here. "Regenerate with AI" (RegenerateQuestionAssetAnalysisUseCase.resetForAnalysis())
     * blanks the field before publishing precisely so that this rule lets it be written again.
     *
     * @return whether any field was filled
     */
    private boolean fillBlankFields(QuestionAsset asset, Command input) {
        boolean changed = false;
        if (isBlank(asset.getTranscript()) && !isBlank(input.transcript())) {
            asset.setTranscript(input.transcript());
            changed = true;
        }
        if (isBlank(asset.getDescription()) && !isBlank(input.description())) {
            asset.setDescription(input.description());
            changed = true;
        }
        return changed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Command(UUID assetId, String transcript, String description) {
    }
}
