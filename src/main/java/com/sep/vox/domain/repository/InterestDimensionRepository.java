package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;

import com.sep.vox.domain.model.personalization.InterestDimension;

public interface InterestDimensionRepository {

    /** Toàn bộ danh mục, kể cả đã tắt -- dùng cho màn quản trị. */
    List<InterestDimension> findAll();

    /** Chiều còn hiệu lực, dùng để xếp hạng chủ đề (gồm cả ACADEMIC_EXAM). */
    List<InterestDimension> findActive();

    /** Chiều được phép đem ra hỏi trong quiz sở thích -- KHÔNG gồm ACADEMIC_EXAM. */
    List<InterestDimension> findQuizEligible();

    Optional<InterestDimension> findByCode(String code);

    InterestDimension save(InterestDimension dimension);

    /** Tắt mềm -- giữ nguyên điểm số/topic lịch sử đã gán chiều này. */
    void deactivate(String code);
}
