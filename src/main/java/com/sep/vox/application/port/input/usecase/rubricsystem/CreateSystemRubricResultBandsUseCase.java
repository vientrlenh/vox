package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricResultBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkResultBandStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreateSystemRubricResultBandsUseCase implements IUseCase<CreateSystemRubricResultBandsCommand, List<UUID>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserRepository userRepository; // BỔ SUNG USER REPO

    public CreateSystemRubricResultBandsUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserContextPort userContextPort,
            FrameworkResultBandRepository frameworkResultBandRepository, UserRepository userRepository) {
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userContextPort = userContextPort;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSystemRubricResultBandsCommand command) {
        // 1. Xác thực tài khoản (BỔ SUNG CHECK ACTIVE)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Lấy Version và kiểm tra DRAFT
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm Thang điểm kết quả khi Rubric ở trạng thái DRAFT.");
        }

        // 3. Chặn System Admin sửa vào Rubric của Trường học
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về Trường học. System Admin không có quyền can thiệp.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4.1 Lấy danh sách ID của FrameworkResultBands
        List<UUID> frameworkBandIds = command.resultBands().stream()
                .map(CreateSystemRubricResultBandsCommand.ResultBandItemCommand::frameworkResultBandId)
                .distinct()
                .collect(Collectors.toList());

        // 4.2 Gọi DB 1 lần
        List<FrameworkResultBand> existingFrameworkBands = frameworkResultBandRepository.findAllByIds(frameworkBandIds);

        // 4.3 Tạo Map siêu tốc
        Map<UUID, FrameworkResultBand> frameworkBandMap = existingFrameworkBands.stream()
                .collect(Collectors.toMap(FrameworkResultBand::getId, band -> band));

        // BỔ SUNG: Khởi tạo Set để chống trùng lặp dữ liệu nội bộ trên RAM
        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();

        // 4.4 Lặp và tạo thực thể
        List<RubricResultBand> bandsToSave = command.resultBands().stream().map(bCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(bCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(bCmd.name());

            // BỔ SUNG: Check trùng lặp
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã thang điểm (Code): " + safeCode);
            }
            if (!uniqueFrameworkIds.add(bCmd.frameworkResultBandId())) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Framework Result Band cho thang điểm: " + safeName);
            }

            FrameworkResultBand frameworkBand = frameworkBandMap.get(bCmd.frameworkResultBandId());

            if (frameworkBand == null) {
                throw new NotFoundException("Không tìm thấy Framework Result Band với ID: " + bCmd.frameworkResultBandId());
            }

            if (frameworkBand.getStatus() != FrameworkResultBandStatus.PUBLISHED) {
                throw new IllegalStateException("Framework Result Band (Mã: " + frameworkBand.getCode() + ") chưa được PUBLISHED.");
            }

            // Validate Min <= Max
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
                    now, now, currentUserId, currentUserId
            );
        }).collect(Collectors.toList());

        // 5. Lưu hàng loạt (BỔ SUNG TRY-CATCH)
        try {
            rubricResultBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã thang điểm (Code) hoặc Framework Result Band đã tồn tại trong phiên bản Rubric hệ thống này từ trước.");
        }

        return bandsToSave.stream().map(RubricResultBand::getId).collect(Collectors.toList());
    }
}