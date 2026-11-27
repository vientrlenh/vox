package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticePaper;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;

/** The DB write part (read the reserved paper + create the session + change the paper status) of
 * StartPracticeSessionUseCase -- split out so @Transactional does not also wrap
 * agentAvailabilityClient.requireReady() (an external health check, timeout up to 12s). */
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
            .orElseThrow(() -> new ResourceNotFoundException(
                "The practice paper does not exist or its reservation time has expired."
            ));
        com.sep.vox.domain.model.personalization.PracticeSession savedSession = startSession(studentId, paper, now);
        practicePaperRepository.save(paper.withStatus("STARTED"));
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(savedSession.getId(), studentId).orElse(null)
            )
        );
    }

    /**
     * There is no rubric and no assessment policy lookup: practice is graded on a fixed 0-100 scale.
     * The policy is chosen by the student's class, and when the school has not configured one the student
     * could NOT enter the session -- blocking at the door for something practice does not use.
     *
     * <p>The band is chosen by the STUDENT on the preparation screen; it is the only thing left from the
     * assessment framework.
     */
    private com.sep.vox.domain.model.personalization.PracticeSession startSession(
            UUID studentId, PracticePaper paper, Instant now) {
        return practiceSessionRepository.save(
            com.sep.vox.domain.model.personalization.PracticeSession.started(
                studentId,
                paper.getId(),
                requireTargetBand(paper.getTargetFrameworkBandId()),
                paper.getPracticeTopicId(),
                paper.getOrigin(),
                offerEvidenceJson(paper.getOfferedTopicIdsJson(), paper.getPreviousOfferedTopicIdsJson()),
                now));
    }

    /**
     * Papers built BEFORE V15 have no target band. Their reservation expired long ago
     * ({@code findReservedPaper} filters by {@code now}) so in practice no paper reaches
     * here -- but saying it plainly still beats writing an invented band into the session.
     */
    private UUID requireTargetBand(UUID targetFrameworkBandId) {
        if (targetFrameworkBandId == null) {
            throw new ResourceNotFoundException("The practice paper has no target band, please build the paper again.");
        }
        return targetFrameworkBandId;
    }

    private String offerEvidenceJson(String current, String previous) {
        String currentJson = current == null || current.isBlank() ? "[]" : current;
        String previousJson = previous == null || previous.isBlank() ? "[]" : previous;
        return "{\"current\":%s,\"previous\":%s}".formatted(currentJson, previousJson);
    }
}
