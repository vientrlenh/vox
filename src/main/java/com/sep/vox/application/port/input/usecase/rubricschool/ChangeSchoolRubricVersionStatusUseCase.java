package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class ChangeSchoolRubricVersionStatusUseCase implements IUseCase<ChangeSchoolRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final FrameworkRepository frameworkRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository; // Dùng Repo mới

    public ChangeSchoolRubricVersionStatusUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricResultBandRepository rubricResultBandRepository,
            FrameworkRepository frameworkRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.frameworkRepository = frameworkRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public UUID execute(ChangeSchoolRubricVersionStatusCommand command) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Lấy Version ra kiểm tra
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() == command.status()) {
            throw new IllegalStateException("Phiên bản đã đang ở trạng thái " + command.status() + ".");
        }

        // 3. KIỂM TRA BẢO MẬT SCHOOL OWNERSHIP (Sử dụng bảng SchoolUser mới)
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));

        // Lấy thông tin mapping trường học của User hiện tại
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        // Xác thực quyền can thiệp
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể chuyển trạng thái Rubric của trường khác.");
        }

        Instant now = Instant.now();

        // 4. XỬ LÝ LOGIC MÁY TRẠNG THÁI (STATE MACHINE) CHẶT CHẼ
        if (command.status() == RubricStatus.PUBLISHED) {

            if (version.getStatus() != RubricStatus.DRAFT) {
                throw new IllegalStateException("Chỉ có thể ban hành (PUBLISH) phiên bản đang ở trạng thái Nháp (DRAFT).");
            }

            // KIỂM TRA VERSION KHÔNG ĐƯỢC RỖNG -- version không có tiêu chí/thang điểm thì không dùng
            // để chấm bài được, publish xong sẽ vô dụng và không thể sửa nữa (ngoại trừ archive)
            if (rubricCriterionRepository.findByRubricVersionId(version.getId()).isEmpty()) {
                throw new IllegalStateException("Không thể ban hành phiên bản này vì chưa có tiêu chí (Criterion) nào.");
            }
            if (rubricResultBandRepository.findByRubricVersionId(version.getId()).isEmpty()) {
                throw new IllegalStateException("Không thể ban hành phiên bản này vì chưa có thang điểm (Result Band) nào.");
            }

            Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework) liên kết."));
            if (!framework.isActive()) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì Khung tiêu chuẩn (Framework) gốc đang bị vô hiệu hóa.");
            }

            // KIỂM TRA ASSESSMENT POLICY LIÊN KẾT ĐÃ ĐƯỢC PUBLISHED HAY CHƯA
            if (!assessmentPolicyRepository.existsPublishedByRubricVersionId(version.getId())) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì chưa có Assessment Policy nào liên kết đang ở trạng thái PUBLISHED.");
            }

            validateCriteriaMatchScoringScale(version);

        } else if (command.status() == RubricStatus.ARCHIVED) {
            throw new IllegalStateException("Hành động bị từ chối: Vui lòng dùng chức năng Lưu trữ (Archive) riêng để chuyển phiên bản sang ARCHIVED.");
        } else if (command.status() == RubricStatus.DRAFT) {
            throw new IllegalStateException("Hành động bị từ chối: Không thể chuyển một phiên bản đã Ban hành/Lưu trữ quay ngược lại trạng thái Nháp (DRAFT). Vui lòng tạo phiên bản mới.");
        }

        // 5. Lưu xuống DB
        version.setStatus(command.status());
        version.setUpdatedAt(now);
        version.setUpdatedBy(currentUserId);

        rubricVersionRepository.save(version);
        return version.getId();
    }

    /**
     * Chốt chặn 2026-08-11: bộ tiêu chí phải khớp với phương pháp tính điểm, nếu không điểm câu sẽ
     * rời khỏi thang rubric.
     *
     * <p>Vì sao kiểm ở ĐÂY: mọi thao tác thêm/sửa/xoá tiêu chí và sửa thang điểm đều chỉ cho phép
     * khi version còn DRAFT (xem {@code CreateSchoolRubricCriterionUseCase},
     * {@code UpdateSchoolRubricCriterionUseCase}, {@code DeleteSchoolRubricCriterionUseCase},
     * {@code UpdateSchoolRubricVersionUseCase}). Nên PUBLISH là cửa duy nhất một cấu hình đi từ
     * "đang soạn" sang "dùng để chấm thật" -- chặn ở đây là kín, và không cần kiểm lặp lại ở từng
     * thao tác sửa lẻ.
     *
     * <p>Hai phương pháp là hai KIỂU KHAI BÁO khác nhau, mỗi kiểu có ràng buộc riêng:
     *
     * <ul>
     *   <li>{@code WEIGHTED_AVERAGE} -- mỗi tiêu chí chấm trên cả thang, trọng số phân bổ tỉ lệ.
     *       Tổng trọng số phải bằng 1.
     *   <li>{@code SUM} -- mỗi tiêu chí là một lát điểm thành phần (thang 10 = 2+2+2+2+2), cộng
     *       thẳng không qua trọng số. Tổng khoảng điểm các tiêu chí phải phủ đúng thang.
     * </ul>
     *
     * <p>Lỗi đã gặp thật: chọn {@code SUM} nhưng để mỗi tiêu chí trong 5 tiêu chí tối đa 10 điểm
     * trên thang 0-10. Điểm câu cộng ra 38.4, bị kẹp còn 10 -- mọi bài đều 10/10.
     */
    private void validateCriteriaMatchScoringScale(RubricVersion version) {
        var criteria = rubricCriterionRepository.findByRubricVersionId(version.getId());
        if (criteria.isEmpty()) {
            throw new IllegalStateException(
                    "Không thể ban hành: phiên bản Rubric này chưa có tiêu chí nào.");
        }

        if (version.getTotalScoreMethod() == RubricTotalScoreMethod.SUM) {
            var minSum = criteria.stream().map(RubricCriterion::getMinScore)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var maxSum = criteria.stream().map(RubricCriterion::getMaxScore)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (maxSum.compareTo(version.getScoringScaleMax()) != 0
                    || minSum.compareTo(version.getScoringScaleMin()) != 0) {
                throw new IllegalStateException(String.format(
                        "Không thể ban hành: phương pháp SUM cộng thẳng điểm các tiêu chí, nên"
                                + " khoảng điểm của %d tiêu chí phải cộng lại vừa đúng thang của phiên bản."
                                + " Hiện tổng là %s - %s, trong khi thang là %s - %s."
                                + " Hãy chia nhỏ điểm tối đa của từng tiêu chí, hoặc đổi sang WEIGHTED_AVERAGE.",
                        criteria.size(), minSum.toPlainString(), maxSum.toPlainString(),
                        version.getScoringScaleMin().toPlainString(),
                        version.getScoringScaleMax().toPlainString()));
            }
            return;
        }

        var weightSum = criteria.stream().map(RubricCriterion::getWeight)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalStateException(String.format(
                    "Không thể ban hành: phương pháp WEIGHTED_AVERAGE yêu cầu tổng trọng số của"
                            + " %d tiêu chí phải bằng 1, hiện đang là %s.",
                    criteria.size(), weightSum.toPlainString()));
        }

        // Mỗi tiêu chí phải phủ ĐÚNG thang. Trung bình có trọng số của các giá trị trong [a,b] thì
        // nằm trong [a,b] -- nhưng chỉ khi MỌI tiêu chí cùng nằm trong [a,b] đó. Lệch một trong hai
        // đầu là hỏng theo hai kiểu:
        //   - tiêu chí RỘNG hơn thang (thang 4-10, tiêu chí 0-100): trung bình vọt lên tới 100 rồi
        //     bị kẹp còn 10, mọi bài khá trở lên đều thành 10.
        //   - tiêu chí HẸP hơn thang (thang 4-10, một tiêu chí 4-5): học sinh làm tối đa mọi thứ
        //     vẫn không bao giờ chạm được 10, trần thang thành trần chết.
        // Đối xứng với ràng buộc của SUM ở trên -- SUM đòi các khoảng CỘNG LẠI phủ thang, còn
        // WEIGHTED_AVERAGE đòi từng khoảng phủ thang.
        var mismatched = criteria.stream()
                .filter(criterion -> criterion.getMinScore() == null
                        || criterion.getMaxScore() == null
                        || criterion.getMinScore().compareTo(version.getScoringScaleMin()) != 0
                        || criterion.getMaxScore().compareTo(version.getScoringScaleMax()) != 0)
                .findFirst()
                .orElse(null);
        if (mismatched != null) {
            throw new IllegalStateException(String.format(
                    "Không thể ban hành: tiêu chí \"%s\" có khoảng điểm %s - %s, trong khi thang của"
                            + " phiên bản là %s - %s. Với WEIGHTED_AVERAGE, mỗi tiêu chí phải phủ"
                            + " đúng thang (chênh lệch giữa các tiêu chí thể hiện bằng TRỌNG SỐ,"
                            + " không phải bằng khoảng điểm).",
                    mismatched.getName(),
                    mismatched.getMinScore() == null ? "?" : mismatched.getMinScore().toPlainString(),
                    mismatched.getMaxScore() == null ? "?" : mismatched.getMaxScore().toPlainString(),
                    version.getScoringScaleMin().toPlainString(),
                    version.getScoringScaleMax().toPlainString()));
        }
    }
}
