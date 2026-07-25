package com.sep.vox.application.query.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sep.vox.domain.dto.InvoiceDto;

public interface InvoiceQueryRepository {
    Page<InvoiceDto> findAllBySchoolId(UUID schoolId, Pageable pageable);
}
