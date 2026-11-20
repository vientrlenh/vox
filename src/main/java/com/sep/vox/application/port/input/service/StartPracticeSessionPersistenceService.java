package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

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
public class StartPracticeSessionPersistenceService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticePaperRepository practicePaperRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;

    public StartPracticeSessionPersistenceService(
            PracticeSessionRepository practiceSessionRepository,
            PracticePaperRepository practicePaperRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practicePaperRepository = practicePaperRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
    }

    @Transactional
    public PracticeSession persist(UUID studentId, UUID paperId) {
        var now = Instant.now();
        var paper = practicePaperRepository
            .findReservedPaper(paperId, studentId, now)
            .orElseThrow(() -> new NotFoundException(
                "Đề luyện không tồn tại hoặc đã hết thời gian giữ chỗ."
            ));
        var sessionId = UUID.randomUUID();
        practiceSessionRepository.save(new com.sep.vox.domain.model.personalization.PracticeSession(
            sessionId,
            studentId,
            paper.getId(),
            // Không có rubric, cũng không tra assessment policy: luyện tập chấm thang 0-100
            // cố định. Policy chọn theo lớp học sinh, mà trường chưa cấu hình thì học sinh
            // KHÔNG vào được phiên -- chặn ngay ở cửa vì một thứ luyện tập không dùng tới.
            //
            // Bậc do HỌC SINH chọn ở màn chuẩn bị, là thứ duy nhất còn lại từ khung đánh giá.
            requireTargetBand(paper.getTargetFrameworkBandId()),
            paper.getPracticeTopicId(),
            "[]",
            paper.getOrigin(),
            offerEvidenceJson(paper.getOfferedTopicIdsJson(), paper.getPreviousOfferedTopicIdsJson()),
            null,
            now,
            null,
            now,
            0,
            "IN_PROGRESS",
            null,
            0,
            0
        ));
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
        var currentJson = current == null || current.isBlank() ? "[]" : current;
        var previousJson = previous == null || previous.isBlank() ? "[]" : previous;
        return "{\"current\":%s,\"previous\":%s}".formatted(currentJson, previousJson);
    }
}
