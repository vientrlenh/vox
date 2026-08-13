package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ChangeSystemRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class ChangeSystemRubricVersionStatusUseCase implements IUseCase<ChangeSystemRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final FrameworkRepository frameworkRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ChangeSystemRubricVersionStatusUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            FrameworkRepository frameworkRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricCriterionRepository rubricCriterionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.frameworkRepository = frameworkRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(ChangeSystemRubricVersionStatusCommand command) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Lấy Version ra kiểm tra
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() == command.status()) {
            throw new IllegalStateException("Phiên bản đã đang ở trạng thái " + command.status() + ".");
        }

        // 3. Kiểm tra bảo mật System Ownership
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể chuyển trạng thái Rubric thuộc về Trường học (School).");
        }

        Instant now = Instant.now();

        // 4. XỬ LÝ LOGIC MÁY TRẠNG THÁI (STATE MACHINE) CHẶT CHẼ
        if (command.status() == RubricStatus.PUBLISHED) {

            // Nếu muốn đổi sang PUBLISHED thì phải đang là DRAFT
            if (version.getStatus() != RubricStatus.DRAFT) {
                throw new IllegalStateException("Chỉ có thể ban hành (PUBLISH) phiên bản đang ở trạng thái Nháp (DRAFT).");
            }

            // KIỂM TRA FRAMEWORK GỐC
            Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework) liên kết."));
            if (!framework.isActive()) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì Khung tiêu chuẩn (Framework) gốc đang bị vô hiệu hóa.");
            }

            // KIỂM TRA ASSESSMENT POLICY LIÊN KẾT ĐÃ ĐƯỢC PUBLISHED HAY CHƯA
            if (!assessmentPolicyRepository.existsPublishedByRubricVersionId(version.getId())) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì chưa có Assessment Policy nào liên kết đang ở trạng thái PUBLISHED.");
            }

            // BẮT BUỘC TẤT CẢ ASSESSMENT POLICY LIÊN KẾT PHẢI ĐANG Ở TRẠNG THÁI PUBLISHED
            if (assessmentPolicyRepository.existsNotPublishedByRubricVersionId(version.getId())) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì vẫn còn Assessment Policy liên kết chưa ở trạng thái PUBLISHED.");
            }

            validateCriteriaMatchScoringScale(version);

            // ĐÃ BỎ LỆNH SAVE RUBRIC DƯ THỪA Ở ĐÂY VÌ MODEL KHÔNG CÒN CURRENTVERSIONID

        } else if (command.status() == RubricStatus.ARCHIVED) {
            throw new IllegalStateException("Hành động bị từ chối: Vui lòng dùng chức năng Lưu trữ (Archive) riêng để chuyển phiên bản sang ARCHIVED.");
        } else if (command.status() == RubricStatus.DRAFT) {
            // CHẶN LỖ HỔNG LÙI TRẠNG THÁI
            throw new IllegalStateException("Hành động bị từ chối: Không thể chuyển một phiên bản đã Ban hành/Lưu trữ quay ngược lại trạng thái Nháp (DRAFT). Vui lòng tạo phiên bản mới.");
        }

        // 5. Lưu trạng thái mới cho Version
        version.setStatus(command.status());
        version.setUpdatedAt(now);
        version.setUpdatedBy(currentUserId);

        rubricVersionRepository.save(version);
        return version.getId();
    }

    /**
     * Chặn ban hành rubric mà phép chấm không thể cho ra điểm có nghĩa.
     *
     * <p>Hai ràng buộc, áp cho CẢ HAI phương pháp tính điểm câu:
     *
     * <ul>
     *   <li>Tổng trọng số bằng 1 -- {@code SUM} nhân trọng số ({@code Σ(điểm × trọng số)}) nên
     *       tổng khác 1 đẩy điểm ra ngoài thang; {@code WEIGHTED_AVERAGE} không dùng trọng số để
     *       tính nhưng vẫn hiển thị tỉ trọng cho người chấm đọc, để tổng lệch là rubric nói dối
     *       về chính nó.
     *   <li>Mỗi tiêu chí phủ ĐÚNG thang -- tiêu chí hẹp hơn thang thì học sinh làm tối đa vẫn
     *       không chạm được trần; rộng hơn thang thì điểm vọt lên rồi bị kẹp.
     * </ul>
     *
     * <p>Bản sao có chủ đích của {@code ChangeSchoolRubricVersionStatusUseCase}. Trước 2026-08-13
     * chỉ đường của TRƯỜNG có cổng này, còn rubric HỆ THỐNG ban hành thẳng không kiểm gì -- mà
     * rubric hệ thống là mẫu dùng chung cho mọi trường, nên một cấu hình sai ở đây lan rộng hơn
     * hẳn. Hậu quả đo được: {@code Σweight = 1.5} trên thang 0-10 cho điểm câu 15, bị kẹp còn 10,
     * mọi bài khá trở lên đều 10/10 mà không có lỗi nào được ném ra.
     */
    private void validateCriteriaMatchScoringScale(RubricVersion version) {
        var criteria = rubricCriterionRepository.findByRubricVersionId(version.getId());
        if (criteria.isEmpty()) {
            throw new IllegalStateException(
                    "Không thể ban hành: phiên bản Rubric này chưa có tiêu chí nào.");
        }

        var weightSum = criteria.stream()
                .map(criterion -> criterion.getWeight())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalStateException(String.format(
                    "Không thể ban hành: tổng trọng số của %d tiêu chí phải bằng 1, hiện đang là %s.",
                    criteria.size(), weightSum.toPlainString()));
        }

        var mismatched = criteria.stream()
                .filter(criterion -> criterion.getMinScore() == null
                        || criterion.getMaxScore() == null
                        || criterion.getMinScore().compareTo(version.getScoringScaleMin()) != 0
                        || criterion.getMaxScore().compareTo(version.getScoringScaleMax()) != 0)
                .findFirst()
                .orElse(null);
        if (mismatched != null) {
            throw new IllegalStateException(String.format(
                    "Không thể ban hành: tiêu chí \"%s\" có khoảng điểm %s - %s, trong khi thang"
                            + " của phiên bản là %s - %s. Mỗi tiêu chí phải chấm trên đúng thang;"
                            + " muốn phân bổ tỉ trọng thì dùng cột trọng số.",
                    mismatched.getName(),
                    mismatched.getMinScore() == null ? "?" : mismatched.getMinScore().toPlainString(),
                    mismatched.getMaxScore() == null ? "?" : mismatched.getMaxScore().toPlainString(),
                    version.getScoringScaleMin().toPlainString(),
                    version.getScoringScaleMax().toPlainString()));
        }
    }
}
