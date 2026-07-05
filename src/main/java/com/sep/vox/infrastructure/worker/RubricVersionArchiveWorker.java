package com.sep.vox.infrastructure.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.RubricVersionArchiveService;

@Component
public class RubricVersionArchiveWorker {

    private static final Logger log = LoggerFactory.getLogger(RubricVersionArchiveWorker.class);

    private final RubricVersionArchiveService archiveService;

    public RubricVersionArchiveWorker(RubricVersionArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        try {
            archiveService.archiveExpired();
        } catch (Exception e) {
            log.error("Lỗi khi chạy worker archive RubricVersion hết hạn", e);
        }
    }
}
