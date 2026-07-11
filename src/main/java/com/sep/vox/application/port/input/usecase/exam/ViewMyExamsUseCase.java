package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.StudentExamSummaryResponse;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class ViewMyExamsUseCase implements IUseCase<Void, List<StudentExamSummaryResponse>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamSessionRepository examSessionRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamsUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamSessionRepository examSessionRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examSessionRepository = examSessionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public List<StudentExamSummaryResponse> execute(Void input) {
        var now = OffsetDateTime.now();
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId);
        var examsById = new HashMap<>(examRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getExamId())
            .distinct()
            .toList()).stream().collect(java.util.stream.Collectors.toMap(exam -> exam.getId(), exam -> exam)));
        var schedulesById = new HashMap<>(examScheduleRepository.findByIdIn(
            candidates.stream().map(candidate -> candidate.getScheduleId()).filter(java.util.Objects::nonNull).distinct().toList()
        ).stream().collect(java.util.stream.Collectors.toMap(schedule -> schedule.getId(), schedule -> schedule)));

        return candidates.stream()
            .map(candidate -> {
                var exam = examsById.get(candidate.getExamId());
                var schedule = schedulesById.get(candidate.getScheduleId());
                if (exam == null) {
                    return null;
                }

                var latestSessionId = examSessionRepository
                    .findLatestByExamIdAndCandidateId(candidate.getExamId(), candidate.getId())
                    .map(session -> session.getId())
                    .orElse(null);

                return new StudentExamSummaryResponse(
                    exam.getId(),
                    exam.getName(),
                    StudentExamViewSupport.subjectOf(exam),
                    exam.getDescription(),
                    StudentExamViewSupport.durationMinutesOf(schedule, 30),
                    StudentExamViewSupport.examDateOf(schedule, exam.getOpenAt()),
                    StudentExamViewSupport.statusOf(schedule, now),
                    latestSessionId
                );
            })
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(StudentExamSummaryResponse::examDate, Comparator.nullsLast(String::compareTo)))
            .toList();
    }
}
