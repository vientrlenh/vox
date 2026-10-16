package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.GradingAssignmentOpenedPayload;
import com.sep.vox.application.mapper.assessmentgrading.GradingResultCode;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

/**
 * Báo cho giáo viên rằng họ vừa được giao một vòng chấm.
 *
 * <p>Gọi từ ba đường GIAO VIỆC: gán tay ({@code AssignGradingUseCase}), gán tự động
 * ({@code AutoAssignGradingUseCase}), và chuyển người chấm ({@code ReassignGradingUseCase}).
 * KHÔNG gọi từ {@code ClaimClassTestGradingUseCase} -- ở đó giáo viên tự bấm nhận bài, và báo
 * lại chính việc họ vừa làm là tiếng ồn thuần tuý.
 *
 * <p>Mọi truy vấn đều theo LÔ: gán tự động rải cả lớp trong một lần gọi, nên hỏi lẻ từng phân
 * công là N+1 ngay giữa một transaction đang giữ khoá.
 */
@Service
public class GradingAssignmentNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradingAssignmentNotificationService.class);

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public GradingAssignmentNotificationService(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            UserRepository userRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    /**
     * {@code REQUIRED}: cả ba nơi gọi đều đã ở trong transaction của use case, nên outbox và
     * dòng phân công cùng sống hoặc cùng chết -- giao việc xong mà mất thông báo thì giáo viên
     * không biết mình có việc cho tới lần mở app kế tiếp.
     */
    @Transactional
    public void publishAssigned(List<AssessmentGradingAssignment> assignments, Instant now) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        var resultsById = byId(
            assessmentCandidateResultRepository.findByIdIn(assignments.stream()
                .map(assignment -> assignment.getCandidateResultId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            result -> result.getId());

        var assessmentsById = byId(
            assessmentRepository.findByIdIn(resultsById.values().stream()
                .map(result -> result.getAssessmentId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            assessment -> assessment.getId());

        var studentNamesByResultId = studentNames(resultsById.values(), assessmentsById);

        for (var assignment : assignments) {
            var result = resultsById.get(assignment.getCandidateResultId());
            var assessment = result == null ? null : assessmentsById.get(result.getAssessmentId());
            if (result == null || assessment == null || assignment.getTeacherId() == null) {
                LOGGER.warn("Bỏ qua thông báo giao chấm, thiếu dữ liệu: assignmentId={}", assignment.getId());
                continue;
            }

            var payload = jsonSerializationPort.toJson(new GradingAssignmentOpenedPayload(
                assignment.getId(),
                assignment.getTeacherId(),
                assessment.getId(),
                assessment.getName(),
                assessment.getKind(),
                assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
                candidateLabel(assessment, result, studentNamesByResultId),
                assignment.getDeadlineAt()
            ));

            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.ASSESSMENT_GRADING_ASSIGNMENT, assignment.getId(),
                EventTypeConstant.GRADING_ASSIGNMENT_OPENED, payload, now
            ));
        }

        LOGGER.info("Đã báo giao chấm cho {} phân công", assignments.size());
    }

    /**
     * Thứ giáo viên dùng để nhận ra bài, chọn theo ĐÚNG luật của màn hình chấm.
     *
     * <p>Kỳ thi tập trung chấm mù: tên học sinh không bao giờ rời khỏi tầng dữ liệu, kể cả vào
     * một thông báo. Mã bài 8 ký tự là thứ duy nhất nhận diện được bài trên màn ẩn danh -- đúng
     * thứ {@code JpaAssessmentGradingQueryRepository} trả về cho hàng đợi chấm.
     */
    private String candidateLabel(
            Assessment assessment, AssessmentCandidateResult result, Map<UUID, String> studentNamesByResultId) {
        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return GradingResultCode.of(result.getId());
        }
        var studentName = studentNamesByResultId.get(result.getId());
        return studentName != null ? studentName : GradingResultCode.of(result.getId());
    }

    /**
     * Chỉ tra tên cho bài kiểm tra trên lớp. Không phải để tiết kiệm truy vấn mà để KHÔNG bao
     * giờ nạp tên của bài thi tập trung vào bộ nhớ ở luồng này -- thứ không được nạp thì không
     * lọt vào payload được, dù ai đó sửa nhầm {@link #candidateLabel} về sau.
     */
    private Map<UUID, String> studentNames(
            Collection<AssessmentCandidateResult> results, Map<UUID, Assessment> assessmentsById) {
        var classTestResults = results.stream()
            .filter(result -> {
                var assessment = assessmentsById.get(result.getAssessmentId());
                return assessment != null && assessment.getKind() == AssessmentKind.CLASS_TEST;
            })
            .filter(result -> result.getCandidateId() != null)
            .toList();

        if (classTestResults.isEmpty()) {
            return Map.of();
        }

        var candidatesById = byId(
            assessmentCandidateRepository.findByIdIn(classTestResults.stream()
                .map(result -> result.getCandidateId())
                .distinct()
                .toList()),
            candidate -> candidate.getId());

        var usersById = byId(
            userRepository.findByIdIn(candidatesById.values().stream()
                .map(candidate -> candidate.getStudentId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            user -> user.getId());

        var names = new java.util.HashMap<UUID, String>();
        for (var result : classTestResults) {
            var candidate = candidatesById.get(result.getCandidateId());
            if (candidate == null || candidate.getStudentId() == null) {
                continue;
            }
            var user = usersById.get(candidate.getStudentId());
            if (user == null || user.getFullName() == null) {
                continue;
            }
            names.put(result.getId(), user.getFullName().value());
        }
        return names;
    }

    private <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> idOf) {
        return items.stream().collect(Collectors.toMap(idOf, Function.identity(), (left, right) -> left));
    }
}
