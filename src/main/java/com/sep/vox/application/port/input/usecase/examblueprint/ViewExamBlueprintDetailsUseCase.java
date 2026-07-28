package com.sep.vox.application.port.input.usecase.examblueprint;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamBlueprintDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamBlueprintDetailsUseCase implements IUseCase<ViewExamBlueprintDetailsQuery, ExamBlueprintDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewExamBlueprintDetailsUseCase.class);
    // TODO(perf-debug): watchdog tạm thời để bắt stack trace của chính thread đang chạy
    // nếu nó chưa xong sau 3s — xóa sau khi tìm ra nguyên nhân treo không rõ lý do.
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "blueprint-slow-watchdog");
        t.setDaemon(true);
        return t;
    });

    private final ExamBlueprintRepository examBlueprintRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewExamBlueprintDetailsUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    public ExamBlueprintDto execute(ViewExamBlueprintDetailsQuery input) {
        var startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] ViewExamBlueprintDetailsUseCase start blueprintId={}", input.id());
        var callingThread = Thread.currentThread();
        var watchdogTask = WATCHDOG.schedule(() -> {
            var sb = new StringBuilder("[blueprint-perf] SLOW: thread=" + callingThread.getName()
                + " state=" + callingThread.getState() + " still running after 3000ms, stack=");
            for (var el : callingThread.getStackTrace()) {
                sb.append("\n\tat ").append(el);
            }
            LOGGER.warn(sb.toString());
        }, 3000, TimeUnit.MILLISECONDS);
        try {
            var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
            var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElse(null);

            var blueprint = examBlueprintRepository.findById(input.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi với id: " + input.id()));
            if (!hasAccess(blueprint, currentSchoolId)) {
                throw new ForbiddenException("Quyền truy cập bị từ chối: không có quyền truy cập blueprint đề thi này");
            }
            var dto = ExamBlueprintDtoMapper.toDto(blueprint);
            var tookMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.info("[blueprint-perf] ViewExamBlueprintDetailsUseCase done blueprintId={} tookMs={}", input.id(), tookMs);
            return dto;
        } finally {
            watchdogTask.cancel(false);
        }
    }

    private boolean hasAccess(ExamBlueprint blueprint, java.util.UUID currentSchoolId) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        return currentSchoolId != null && blueprint.getSchoolId().equals(currentSchoolId);
    }
}
