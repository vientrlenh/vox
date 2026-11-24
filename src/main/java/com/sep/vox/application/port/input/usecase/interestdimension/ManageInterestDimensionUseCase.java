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
 * Quản lý danh mục chiều sở thích (SYSTEM_ADMIN). Xem V15__personalize.sql, mục
 * 19. interest_dimensions, để biết vì sao danh mục này phải là dữ liệu chứ không phải hằng số
 * cứng trong code.
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
            throw new ResourceDuplicatedException("Mã chiều sở thích đã tồn tại: " + code);
        }
        requireLabel(input.label());
        InterestDimensionResponses.InterestDimensionResponse saved = toResponse(repository.save(InterestDimension.builder()
                .code(code)
                .label(input.label().strip())
                .description(input.description() == null ? null : input.description().strip())
                .active(input.active() == null || input.active())
                .quizEligible(input.quizEligible() == null || input.quizEligible())
                .displayOrder(input.displayOrder() == null ? 0 : input.displayOrder())
                .createdAt(null)
                .updatedAt(null)
                .build()));
        // Thêm chiều KHÔNG tự làm nó xuất hiện trong quiz: mọi triplet đã có đều gắn 3 chiều
        // cũ. Không sinh bổ sung thì chiều mới không bao giờ được hỏi -> điểm luôn rỗng.
        // Chạy nền để admin không phải chờ LLM.
        if (saved.active() && saved.quizEligible()) {
            quizBankTopUpService.topUpAsync();
        }
        return saved;
    }

    @Transactional
    public InterestDimensionResponse update(UpsertInterestDimensionCommand input) {
        String code = normalizeCode(input.code());
        InterestDimension existing = repository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chiều sở thích: " + code));
        // The code is the key and is already assigned to practice_topics/dimension_interest_scores, so it cannot change:
        // only the display fields and flags are editable. To change a code, create a new one and deactivate the old one.
        InterestDimensionResponses.InterestDimensionResponse saved = toResponse(repository.save(InterestDimension.builder()
                .code(existing.getCode())
                .label(input.label() == null ? existing.getLabel() : input.label().strip())
                .description(descriptionAfterUpdate(input, existing))
                .active(input.active() == null ? existing.isActive() : input.active())
                .quizEligible(input.quizEligible() == null ? existing.isQuizEligible() : input.quizEligible())
                .displayOrder(input.displayOrder() == null ? existing.getDisplayOrder() : input.displayOrder())
                .createdAt(existing.getCreatedAt())
                .updatedAt(null)
                .build()));
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chiều sở thích: " + normalized));
        // Tắt mềm, không xoá: điểm số và chủ đề đã gán chiều này vẫn phải đọc được.
        repository.deactivate(normalized);
        return true;
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã chiều sở thích không được để trống");
        }
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Mã chiều sở thích phải viết HOA, 3-32 ký tự, chỉ gồm chữ/số/gạch dưới"
            );
        }
        return normalized;
    }

    private static void requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị của chiều sở thích không được để trống");
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
