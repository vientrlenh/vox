package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.StudentQuestionExposureRepository;

/**
 * Ghi PracticePaper + PracticePaperItem + exposure -- tách riêng khỏi
 * BuildPracticePaperUseCase để @Transactional chỉ bọc đúng phần ghi DB, không
 * bọc luôn phần gọi ra ngoài (Python agents) phía trước nó. Là bean riêng
 * (không phải method private/protected cùng class) vì self-invocation bỏ qua
 * proxy AOP của Spring -- @Transactional trên method cùng class sẽ bị lờ đi
 * nếu gọi qua this.
 */
@Service
public class PracticePaperPersistenceService {

    /** Thời gian giữ chỗ quota cho một đề đã dựng nhưng chưa vào phiên. */
    private static final Duration RESERVATION_WINDOW = Duration.ofMinutes(10);

    private final PracticePaperRepository paperRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public PracticePaperPersistenceService(
            PracticePaperRepository paperRepository,
            PracticePaperItemRepository paperItemRepository,
            StudentQuestionExposureRepository studentQuestionExposureRepository,
            LearnerProfileRepository learnerProfileRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.paperRepository = paperRepository;
        this.paperItemRepository = paperItemRepository;
        this.studentQuestionExposureRepository = studentQuestionExposureRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Transactional
    public com.sep.vox.domain.model.personalization.PracticePaper persist(
            UUID studentId,
            UUID topicId,
            UUID targetFrameworkBandId,
            String origin,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds,
            PracticeQuestion question,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        var paper = createPaper(
            studentId, topicId, targetFrameworkBandId, origin,
            offeredTopicIds, previousOfferedTopicIds, question
        );
        saveItemAndExposure(studentId, paper.getId(), selection);
        return paper;
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private com.sep.vox.domain.model.personalization.PracticePaper createPaper(
            UUID studentId,
            UUID topicId,
            UUID targetFrameworkBandId,
            String origin,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds,
            PracticeQuestion question) {
        var resolvedOrigin = origin == null ? "SELECTED" : origin;
        var now = Instant.now();
        return paperRepository.save(new com.sep.vox.domain.model.personalization.PracticePaper(
            UUID.randomUUID(),
            studentId,
            topicId,
            targetFrameworkBandId,
            resolvedOrigin,
            currentGoal(studentId),
            jsonSerializationPort.toJson(
                offeredTopicIds == null ? List.of() : offeredTopicIds
            ),
            jsonSerializationPort.toJson(
                previousOfferedTopicIds == null ? List.of() : previousOfferedTopicIds
            ),
            // plannedSeconds cũ = preparationTimeSeconds + spokenSeconds; bỏ cột chuẩn bị (V11)
            // thì hai con số trùng nhau, nên gộp về một tên thay vì giữ hai tên cho một giá trị.
            question.spokenSeconds(),
            question.spokenSeconds(),
            // Instant không có plusMinutes -- nó là mốc tuyệt đối, không mang lịch/múi giờ nên
            // chỉ cộng được Duration. Ý nghĩa vẫn y hệt: giữ chỗ quota 10 phút.
            now.plus(RESERVATION_WINDOW),
            "RESERVED",
            now
        ));
    }

    private void saveItemAndExposure(
            UUID studentId,
            UUID paperId,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        paperItemRepository.save(new PracticePaperItem(
            UUID.randomUUID(),
            paperId,
            selection.question().getId(),
            selection.slot(),
            selection.criterion(),
            selection.subAttribute(),
            selection.targetRank()
        ));
        studentQuestionExposureRepository.recordExposure(studentId, selection.question().getId());
    }
}
