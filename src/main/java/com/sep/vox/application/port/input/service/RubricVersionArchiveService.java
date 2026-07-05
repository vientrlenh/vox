package com.sep.vox.application.port.input.service;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
public class RubricVersionArchiveService {

    private static final Logger log = LoggerFactory.getLogger(RubricVersionArchiveService.class);

    private final RubricVersionRepository rubricVersionRepository;

    public RubricVersionArchiveService(RubricVersionRepository rubricVersionRepository) {
        this.rubricVersionRepository = rubricVersionRepository;
    }

    public int archiveExpired() {
        int count = rubricVersionRepository.archiveExpiredPublishedVersions(OffsetDateTime.now());
        if (count > 0) {
            log.info("Đã tự động Archive {} phiên bản Rubric hết hạn", count);
        }
        return count;
    }
}
