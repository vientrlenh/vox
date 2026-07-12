package com.sep.vox.infrastructure.worker;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamRepository;

@Component
public class ExamStatusAutoTransitionJob {

    private static final Logger log = LoggerFactory.getLogger(ExamStatusAutoTransitionJob.class);

    private final ExamRepository examRepository;

    public ExamStatusAutoTransitionJob(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        var now = OffsetDateTime.now();

        for (var exam : examRepository.findByStatusAndOpenAtBefore(ExamStatus.SCHEDULED, now)) {
            if (exam.getKind() != ExamKind.CLASS_TEST) {
                continue;
            }
            exam.setStatus(ExamStatus.IN_PROGRESS);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            log.info("Tự động mở bài kiểm tra {} (openAt đã tới)", exam.getId());
        }

        for (var exam : examRepository.findByStatusAndCloseAtBefore(ExamStatus.IN_PROGRESS, now)) {
            if (exam.getKind() != ExamKind.CLASS_TEST) {
                continue;
            }
            exam.setStatus(ExamStatus.CLOSED);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            log.info("Tự động đóng bài kiểm tra {} (closeAt đã tới)", exam.getId());
        }
    }
}
