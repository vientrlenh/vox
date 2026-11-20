package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;

import com.sep.vox.domain.model.personalization.InterestDimension;

public interface InterestDimensionRepository {

    /** Toàn bộ danh mục, kể cả đã tắt -- dùng cho màn quản trị. */
    List<InterestDimension> findAll();

    /** Dimensions still in effect, used to rank topics (including ACADEMIC_ASSESSMENT). */
    List<InterestDimension> findActive();

    /** Dimensions allowed to be put to the student in the interest quiz -- NOT including ACADEMIC_ASSESSMENT. */
    List<InterestDimension> findQuizEligible();

    Optional<InterestDimension> findByCode(String code);

    InterestDimension save(InterestDimension dimension);

    /** Tắt mềm -- giữ nguyên điểm số/topic lịch sử đã gán chiều này. */
    void deactivate(String code);
}
