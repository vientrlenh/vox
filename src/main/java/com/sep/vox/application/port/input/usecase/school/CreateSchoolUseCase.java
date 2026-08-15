package com.sep.vox.application.port.input.usecase.school;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

/**
 * Tạo trường TRỰC TIẾP, không qua đơn đăng ký -- SYSTEM_ADMIN nhập thẳng thông tin trường và
 * quản trị viên, bỏ toàn bộ vòng RegisterForm (OTP theo domain / nộp tài liệu / chờ duyệt) mà
 * RegisterFromSchoolDirectoryUseCase + ApproveRegisterFormUseCase phải đi qua.
 *
 * <p>Phần lưu dữ liệu KHÔNG viết lại: dùng chung ProvisionSchoolService với luồng duyệt đơn, nên
 * chống trùng (mã trường, domain, email, số điện thoại), tạo School + User quản trị + vai trò
 * SCHOOL_ADMIN + liên kết SchoolUser, và phát sự kiện USER_CREATED đều giống hệt. Sự kiện đó là
 * thứ khiến quản trị viên mới nhận mail đặt mật khẩu như mọi người dùng khác -- token sinh ở
 * consumer ngay trước lúc gửi, không nằm plaintext trong outbox/Kafka.
 */
@Service
public class CreateSchoolUseCase implements IUseCase<CreateSchoolCommand, UUID> {

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final ProvisionSchoolService provisionSchoolService;
    private final UserContextPort userContextPort;

    public CreateSchoolUseCase(
            SchoolDirectoryRepository schoolDirectoryRepository,
            ProvisionSchoolService provisionSchoolService,
            UserContextPort userContextPort) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.provisionSchoolService = provisionSchoolService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolCommand input) {
        var command = normalize(input);
        validateCommand(command);

        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var school = resolveSchoolInfo(command, currentUserId, now);

        return provisionSchoolService.provision(new ProvisionSchoolCommand(
            school.code(),
            school.name(),
            null,
            school.domain(),
            school.address(),
            command.studentCount(),
            command.adminEmail(),
            command.adminPhone(),
            command.adminFullName(),
            command.adminDateOfBirth(),
            command.adminAddress(),
            command.adminAvatarUrl(),
            currentUserId,
            now
        ));
    }

    private CreateSchoolCommand normalize(CreateSchoolCommand input) {
        return new CreateSchoolCommand(
            input.schoolDirectoryId(),
            StringNormalization.normalizeCode(input.schoolCode()),
            StringNormalization.trimAndCollapseSpaces(input.schoolName()),
            StringNormalization.trimAndCollapseSpaces(input.schoolAddress()),
            StringNormalization.normalizeDomain(input.schoolDomain()),
            input.studentCount(),
            StringNormalization.normalizeEmail(input.adminEmail()),
            StringNormalization.normalizePhone(input.adminPhone()),
            StringNormalization.trimAndCollapseSpaces(input.adminFullName()),
            input.adminDateOfBirth(),
            StringNormalization.trimAndCollapseSpaces(input.adminAddress()),
            StringNormalization.trimAndCollapseSpaces(input.adminAvatarUrl())
        );
    }

    private void validateCommand(CreateSchoolCommand command) {
        if (command.schoolDirectoryId() == null && (command.schoolCode() == null || command.schoolCode().isBlank() || command.schoolName() == null || command.schoolName().isBlank() || command.schoolAddress() == null || command.schoolAddress().isBlank())) {
            throw new IllegalArgumentException("Yêu cầu cần phải có danh mục trường, hoặc phải có mã trường, tên trường và địa chỉ trường");
        }
    }

    /**
     * Ưu tiên danh mục: có schoolDirectoryId thì LẤY TRỌN thông tin trường từ entry đó và bỏ qua
     * phần tự khai trong yêu cầu -- không trộn hai nguồn, tránh trường mang mã/tên của danh mục
     * này nhưng domain lại do người gọi tự gõ.
     */
    private SchoolInfo resolveSchoolInfo(CreateSchoolCommand command, UUID currentUserId, Instant now) {
        if (command.schoolDirectoryId() == null) {
            return new SchoolInfo(
                command.schoolCode(),
                command.schoolName(),
                command.schoolAddress(),
                command.schoolDomain()
            );
        }

        var directory = schoolDirectoryRepository.findById(command.schoolDirectoryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));

        // KHÔNG chặn khi danh mục chưa xác minh -- API này cố tình bỏ vòng duyệt, và chính việc
        // SYSTEM_ADMIN tạo trường ở đây đã là hành vi xác minh. Đánh dấu luôn để không còn trạng
        // thái lệch "School đã tồn tại nhưng danh mục vẫn chưa xác minh" (cùng cách
        // ApproveRegisterFormUseCase xử lý đơn từ danh mục).
        if (!directory.isVerified()) {
            directory.verify(currentUserId, now);
            schoolDirectoryRepository.save(directory);
        }

        return new SchoolInfo(
            directory.getCode(),
            directory.getName(),
            directory.getAddress(),
            directory.getDomain()
        );
    }

    /** Bốn trường định danh trường, sau khi đã chốt lấy từ danh mục hay từ phần tự khai. */
    private record SchoolInfo(String code, String name, String address, String domain) {
    }
}
