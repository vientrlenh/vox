package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.service.rubric.RubricResultBandValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricResultBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CreateSchoolRubricResultBandsUseCase implements IUseCase<CreateSchoolRubricResultBandsCommand, List<UUID>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    public CreateSchoolRubricResultBandsUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository) {
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricResultBandsCommand command) {
        // 1. Xác thực người dùng
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2 & 3. Validate Version và quyền School
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm Thang điểm kết quả khi Rubric đang ở trạng thái DRAFT.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        // Lấy thông tin liên kết trường học của User hiện tại
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        // Xác thực quyền chỉnh sửa
        if (rubric.getSchoolId() == null || !rubric.getSchoolId().equals(command.schoolId()) || !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền chỉnh sửa Rubric của trường khác.");
        }

        // Kiểm tra xem trường học có đang hoạt động không
        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Khởi tạo Set để chống trùng lặp dữ liệu nội bộ
        Set<String> uniqueCodes = new HashSet<>();

        // Tích luỹ band đã có trong version + các band mới trong cùng batch để check overlap khoảng điểm.
        // Dùng TreeMap (key = scoreMin) để check O(log n)/band thay vì quét List O(n)/band (O(n log n) cho cả batch thay vì O(n^2)).
        NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin = new TreeMap<>();
        rubricResultBandRepository.findByRubricVersionId(command.versionId())
                .forEach(b -> bandsSoFarByMin.put(b.getScoreMin(), b));

        // 4.4 Lặp qua danh sách từ Command và xử lý logic
        List<RubricResultBand> bandsToSave = command.resultBands().stream().map(bCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(bCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(bCmd.name());

            // Check trùng lặp Mã (Code)
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã thang điểm (Code): " + safeCode);
            }

            // Validate logic điểm
            if (bCmd.mappedScoreMin().compareTo(bCmd.mappedScoreMax()) > 0) {
                throw new IllegalArgumentException("Thang điểm '" + safeName + "': Điểm quy đổi tối thiểu không được lớn hơn tối đa.");
            }

            // Validate không chồng lấn với các band đã có/đang tạo cùng batch
            RubricResultBandValidator.assertNoOverlap(bandsSoFarByMin, bCmd.mappedScoreMin(), bCmd.mappedScoreMax(), safeName);

            // Validate nằm trong thang điểm tổng của RubricVersion
            ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                    bCmd.mappedScoreMin(), bCmd.mappedScoreMax(), safeName);

            // Để ID là null để đảm bảo JPA hiểu đây là record mới, chừa cho DB sinh UUID
            RubricResultBand band = new RubricResultBand(
                    null,
                    command.versionId(),
                    safeCode,
                    safeName,
                    bCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(bCmd.description()) : null,
                    bCmd.mappedScoreMin(),
                    bCmd.mappedScoreMax(),
                    bCmd.order(),
                    now,
                    now,
                    currentUserId,
                    currentUserId
            );
            bandsSoFarByMin.put(band.getScoreMin(), band);
            return band;
        }).toList();

        // 5. Lưu hàng loạt xuống Database
        List<RubricResultBand> savedBands;
        try {
            savedBands = rubricResultBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã thang điểm (Code) đã tồn tại trong phiên bản Rubric này từ trước. Vui lòng kiểm tra lại.");
        }

        // Bốc ID từ cái mảng savedBands (đã được DB gắn ID) chứ không phải mảng bandsToSave gốc
        return savedBands.stream().map(b -> b.getId()).toList();
    }
}