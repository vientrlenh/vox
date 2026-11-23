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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;

/**
 * Assigns a default paper variant to candidates, so that nobody reaches the assessment room without a paper.
 *
 * <p>This used to be done only for class tests with exactly one paper variant; a centralized assessment had to
 * assign papers completely by hand. It now applies to both kinds: variants are spread evenly over the existing distribution of the
 * assessment, so several variants keep their meaning instead of the whole room sharing one paper.
 *
 * <p><b>Condition</b>: EVERY paper variant of the assessment must already be {@code LOCKED} -- exactly the invariant that
 * {@code AssignAssessmentPapersUseCase} builds for the manual assignment step. If not all are locked, it is left empty, because a candidate
 * pointing at a variant that the author can still edit means changing the assessment under their feet. So the most important
 * hook is not the moment candidates are placed into sessions (papers are usually not locked yet) but the moment the last
 * paper variant is locked -- see {@code UpdateAssessmentPaperStatusUseCase} calling {@link #backfillAssessment}.
 *
 * <p><b>Never overwrites</b> a candidate who already has a paper: manual assignment always takes priority. A candidate who is
 * exempted or cancelled does not enter the room, so they do not need a paper either.
 *
 * <p>The even spread only balances the <i>count</i> in a stable order (variant ascending), not by
 * seating plan -- the system does not store where a candidate sits.
 */
@Service
@RequiredArgsConstructor
public class AssessmentPaperAutoAssigner {

    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;

    /**
     * Gán đề cho những thí sinh chưa có đề trong danh sách. Chỉ sửa đối tượng trong bộ nhớ -- người
     * gọi tự lưu, vì các use case xếp ca đều đã có sẵn một lượt {@code saveAll} ở cuối.
     */
    public void assignPapersIfNeeded(Assessment assessment, Collection<AssessmentCandidate> candidates, Instant now, UUID updatedBy) {
        List<AssessmentCandidate> pending = pendingOf(candidates);
        if (pending.isEmpty()) {
            return;
        }
        List<UUID> paperIds = resolveAssignablePaperIds(assessment);
        if (paperIds.isEmpty()) {
            return;
        }
        // Phân bố hiện có phải tính cả thí sinh đã có đề, nếu không mỗi lượt gán lại dồn hết vào mã
        // đề đầu tiên.
        assign(pending, paperIds, assessmentCandidateRepository.findByAssessmentId(assessment.getId()), now, updatedBy);
    }

    /**
     * Gán đề cho MỌI thí sinh của kỳ thi còn thiếu đề và lưu lại. Đây là điểm móc chính: gọi ngay sau
     * khi mã đề cuối cùng được khoá, lúc điều kiện "mọi mã đề LOCKED" vừa đủ.
     *
     * @return số thí sinh vừa được gán đề
     */
    public int backfillAssessment(Assessment assessment, Instant now, UUID updatedBy) {
        List<UUID> paperIds = resolveAssignablePaperIds(assessment);
        if (paperIds.isEmpty()) {
            return 0;
        }
        List<AssessmentCandidate> all = assessmentCandidateRepository.findByAssessmentId(assessment.getId());
        List<AssessmentCandidate> pending = pendingOf(all);
        if (pending.isEmpty()) {
            return 0;
        }
        assign(pending, paperIds, all, now, updatedBy);
        assessmentCandidateRepository.saveAll(pending);
        return pending.size();
    }

    /**
     * Id các mã đề dùng được, sắp theo variant tăng dần cho ổn định; rỗng nếu kỳ thi chưa có mã đề
     * nào hoặc còn mã đề chưa khoá.
     */
    public List<UUID> resolveAssignablePaperIds(Assessment assessment) {
        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessment.getId());
        if (papers.isEmpty() || papers.stream().anyMatch(paper -> paper.getStatus() != AssessmentPaperStatus.LOCKED)) {
            return List.of();
        }
        return papers.stream()
            .sorted(Comparator.comparingInt((AssessmentPaper paper) -> paper.getVariant())
                .thenComparing(paper -> paper.getId()))
            .map(paper -> paper.getId())
            .toList();
    }

    private List<AssessmentCandidate> pendingOf(Collection<AssessmentCandidate> candidates) {
        return candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .toList();
    }

    private Map<UUID, Long> usageOf(Collection<AssessmentCandidate> candidates, List<UUID> paperIds) {
        Map<UUID, Long> usage = new HashMap<UUID, Long>();
        paperIds.forEach(paperId -> usage.put(paperId, 0L));
        for (AssessmentCandidate candidate : candidates) {
            UUID paperId = candidate.getAssignedPaperId();
            if (paperId != null && usage.containsKey(paperId)) {
                increment(usage, paperId);
            }
        }
        return usage;
    }

    /** Số lần một mã đề đã được dùng; mã đề chưa có mặt trong bảng đếm coi như 0. */
    private static long countOf(Map<UUID, Long> usage, UUID paperId) {
        Long count = usage.get(paperId);
        return count == null ? 0L : count;
    }

    private static void increment(Map<UUID, Long> usage, UUID paperId) {
        usage.put(paperId, countOf(usage, paperId) + 1L);
    }

    /**
     * Spread evenly <b>within each session</b>, not over the whole assessment. Balancing over the whole assessment makes the two
     * round-robin passes -- placing candidates into sessions ({@code AutoFillAssessmentCandidatesUseCase}) and choosing the paper variant -- share the same
     * cycle, so with 2 sessions and 2 variants a whole room receives a single paper variant, which is exactly what several variants exist
     * to avoid.
     */
    private void assign(
            List<AssessmentCandidate> pending,
            List<UUID> paperIds,
            Collection<AssessmentCandidate> assessmentCandidates,
            Instant now,
            UUID updatedBy) {
        Map<UUID, Map<UUID, Long>> usageBySchedule = new HashMap<UUID, Map<UUID, Long>>();
        groupBySchedule(assessmentCandidates)
            .forEach((scheduleId, group) -> usageBySchedule.put(scheduleId, usageOf(group, paperIds)));

        groupBySchedule(pending).forEach((scheduleId, group) -> {
            Map<UUID, Long> usage = usageBySchedule.computeIfAbsent(scheduleId, key -> usageOf(List.of(), paperIds));
            for (AssessmentCandidate candidate : group) {
                UUID paperId = leastUsed(paperIds, usage);
                candidate.assignPaper(paperId, now, updatedBy);
                increment(usage, paperId);
            }
        });
    }

    /** Thí sinh chưa xếp ca gom chung một nhóm (khoá {@code null}) -- rải đều giữa họ với nhau. */
    private Map<UUID, List<AssessmentCandidate>> groupBySchedule(Collection<AssessmentCandidate> candidates) {
        Map<UUID, List<AssessmentCandidate>> grouped = new LinkedHashMap<UUID, List<AssessmentCandidate>>();
        for (AssessmentCandidate candidate : candidates) {
            grouped.computeIfAbsent(candidate.getScheduleId(), key -> new ArrayList<>()).add(candidate);
        }
        return grouped;
    }

    /** Mã đề đang được dùng ít nhất; hoà thì lấy mã đề đứng trước trong thứ tự ổn định. */
    private UUID leastUsed(List<UUID> paperIds, Map<UUID, Long> usage) {
        UUID chosen = paperIds.get(0);
        long chosenCount = countOf(usage, chosen);
        for (UUID paperId : paperIds) {
            long count = countOf(usage, paperId);
            if (count < chosenCount) {
                chosen = paperId;
                chosenCount = count;
            }
        }
        return chosen;
    }
}
