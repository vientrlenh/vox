package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.ExamSessionRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    ExamSessionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExamSessionRepositoryTests extends ContainerTestConfig {

    @Autowired
    private ExamSessionRepository examSessionRepository;

    private ExamSession newSession(ExamSessionStatus status) {
        return examSessionRepository.save(new ExamSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            null,
            status,
            false,
            null
        ));
    }

    @Test
    void whenFindByIdAndResumable_thenReturnsInProgressSession() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);

        assertThat(examSessionRepository.findByIdAndResumable(saved.getId())).isPresent();
    }

    /**
     * Đúng tình huống cần nhất: máy học viên mất kết nối, phiên bị đánh dấu INTERRUPTED, rồi máy
     * quay lại với các đoạn ghi còn trong buffer và cần token để đẩy lên.
     */
    @Test
    void whenFindByIdAndResumable_thenReturnsInterruptedSession() {
        var saved = newSession(ExamSessionStatus.INTERRUPTED);

        assertThat(examSessionRepository.findByIdAndResumable(saved.getId())).isPresent();
    }

    @Test
    void whenFindByIdAndResumable_thenSkipsSubmittedSession() {
        var saved = newSession(ExamSessionStatus.SUBMITTED);

        assertThat(examSessionRepository.findByIdAndResumable(saved.getId())).isEmpty();
    }

    @Test
    void whenSessionHasNoChosenStreamType_thenLockWritesIt() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);
        assertThat(saved.getChosenStreamType()).isNull();

        var updated = examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.SCREEN);

        assertThat(updated).isEqualTo(1);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(ExamRequiredStreamType.SCREEN);
    }

    /**
     * Ghi một lần rồi khóa: lần phát token thứ hai không được đổi lựa chọn, kể cả khi request đến
     * song song. Đây là thứ ngăn việc bắt đầu bằng camera + màn hình rồi hạ xuống còn camera.
     */
    @Test
    void whenStreamTypeAlreadyChosen_thenLockChangesNothing() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.CAMERA_AND_SCREEN);

        var updated = examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.CAMERA);

        assertThat(updated).isEqualTo(0);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(ExamRequiredStreamType.CAMERA_AND_SCREEN);
    }

    @Test
    void whenSessionHasNoCheckpoint_thenRemainingSecondsIsWritten() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);
        assertThat(saved.getRemainingSeconds()).isNull();

        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500)).isEqualTo(1);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(1500);
    }

    @Test
    void whenCountdownMovesDown_thenCheckpointIsAccepted() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500);

        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1490)).isEqualTo(1);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(1490);
    }

    /**
     * Ràng buộc bảo mật, không phải tối ưu: giá trị đến từ máy học viên, nên nếu đồng hồ được phép
     * đi tới thì endpoint checkpoint trở thành API tự gia hạn thời gian thi. Cũng loại luôn các gói
     * checkpoint cũ đến muộn không đúng thứ tự.
     */
    @Test
    void whenCountdownWouldMoveUp_thenCheckpointIsRejected() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.checkpointRemainingSeconds(saved.getId(), 600);

        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 1500)).isEqualTo(0);
        assertThat(examSessionRepository.checkpointRemainingSeconds(saved.getId(), 600)).isEqualTo(0);
        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getRemainingSeconds())
            .isEqualTo(600);
    }

    @Test
    void whenSavingSession_thenChosenStreamTypeSurvivesRoundTrip() {
        var saved = newSession(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.lockChosenStreamType(saved.getId(), ExamRequiredStreamType.CAMERA);

        // Một use case khác đọc phiên thi, đổi trạng thái rồi lưu lại: lựa chọn đã chốt không được
        // biến mất chỉ vì đi qua một vòng map domain <-> JPA.
        var reloaded = examSessionRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(ExamSessionStatus.INTERRUPTED);
        examSessionRepository.save(reloaded);

        assertThat(examSessionRepository.findById(saved.getId()))
            .get()
            .extracting(session -> session.getChosenStreamType())
            .isEqualTo(ExamRequiredStreamType.CAMERA);
    }
}
