package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.scoringrule.ScoringRule;

public interface ScoringRuleRepository {
    Optional<ScoringRule> findById(UUID id);
    ScoringRule save(ScoringRule rule);

    // Kiểm tra trùng mã Code trong phạm vi 1 Assessment Policy (code chỉ cần unique theo từng Policy)
    boolean existsByPolicyIdAndCode(UUID policyId, String code);

    // Tìm kiếm/phân trang danh sách Scoring Rule thuộc 1 Assessment Policy, sắp theo priority tăng dần
    PageResult<ScoringRule> searchByPolicyId(UUID policyId, String keyword, Boolean isActive, int page, int size);

    void deleteById(UUID id);
}
