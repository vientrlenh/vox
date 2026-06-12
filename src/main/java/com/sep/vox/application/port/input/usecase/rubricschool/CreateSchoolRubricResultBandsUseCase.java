package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricResultBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkResultBandStatus;
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

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreateSchoolRubricResultBandsUseCase implements IUseCase<CreateSchoolRubricResultBandsCommand, List<UUID>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public CreateSchoolRubricResultBandsUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricResultBandsCommand command) {
        // 1. Xác thực người dùng (BỔ SUNG CHECK ACTIVE)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2 & 3. Validate Version và quyền School (Giữ nguyên của bạn)
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm Thang điểm kết quả khi Rubric đang ở trạng thái DRAFT.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (!rubric.getSchoolId().equals(command.schoolId()) ||
                (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(rubric.getSchoolId()))) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền chỉnh sửa Rubric của trường khác.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4.1 Lấy toàn bộ danh sách ID của FrameworkResultBand từ request
        List<UUID> frameworkBandIds = command.resultBands().stream()
                .map(CreateSchoolRubricResultBandsCommand.ResultBandItemCommand::frameworkResultBandId)
                .distinct()
                .collect(Collectors.toList());

        // 4.2 Gọi Database 1 LẦN DUY NHẤT để lấy tất cả Framework Bands cần thiết
        List<FrameworkResultBand> existingFrameworkBands = frameworkResultBandRepository.findAllByIds(frameworkBandIds);

        // 4.3 Biến List thành Map<ID, Đối_Tượng>
        Map<UUID, FrameworkResultBand> frameworkBandMap = existingFrameworkBands.stream()
                .collect(Collectors.toMap(FrameworkResultBand::getId, band -> band));

        // BỔ SUNG: Khởi tạo Set để chống trùng lặp dữ liệu nội bộ
        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();

        // 4.4 Lặp qua danh sách từ Command và xử lý logic
        List<RubricResultBand> bandsToSave = command.resultBands().stream().map(bCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(bCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(bCmd.name());

            // BỔ SUNG: Check trùng lặp Mã (Code) và FrameworkBandId
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã thang điểm (Code): " + safeCode);
            }
            if (!uniqueFrameworkIds.add(bCmd.frameworkResultBandId())) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Framework Result Band cho thang điểm: " + safeName);
            }

            // Tra cứu từ trong RAM (Map) thay vì gọi xuống Database
            FrameworkResultBand frameworkBand = frameworkBandMap.get(bCmd.frameworkResultBandId());

            if (frameworkBand == null) {
                throw new NotFoundException("Không tìm thấy Framework Result Band với ID: " + bCmd.frameworkResultBandId());
            }

            if (frameworkBand.getStatus() != FrameworkResultBandStatus.PUBLISHED) {
                throw new IllegalStateException("Framework Result Band (Mã: " + frameworkBand.getCode() + ") chưa được PUBLISHED, không thể sử dụng.");
            }

            // Validate logic điểm
            if (bCmd.mappedScoreMin().compareTo(bCmd.mappedScoreMax()) > 0) {
                throw new IllegalArgumentException("Thang điểm '" + safeName + "': Điểm quy đổi tối thiểu không được lớn hơn tối đa.");
            }

            return new RubricResultBand(
                    command.versionId(),
                    bCmd.frameworkResultBandId(),
                    safeCode,
                    safeName,
                    bCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(bCmd.description()) : null,
                    bCmd.mappedScoreMin(),
                    bCmd.mappedScoreMax(),
                    bCmd.order(),
                    bCmd.isPassing(),
                    now, now, currentUserId, currentUserId
            );
        }).collect(Collectors.toList());

        // 5. Lưu hàng loạt xuống Database (BỔ SUNG BỌC LỖI TRY-CATCH)
        try {
            rubricResultBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã thang điểm (Code) hoặc Framework Result Band đã tồn tại trong phiên bản Rubric này từ trước. Vui lòng kiểm tra lại.");
        }

        return bandsToSave.stream().map(RubricResultBand::getId).collect(Collectors.toList());
    }
}