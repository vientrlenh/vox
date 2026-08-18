package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Một học sinh không được xếp vào hai ca thi chồng lấn thời gian — người thật không thể ngồi ở hai
 * phòng cùng lúc. Song song với {@link ExamScheduleProctorConflictValidator}, chỉ đổi trục từ giám
 * thị sang thí sinh.
 *
 * <p>Phạm vi kiểm tra là TOÀN TRƯỜNG, mọi kỳ thi: chỉ soát trong cùng một kỳ thi thì trường chạy
 * nhiều kỳ thi song song vẫn xếp trùng được — vốn là chính lỗ hổng này.
 *
 * <p>Gom lại vì sáu luồng đều cần đúng một luật, và thiếu bất kỳ luồng nào là còn lối lách: xếp
 * từng thí sinh, xếp hàng loạt, rải đều tự động, sửa giờ ca đã có thí sinh, dời ca (thí sinh theo
 * sang ca đích có khung giờ khác), và sửa giờ mở/đóng của bài kiểm tra trên lớp (giờ được ghi
 * thẳng xuống ca).
 *
 * <h2>Vì sao không cần truyền {@code excludeScheduleId}</h2>
 *
 * <p>Ràng buộc {@code unique (exam_id, student_id)} cho biết mỗi học sinh có tối đa MỘT dòng thí
 * sinh mỗi kỳ thi, tức tối đa một ca bị chiếm mỗi kỳ thi. Nên một dòng xung đột
 * {@code (studentId = S, scheduleId = X)} chính là dòng của thí sinh {@code C} khi và chỉ khi
 * {@code X == C.getScheduleId()}: ca {@code X} thuộc đúng một kỳ thi, mà {@code S} chỉ có một dòng
 * trong kỳ thi đó. Lọc theo ca hiện tại của chính từng thí sinh vì thế loại ĐÚNG dòng của bản thân,
 * không thừa không thiếu — và tự sinh ra đúng phép loại trừ mà cả sáu luồng cần (ca đang bị thay
 * thế khi xếp lại, ca nguồn khi dời, chính ca đó khi đổi giờ).
 */
@Service
public class ExamScheduleCandidateConflictValidator {

    /** Quá số này thì message nêu tên đại diện rồi tóm tắt phần còn lại, tránh dài vô hạn. */
    private static final int MAX_NAMES_IN_MESSAGE = 5;

    private final ExamCandidateRepository examCandidateRepository;
    private final UserRepository userRepository;

    public ExamScheduleCandidateConflictValidator(
            ExamCandidateRepository examCandidateRepository,
            UserRepository userRepository) {
        this.examCandidateRepository = examCandidateRepository;
        this.userRepository = userRepository;
    }

    /**
     * Mọi thí sinh trong {@code candidates} phải rảnh trong khoảng [start, end). Ca hiện tại của
     * chính từng thí sinh được tự loại khỏi phép kiểm tra (xem javadoc lớp).
     */
    public void requireCandidatesFree(Collection<ExamCandidate> candidates, Instant start, Instant end) {
        // Ca chưa đặt giờ thì không có gì để so — cùng cách ExamScheduleRoomValidator bỏ qua khi
        // thiếu dữ liệu.
        if (candidates == null || candidates.isEmpty() || start == null || end == null) {
            return;
        }

        // Thí sinh đã miễn thi hoặc đã huỷ không vào phòng nên không chiếm chỗ, và vì thế cũng
        // không thể bị xếp trùng — dùng chung cách phân loại với ExamCandidateStatus.
        var occupying = candidates.stream()
            .filter(candidate -> !ExamCandidateStatus.isNonScorable(candidate.getStatus()))
            .toList();
        if (occupying.isEmpty()) {
            return;
        }

        // Giá trị nullable nên phải dùng HashMap.put; Collectors.toMap ném NPE khi value là null.
        var currentScheduleByStudent = new HashMap<UUID, UUID>();
        for (var candidate : occupying) {
            currentScheduleByStudent.put(candidate.getStudentId(), candidate.getScheduleId());
        }

        var conflictingStudentIds = examCandidateRepository
            .findConflictsForStudents(currentScheduleByStudent.keySet(), start, end, null).stream()
            .filter(conflict -> !Objects.equals(
                conflict.scheduleId(), currentScheduleByStudent.get(conflict.studentId())))
            .map(conflict -> conflict.studentId())
            .distinct()
            .toList();
        if (conflictingStudentIds.isEmpty()) {
            return;
        }
        throw new DuplicatedException(buildMessage(conflictingStudentIds));
    }

    /**
     * Đổi giờ một ca thi: mọi thí sinh đang được xếp ca đó phải còn rảnh ở khung giờ mới.
     *
     * <p>Không có bước này thì luật bị lách dễ dàng — xếp thí sinh lúc hai ca chưa đụng nhau, rồi
     * dời giờ cho chúng chồng lên.
     */
    public void requireCandidatesFreeForNewWindow(UUID scheduleId, Instant start, Instant end) {
        requireCandidatesFree(examCandidateRepository.findByScheduleId(scheduleId), start, end);
    }

    /**
     * Message nêu tên để người xếp lịch biết phải gỡ ai, thay vì phải tự dò cả danh sách.
     *
     * <p>Không kèm giờ (mọi message trong hệ thống đều không gắn múi giờ) và không kèm tên kỳ thi
     * đang vướng — người xếp lịch kỳ này có thể không có quyền nhìn kỳ kia. Chi tiết nằm ở API
     * {@code studentBusySlots} dành cho màn hình.
     */
    private String buildMessage(List<UUID> conflictingStudentIds) {
        var names = resolveNames(conflictingStudentIds);
        if (names.size() == 1) {
            return "Học sinh " + names.getFirst() + " đã có ca thi khác trong khoảng thời gian này";
        }
        var shown = names.stream().limit(MAX_NAMES_IN_MESSAGE).toList();
        var message = new StringBuilder()
            .append(names.size())
            .append(" học sinh đã có ca thi khác trong khoảng thời gian này: ")
            .append(String.join(", ", shown));
        if (names.size() > shown.size()) {
            message.append(" và ").append(names.size() - shown.size()).append(" học sinh khác");
        }
        return message.toString();
    }

    /**
     * Chỉ tra tên khi đã chắc có xung đột, nên đường thành công không tốn thêm query nào. Thiếu tên
     * thì lùi về id: dựng message tuyệt đối không được ném và che mất lỗi thật.
     */
    private List<String> resolveNames(List<UUID> studentIds) {
        var nameById = new HashMap<UUID, String>();
        for (var user : userRepository.findByIdIn(studentIds)) {
            if (user.getFullName() != null && user.getFullName().value() != null) {
                nameById.put(user.getId(), user.getFullName().value());
            }
        }
        var names = new ArrayList<String>(studentIds.size());
        for (var studentId : studentIds) {
            names.add(nameById.getOrDefault(studentId, studentId.toString()));
        }
        return names;
    }
}
