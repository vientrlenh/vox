package com.sep.vox.application.port.input.usecase.interestdimension;

import com.sep.vox.application.response.input.interestdimension.InterestDimensionResponses;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.interestdimension.UpsertInterestDimensionCommand;
import com.sep.vox.application.port.input.service.InterestQuizBankTopUpService;
import com.sep.vox.application.response.input.interestdimension.InterestDimensionResponses.InterestDimensionResponse;
import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;

/**
 * Manages the interest-dimension catalogue (SYSTEM_ADMIN). See V15__personalize.sql, section
 * 19. interest_dimensions, for why this catalogue must be data and not a hard-coded constant in the code.
 */
@Service
@RequiredArgsConstructor
public class ManageInterestDimensionUseCase {

    /** The code is used as the primary key and sent straight to the LLM as an enum value -- keep it in
     * UPPER_SNAKE form to match the existing codes and to avoid odd characters in the JSON schema. */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{2,31}$");

    private final InterestDimensionRepository repository;
    private final InterestQuizBankTopUpService quizBankTopUpService;

    public List<InterestDimensionResponse> findAll() {
        return repository.findAll().stream()
            .map(ManageInterestDimensionUseCase::toResponse)
            .toList();
    }

    @Transactional
    public InterestDimensionResponse create(UpsertInterestDimensionCommand input) {
        String code = normalizeCode(input.code());
        if (repository.findByCode(code).isPresent()) {
            throw new ResourceDuplicatedException("Interest dimension code already exists: " + code);
        }
        requireLabel(input.label());
        InterestDimensionResponses.InterestDimensionResponse saved = toResponse(repository.save(InterestDimension.defined(
                code,
                input.label().strip(),
                input.description() == null ? null : input.description().strip(),
                input.active() == null || input.active(),
                input.quizEligible() == null || input.quizEligible(),
                input.displayOrder() == null ? 0 : input.displayOrder())));
        // Adding a dimension does NOT make it appear in the quiz by itself: every existing triplet is tied to the
        // 3 old dimensions. Without a top-up the new dimension is never asked, so its score stays empty.
        // Runs in the background so the admin does not wait for the LLM.
        if (saved.active() && saved.quizEligible()) {
            quizBankTopUpService.topUpAsync();
        }
        return saved;
    }

    @Transactional
    public InterestDimensionResponse update(UpsertInterestDimensionCommand input) {
        String code = normalizeCode(input.code());
        InterestDimension existing = repository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Interest dimension not found: " + code));
        // The code is the key and is already assigned to practice_topics/dimension_interest_scores, so it cannot change:
        // only the display fields and flags are editable. To change a code, create a new one and deactivate the old one.
        InterestDimensionResponses.InterestDimensionResponse saved = toResponse(repository.save(existing.withEditableFields(
                input.label() == null ? existing.getLabel() : input.label().strip(),
                descriptionAfterUpdate(input, existing),
                input.active() == null ? existing.isActive() : input.active(),
                input.quizEligible() == null ? existing.isQuizEligible() : input.quizEligible(),
                input.displayOrder() == null ? existing.getDisplayOrder() : input.displayOrder())));
        // Re-enabling a dimension that was off also needs a coverage check: while it was off, the bank may have
        // been topped up without this dimension.
        boolean becameUsable = saved.active() && saved.quizEligible()
            && (!existing.isActive() || !existing.isQuizEligible());
        if (becameUsable) {
            quizBankTopUpService.topUpAsync();
        }
        return saved;
    }

    private static String descriptionAfterUpdate(UpsertInterestDimensionCommand input, InterestDimension existing) {
        if (!input.descriptionProvided()) {
            return existing.getDescription();
        }
        return input.description() == null ? null : input.description().strip();
    }

    @Transactional
    public boolean deactivate(String code) {
        String normalized = normalizeCode(code);
        repository.findByCode(normalized)
            .orElseThrow(() -> new ResourceNotFoundException("Interest dimension not found: " + normalized));
        // Soft disable, no delete: scores and topics already assigned this dimension must stay readable.
        repository.deactivate(normalized);
        return true;
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Interest dimension code must not be empty");
        }
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Interest dimension code must be UPPERCASE, 3-32 characters, letters/digits/underscores only"
            );
        }
        return normalized;
    }

    private static void requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Interest dimension display name must not be empty");
        }
    }

    private static InterestDimensionResponse toResponse(InterestDimension dimension) {
        return new InterestDimensionResponse(
            dimension.getCode(),
            dimension.getLabel(),
            dimension.getDescription(),
            dimension.isActive(),
            dimension.isQuizEligible(),
            dimension.getDisplayOrder(),
            dimension.getCreatedAt(),
            dimension.getUpdatedAt()
        );
    }
}
