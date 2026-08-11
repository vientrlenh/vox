package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.ExamCandidateStatusSupport;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;

/**
 * Gán mã đề mặc định cho thí sinh, để không ai lọt tới phòng thi mà chưa có đề.
 *
 * <p>Trước đây việc này chỉ làm cho bài kiểm tra trên lớp có đúng một mã đề; kỳ thi tập trung phải
 * phân đề thủ công hoàn toàn. Giờ áp cho cả hai loại: mã đề được rải đều theo phân bố hiện có của kỳ
 * thi, nên nhiều mã đề vẫn giữ được ý nghĩa thay vì cả phòng chung một đề.
 *
 * <p><b>Điều kiện</b>: MỌI mã đề của kỳ thi phải đã {@code LOCKED} -- đúng bất biến mà
 * {@code AssignExamPapersUseCase} dựng cho khâu phân đề thủ công. Chưa khoá hết thì để trống, vì thí
 * sinh trỏ vào một mã đề mà người ra đề vẫn sửa được là thay đổi bài thi dưới chân họ. Vì vậy điểm
 * móc quan trọng nhất không phải lúc xếp thí sinh vào ca (đề thường chưa khoá) mà là lúc khoá mã đề
 * cuối cùng -- xem {@code UpdateExamPaperStatusUseCase} gọi {@link #backfillExam}.
 *
 * <p><b>Không bao giờ ghi đè</b> thí sinh đã có đề: phân đề thủ công luôn được ưu tiên. Thí sinh đã
 * miễn thi hoặc đã huỷ không vào phòng nên cũng không cần đề.
 *
 * <p>Rải đều chỉ cân bằng về <i>số lượng</i> theo thứ tự ổn định (variant tăng dần), không phải theo
 * sơ đồ chỗ ngồi -- hệ thống không lưu vị trí ngồi của thí sinh.
 */
@Service
public class ExamPaperAutoAssigner {

    private final ExamPaperRepository examPaperRepository;
    private final ExamCandidateRepository examCandidateRepository;

    public ExamPaperAutoAssigner(
            ExamPaperRepository examPaperRepository,
            ExamCandidateRepository examCandidateRepository) {
        this.examPaperRepository = examPaperRepository;
        this.examCandidateRepository = examCandidateRepository;
    }

    /**
     * Gán đề cho những thí sinh chưa có đề trong danh sách. Chỉ sửa đối tượng trong bộ nhớ -- người
     * gọi tự lưu, vì các use case xếp ca đều đã có sẵn một lượt {@code saveAll} ở cuối.
     */
    public void assignPapersIfNeeded(Exam exam, Collection<ExamCandidate> candidates, Instant now, UUID updatedBy) {
        var pending = pendingOf(candidates);
        if (pending.isEmpty()) {
            return;
        }
        var paperIds = resolveAssignablePaperIds(exam);
        if (paperIds.isEmpty()) {
            return;
        }
        // Phân bố hiện có phải tính cả thí sinh đã có đề, nếu không mỗi lượt gán lại dồn hết vào mã
        // đề đầu tiên.
        assign(pending, paperIds, examCandidateRepository.findByExamId(exam.getId()), now, updatedBy);
    }

    /**
     * Gán đề cho MỌI thí sinh của kỳ thi còn thiếu đề và lưu lại. Đây là điểm móc chính: gọi ngay sau
     * khi mã đề cuối cùng được khoá, lúc điều kiện "mọi mã đề LOCKED" vừa đủ.
     *
     * @return số thí sinh vừa được gán đề
     */
    public int backfillExam(Exam exam, Instant now, UUID updatedBy) {
        var paperIds = resolveAssignablePaperIds(exam);
        if (paperIds.isEmpty()) {
            return 0;
        }
        var all = examCandidateRepository.findByExamId(exam.getId());
        var pending = pendingOf(all);
        if (pending.isEmpty()) {
            return 0;
        }
        assign(pending, paperIds, all, now, updatedBy);
        examCandidateRepository.saveAll(pending);
        return pending.size();
    }

    /**
     * Id các mã đề dùng được, sắp theo variant tăng dần cho ổn định; rỗng nếu kỳ thi chưa có mã đề
     * nào hoặc còn mã đề chưa khoá.
     */
    public List<UUID> resolveAssignablePaperIds(Exam exam) {
        var papers = examPaperRepository.findByExamId(exam.getId());
        if (papers.isEmpty() || papers.stream().anyMatch(paper -> paper.getStatus() != ExamPaperStatus.LOCKED)) {
            return List.of();
        }
        return papers.stream()
            .sorted(Comparator.comparingInt((ExamPaper paper) -> paper.getVariant())
                .thenComparing(paper -> paper.getId()))
            .map(paper -> paper.getId())
            .toList();
    }

    private List<ExamCandidate> pendingOf(Collection<ExamCandidate> candidates) {
        return candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .filter(candidate -> !ExamCandidateStatusSupport.isNonScorable(candidate.getStatus()))
            .toList();
    }

    private Map<UUID, Long> usageOf(Collection<ExamCandidate> candidates, List<UUID> paperIds) {
        var usage = new HashMap<UUID, Long>();
        paperIds.forEach(paperId -> usage.put(paperId, 0L));
        for (var candidate : candidates) {
            var paperId = candidate.getAssignedPaperId();
            if (paperId != null && usage.containsKey(paperId)) {
                increment(usage, paperId);
            }
        }
        return usage;
    }

    /** Số lần một mã đề đã được dùng; mã đề chưa có mặt trong bảng đếm coi như 0. */
    private static long countOf(Map<UUID, Long> usage, UUID paperId) {
        var count = usage.get(paperId);
        return count == null ? 0L : count;
    }

    private static void increment(Map<UUID, Long> usage, UUID paperId) {
        usage.put(paperId, countOf(usage, paperId) + 1L);
    }

    /**
     * Rải đều <b>trong từng ca</b>, không phải trên toàn kỳ thi. Cân bằng toàn kỳ thi thì hai vòng
     * round-robin -- xếp thí sinh vào ca ({@code AutoFillExamCandidatesUseCase}) và chọn mã đề -- trùng
     * chu kỳ, nên với 2 ca và 2 mã đề thì cả phòng lĩnh trọn một mã đề, đúng thứ mà nhiều mã đề sinh ra
     * để tránh.
     */
    private void assign(
            List<ExamCandidate> pending,
            List<UUID> paperIds,
            Collection<ExamCandidate> examCandidates,
            Instant now,
            UUID updatedBy) {
        var usageBySchedule = new HashMap<UUID, Map<UUID, Long>>();
        groupBySchedule(examCandidates)
            .forEach((scheduleId, group) -> usageBySchedule.put(scheduleId, usageOf(group, paperIds)));

        groupBySchedule(pending).forEach((scheduleId, group) -> {
            var usage = usageBySchedule.computeIfAbsent(scheduleId, key -> usageOf(List.of(), paperIds));
            for (var candidate : group) {
                var paperId = leastUsed(paperIds, usage);
                candidate.assignPaper(paperId, now, updatedBy);
                increment(usage, paperId);
            }
        });
    }

    /** Thí sinh chưa xếp ca gom chung một nhóm (khoá {@code null}) -- rải đều giữa họ với nhau. */
    private Map<UUID, List<ExamCandidate>> groupBySchedule(Collection<ExamCandidate> candidates) {
        var grouped = new LinkedHashMap<UUID, List<ExamCandidate>>();
        for (var candidate : candidates) {
            grouped.computeIfAbsent(candidate.getScheduleId(), key -> new ArrayList<>()).add(candidate);
        }
        return grouped;
    }

    /** Mã đề đang được dùng ít nhất; hoà thì lấy mã đề đứng trước trong thứ tự ổn định. */
    private UUID leastUsed(List<UUID> paperIds, Map<UUID, Long> usage) {
        var chosen = paperIds.get(0);
        var chosenCount = countOf(usage, chosen);
        for (var paperId : paperIds) {
            var count = countOf(usage, paperId);
            if (count < chosenCount) {
                chosen = paperId;
                chosenCount = count;
            }
        }
        return chosen;
    }
}
