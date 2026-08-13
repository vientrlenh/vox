package com.sep.vox.application.query.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sep.vox.domain.dto.SchoolDebtEventDto;

public interface SchoolDebtEventQueryRepository {
    Page<SchoolDebtEventDto> findAllBySchoolId(UUID schoolId, Pageable pageable);
}
