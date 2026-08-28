package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;

public interface QuestionBankRepository {
    QuestionBank save(QuestionBank questionBank);
    Optional<QuestionBank> findById(UUID id);
    List<QuestionBank> findByIdIn(Collection<UUID> ids);
    PageResult<QuestionBank> findAll(int pageNumber, int size);
    PageResult<QuestionBank> findAccessible(
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        QuestionBankOwnerType ownerType,
        QuestionBankStatus status,
        UUID languageId,
        UUID schoolId,
        UUID schoolGradeId,
        String keyword,
        int pageNumber,
        int size
    );
    /**
     * Ngân hàng câu hỏi trong đúng một phạm vi sở hữu, để import đối chiếu trùng {@code code}.
     *
     * <p>{@code schoolId} phải là {@code null} khi {@code ownerType} là {@code SYSTEM} — ngân hàng
     * hệ thống không thuộc trường nào. Không dùng {@code findAccessible} cho việc này: hàm đó phân
     * trang và trộn cả quyền truy cập, còn ở đây cần trọn danh sách của MỘT phạm vi.
     */
    List<QuestionBank> findByOwnerScope(QuestionBankOwnerType ownerType, UUID schoolId);

    boolean existsById(UUID id);
    void deleteById(UUID id);
}
