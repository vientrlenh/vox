package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticePaper;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;

/** Phần ghi DB (đọc đề đã giữ chỗ + tạo session + đổi trạng thái đề) của
 * StartPracticeSessionUseCase -- tách riêng để @Transactional không bọc luôn
 * agentAvailabilityClient.requireReady() (health-check ra ngoài, timeout tới 12s). */
@Service
@RequiredArgsConstructor
public class StartPracticeSessionPersistenceService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticePaperRepository practicePaperRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;

    @Transactional
    public PracticeSession persist(UUID studentId, UUID paperId) {
        Instant now = Instant.now();
        PracticePaper paper = practicePaperRepository
            .findReservedPaper(paperId, studentId, now)
            .orElseThrow(() -> new NotFoundException(
                "Đề luyện không tồn tại hoặc đã hết thời gian giữ chỗ."
            ));
        UUID sessionId = UUID.randomUUID();
        practiceSessionRepository.save(com.sep.vox.domain.model.personalization.PracticeSession.builder()
            .id(sessionId)
            .studentId(studentId)
            .practicePaperId(paper.getId())
            // There is no rubric and no assessment policy lookup: practice is graded on a fixed 0-100 scale.
            // The policy is chosen by the student's class, and when the school has not configured one the student
            // could NOT enter the session -- blocking at the door for something practice does not use.
            //
            // The band is chosen by the STUDENT on the preparation screen; it is the only thing left from the
            // assessment framework.
            .targetFrameworkBandId(requireTargetBand(paper.getTargetFrameworkBandId()))
            .chosenPracticeTopicId(paper.getPracticeTopicId())
            .targetSubAttributesJson("[]")
            .origin(paper.getOrigin())
            .offeredTopicIdsJson(offerEvidenceJson(paper.getOfferedTopicIdsJson(), paper.getPreviousOfferedTopicIdsJson()))
            .startedAt(now)
            .lastHeartbeatAt(now)
            .gradedSeconds(0)
            .status("IN_PROGRESS")
            .helpRequestCount(0)
            .longPauseCount(0)
            .build());
        practicePaperRepository.save(paper.withStatus("STARTED"));
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(sessionId, studentId).orElse(null)
            )
        );
    }

    /**
     * Đề dựng TRƯỚC V15 không có bậc mục tiêu. Chúng đã hết hạn giữ chỗ từ lâu
     * ({@code findReservedPaper} lọc theo {@code now}) nên trên thực tế không đề nào vào được
     * đây -- nhưng nói thẳng vẫn hơn ghi một bậc bịa vào phiên.
     */
    private UUID requireTargetBand(UUID targetFrameworkBandId) {
        if (targetFrameworkBandId == null) {
            throw new NotFoundException("Đề luyện chưa có bậc mục tiêu, hãy dựng lại đề.");
        }
        return targetFrameworkBandId;
    }

    private String offerEvidenceJson(String current, String previous) {
        String currentJson = current == null || current.isBlank() ? "[]" : current;
        String previousJson = previous == null || previous.isBlank() ? "[]" : previous;
        return "{\"current\":%s,\"previous\":%s}".formatted(currentJson, previousJson);
    }
}
