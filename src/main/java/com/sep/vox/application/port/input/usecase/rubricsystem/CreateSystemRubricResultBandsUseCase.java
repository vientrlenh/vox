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
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
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
    private final UserRepository userRepository;

    public CreateSystemRubricResultBandsUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserContextPort userContextPort,
            FrameworkResultBandRepository frameworkResultBandRepository,
            UserRepository userRepository) {
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
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Lấy Version và kiểm tra trạng thái DRAFT
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm Thang điểm kết quả khi Rubric ở trạng thái DRAFT.");
        }

        // 3. Phân quyền: Chặn System Admin sửa vào Rubric của Trường học
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về Trường học. System Admin không có quyền can thiệp.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // CHỐNG N+1 (PHA SELECT): Gom tất cả ID lại và truy vấn DB 1 lần duy nhất
        List<UUID> frameworkBandIds = command.resultBands().stream()
                .map(CreateSystemRubricResultBandsCommand.ResultBandItemCommand::frameworkResultBandId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, FrameworkResultBand> frameworkBandMap;
        if (!frameworkBandIds.isEmpty()) {
            List<FrameworkResultBand> existingFrameworkBands = frameworkResultBandRepository.findAllByIds(frameworkBandIds);

            // Xây dựng HashMap trên RAM để tra cứu nhanh O(1) ở vòng lặp bên dưới
            frameworkBandMap = existingFrameworkBands.stream()
                    .collect(Collectors.toMap(FrameworkResultBand::getId, band -> band));
        } else {
            frameworkBandMap = new HashMap<>();
        }

        // Bộ nhớ tạm để kiểm tra trùng lặp dữ liệu đầu vào (O(1) lookup)
        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();

        // 4. Lặp và build thực thể (Toàn bộ thao tác diễn ra trên RAM)
        List<RubricResultBand> bandsToSave = command.resultBands().stream().map(bCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(bCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(bCmd.name());

            // 4.1 Validate trùng lặp từ request
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã thang điểm (Code): " + safeCode);
            }
            if (bCmd.frameworkResultBandId() != null && !uniqueFrameworkIds.add(bCmd.frameworkResultBandId())) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Framework Result Band cho thang điểm: " + safeName);
            }

            // 4.2 Lấy dữ liệu từ Map trên RAM (Không gọi DB)
            FrameworkResultBand frameworkBand =     frameworkBandMap.get(bCmd.frameworkResultBandId());

            if (frameworkBand == null) {
                throw new NotFoundException("Không tìm thấy Framework Result Band với ID: " + bCmd.frameworkResultBandId());
            }

            if (frameworkBand.getStatus() != FrameworkResultBandStatus.PUBLISHED) {
                throw new IllegalStateException("Framework Result Band (Mã: " + frameworkBand.getCode() + ") chưa được PUBLISHED.");
            }

            // 4.3 Validate Min/Max Score
            if (bCmd.mappedScoreMin().compareTo(bCmd.mappedScoreMax()) > 0) {
                throw new IllegalArgumentException("Thang điểm '" + safeName + "': Điểm quy đổi tối thiểu không được lớn hơn tối đa.");
            }

            // 4.4 Build Object với ID = null để báo cho Hibernate biết đây là dữ liệu mới
            return new RubricResultBand(
                    null,                 // ĐỂ NULL: Chặn đứng lệnh SELECT dư thừa trước khi INSERT
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
        }).toList();

        // CHỐNG N+1 (PHA INSERT): Đẩy nguyên List xuống Repository bằng saveAll
        List<RubricResultBand> savedBands;
        try {
            savedBands = rubricResultBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã thang điểm (Code) hoặc Framework Result Band đã tồn tại trong phiên bản Rubric hệ thống này từ trước.");
        }

        // 5. Trích xuất ID từ List trả về và gửi cho Client
        return savedBands.stream().map(RubricResultBand::getId).toList();
    }
}