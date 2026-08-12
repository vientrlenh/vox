package com.sep.vox.application.port.input.usecase.interestdimension;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpsertInterestDimensionCommand;
import com.sep.vox.application.port.input.service.InterestQuizBankTopUpService;
import com.sep.vox.application.response.input.interestdimension.InterestDimensionResponses.InterestDimensionResponse;
import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;

/**
 * Quản lý danh mục chiều sở thích (SYSTEM_ADMIN). Xem V15__personalize.sql, mục
 * 19. interest_dimension, để biết vì sao danh mục này phải là dữ liệu chứ không phải hằng số
 * cứng trong code.
 */
@Service
public class ManageInterestDimensionUseCase {

    /** Mã dùng làm khoá chính và gửi thẳng cho LLM làm giá trị enum -- giữ dạng
     * UPPER_SNAKE để khớp với các mã đã có và tránh ký tự lạ trong JSON schema. */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{2,31}$");

    private final InterestDimensionRepository repository;
    private final InterestQuizBankTopUpService quizBankTopUpService;

    public ManageInterestDimensionUseCase(
            InterestDimensionRepository repository,
            InterestQuizBankTopUpService quizBankTopUpService) {
        this.repository = repository;
        this.quizBankTopUpService = quizBankTopUpService;
    }

    public List<InterestDimensionResponse> findAll() {
        return repository.findAll().stream()
            .map(ManageInterestDimensionUseCase::toResponse)
            .toList();
    }

    @Transactional
    public InterestDimensionResponse create(UpsertInterestDimensionCommand input) {
        var code = normalizeCode(input.code());
        if (repository.findByCode(code).isPresent()) {
            throw new DuplicatedException("Mã chiều sở thích đã tồn tại: " + code);
        }
        requireLabel(input.label());
        var saved = toResponse(repository.save(new InterestDimension(
            code,
            input.label().strip(),
            input.description() == null ? null : input.description().strip(),
            input.active() == null || input.active(),
            input.quizEligible() == null || input.quizEligible(),
            input.displayOrder() == null ? 0 : input.displayOrder(),
            null,
            null
        )));
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
        var code = normalizeCode(input.code());
        var existing = repository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chiều sở thích: " + code));
        // Mã là khoá và đã được gán vào practice_topic/dimension_interest_score -- không cho
        // đổi, chỉ sửa phần hiển thị và cờ. Muốn đổi mã thì tạo mới rồi tắt cái cũ.
        var saved = toResponse(repository.save(new InterestDimension(
            existing.getCode(),
            input.label() == null ? existing.getLabel() : input.label().strip(),
            input.description() == null ? existing.getDescription() : input.description().strip(),
            input.active() == null ? existing.isActive() : input.active(),
            input.quizEligible() == null ? existing.isQuizEligible() : input.quizEligible(),
            input.displayOrder() == null ? existing.getDisplayOrder() : input.displayOrder(),
            existing.getCreatedAt(),
            null
        )));
        // Bật lại một chiều từng tắt cũng cần kiểm tra phủ: trong lúc nó tắt, kho có thể đã
        // được sinh thêm mà bỏ qua chiều này.
        var becameUsable = saved.active() && saved.quizEligible()
            && (!existing.isActive() || !existing.isQuizEligible());
        if (becameUsable) {
            quizBankTopUpService.topUpAsync();
        }
        return saved;
    }

    @Transactional
    public boolean deactivate(String code) {
        var normalized = normalizeCode(code);
        repository.findByCode(normalized)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chiều sở thích: " + normalized));
        // Tắt mềm, không xoá: điểm số và chủ đề đã gán chiều này vẫn phải đọc được.
        repository.deactivate(normalized);
        return true;
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã chiều sở thích không được để trống");
        }
        var normalized = code.strip().toUpperCase(Locale.ROOT);
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
            dimension.getCreatedAt() == null ? null : dimension.getCreatedAt().toString(),
            dimension.getUpdatedAt() == null ? null : dimension.getUpdatedAt().toString()
        );
    }
}
