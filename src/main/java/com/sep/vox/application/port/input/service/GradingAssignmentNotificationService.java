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

import com.sep.vox.application.event.GradingAssignmentOpenedPayloadV1;
import com.sep.vox.application.mapper.examgrading.GradingResultCode;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;

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

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public GradingAssignmentNotificationService(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            UserRepository userRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
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
    public void publishAssigned(List<ExamGradingAssignment> assignments, Instant now) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        var resultsById = byId(
            examCandidateResultRepository.findByIdIn(assignments.stream()
                .map(assignment -> assignment.getCandidateResultId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            result -> result.getId());

        var examsById = byId(
            examRepository.findByIdIn(resultsById.values().stream()
                .map(result -> result.getExamId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            exam -> exam.getId());

        var studentNamesByResultId = studentNames(resultsById.values(), examsById);

        for (var assignment : assignments) {
            var result = resultsById.get(assignment.getCandidateResultId());
            var exam = result == null ? null : examsById.get(result.getExamId());
            if (result == null || exam == null || assignment.getTeacherId() == null) {
                LOGGER.warn("Bỏ qua thông báo giao chấm, thiếu dữ liệu: assignmentId={}", assignment.getId());
                continue;
            }

            var payload = jsonSerializationPort.toJson(new GradingAssignmentOpenedPayloadV1(
                assignment.getId(),
                assignment.getTeacherId(),
                exam.getId(),
                exam.getName(),
                exam.getKind(),
                assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
                candidateLabel(exam, result, studentNamesByResultId),
                assignment.getDeadlineAt()
            ));

            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.EXAM_GRADING_ASSIGNMENT, assignment.getId(),
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
     * thứ {@code JpaExamGradingQueryRepository} trả về cho hàng đợi chấm.
     */
    private String candidateLabel(
            Exam exam, ExamCandidateResult result, Map<UUID, String> studentNamesByResultId) {
        if (exam.getKind() != ExamKind.CLASS_TEST) {
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
            Collection<ExamCandidateResult> results, Map<UUID, Exam> examsById) {
        var classTestResults = results.stream()
            .filter(result -> {
                var exam = examsById.get(result.getExamId());
                return exam != null && exam.getKind() == ExamKind.CLASS_TEST;
            })
            .filter(result -> result.getCandidateId() != null)
            .toList();

        if (classTestResults.isEmpty()) {
            return Map.of();
        }

        var candidatesById = byId(
            examCandidateRepository.findByIdIn(classTestResults.stream()
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
