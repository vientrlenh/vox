package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.domain.model.school.SchoolDirectory;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.AvatarUrlPolicy;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.school.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.school.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AvatarConfigPort;
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
@RequiredArgsConstructor
public class CreateSchoolUseCase implements IUseCase<CreateSchoolCommand, UUID> {

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final ProvisionSchoolService provisionSchoolService;
    private final AvatarConfigPort avatarConfigPort;

    @Override
    @Transactional
    public UUID execute(CreateSchoolCommand input) {
        CreateSchoolCommand command = normalize(input);
        validateCommand(command);

        Instant now = Instant.now();
        SchoolInfo school = resolveSchoolInfo(command);

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
            // KHÔNG dùng trimAndCollapseSpaces như các trường chữ khác: "gộp khoảng trắng" là thao
            // tác vô nghĩa với URL, và quan trọng hơn là nó không kiểm gì cả -- trước đây ô này
            // nhận URL bất kỳ, tức là mọi giáo viên mở danh sách chấm sẽ tự gọi sang host lạ đó.
            // Xem AvatarUrlPolicy. Đây là đường DUY NHẤT có avatar do người dùng nhập trong nhóm
            // gọi ProvisionSchoolService; hai đường còn lại (duyệt đơn, xác thực OTP) truyền null.
            AvatarUrlPolicy.normalizeOrThrow(avatarConfigPort.allowedHosts(), input.adminAvatarUrl())
        );
    }

    private void validateCommand(CreateSchoolCommand command) {
        if (command.schoolDirectoryId() == null && (command.schoolCode() == null || command.schoolCode().isBlank() || command.schoolName() == null || command.schoolName().isBlank() || command.schoolAddress() == null || command.schoolAddress().isBlank())) {
            throw new IllegalArgumentException("Yêu cầu cần phải có danh mục trường, hoặc phải có mã trường, tên trường và địa chỉ trường");
        }
    }

    /**
     * The directory takes priority: with a schoolDirectoryId, ALL school information is TAKEN from that entry and
     * the self-declared part of the request is ignored -- the two sources are not mixed, so a school never carries
     * the code/name of this directory entry while its domain was typed by the caller.
     */
    private SchoolInfo resolveSchoolInfo(CreateSchoolCommand command) {
        if (command.schoolDirectoryId() == null) {
            return new SchoolInfo(
                command.schoolCode(),
                command.schoolName(),
                command.schoolAddress(),
                command.schoolDomain()
            );
        }

        SchoolDirectory directory = verifiedDirectory(command.schoolDirectoryId());

        return new SchoolInfo(
            directory.getCode(),
            directory.getName(),
            directory.getAddress(),
            directory.getDomain()
        );
    }

    private SchoolDirectory verifiedDirectory(UUID schoolDirectoryId) {
        SchoolDirectory directory = schoolDirectoryRepository.findById(schoolDirectoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));

        // Do NOT block when the directory entry is unverified -- this API deliberately skips the approval loop,
        // and a SYSTEM_ADMIN creating the school here is itself the act of verification. Mark it right away so
        // there is no mismatched state "the School exists but the directory entry is still unverified" (the same
        // way ApproveRegisterFormUseCase handles a form from the directory).
        if (!directory.isVerified()) {
            directory.verify();
            schoolDirectoryRepository.save(directory);
        }
        return directory;
    }

    /** Bốn trường định danh trường, sau khi đã chốt lấy từ danh mục hay từ phần tự khai. */
    private record SchoolInfo(String code, String name, String address, String domain) {
    }
}
