package com.sep.vox.application.usecase.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.school.CreateSchoolUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

/**
 * Tạo trường không qua đơn đăng ký. Điểm cần khoá chặt: khi có danh mục thì mọi thông tin trường
 * phải lấy TRỌN từ danh mục (không trộn với phần tự khai), và danh mục chưa xác minh vẫn dùng được
 * -- đó là toàn bộ lý do API này tồn tại bên cạnh luồng RegisterForm.
 */
class CreateSchoolUseCaseTests {

    private SchoolDirectoryRepository schoolDirectoryRepository;
    private ProvisionSchoolService provisionSchoolService;
    private UserContextPort userContextPort;
    private CreateSchoolUseCase useCase;

    private static final String ALLOWED_AVATAR_HOSTS = "firebasestorage.googleapis.com";
    private static final String VALID_AVATAR_URL =
        "https://firebasestorage.googleapis.com/v0/b/vox.appspot.com/o/avatars%2Fa%2Fb.png?alt=media";

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID directoryId = UUID.randomUUID();
    private final UUID createdSchoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolDirectoryRepository = mock(SchoolDirectoryRepository.class);
        provisionSchoolService = mock(ProvisionSchoolService.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSchoolUseCase(
            schoolDirectoryRepository,
            provisionSchoolService,
            userContextPort,
            ALLOWED_AVATAR_HOSTS
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(provisionSchoolService.provision(any())).thenReturn(createdSchoolId);
    }

    @Test
    void should_take_all_school_info_from_directory_when_directory_id_given() {
        givenDirectory(SchoolDirectory.createByAdmin(
            "THPT-A", "Trường THPT A", "01", "Hà Nội", "Ba Đình",
            "thpta.edu.vn", "12 Phố A", Instant.now(), currentUserId));

        // Phần tự khai bên dưới cố tình khác hẳn danh mục -- không được lọt vào kết quả.
        useCase.execute(commandWith(directoryId, "TU-KHAI", "Tên tự khai", "Địa chỉ tự khai", "tukhai.vn"));

        var provisioned = captureProvisioned();
        assertThat(provisioned.schoolCode()).isEqualTo("THPT-A");
        assertThat(provisioned.schoolName()).isEqualTo("Trường THPT A");
        assertThat(provisioned.schoolAddress()).isEqualTo("12 Phố A");
        assertThat(provisioned.schoolDomain()).isEqualTo("thpta.edu.vn");
    }

    @Test
    void should_create_from_unverified_directory_and_mark_it_verified() {
        var unverified = SchoolDirectory.createByUserSubmitted(
            "THPT-B", "Trường THPT B", "02", "Huế", "Phú Vang",
            "thptb.edu.vn", "34 Phố B", Instant.now(), currentUserId);
        assertThat(unverified.isVerified()).isFalse();
        givenDirectory(unverified);

        useCase.execute(commandWith(directoryId, null, null, null, null));

        assertThat(unverified.isVerified()).isTrue();
        verify(schoolDirectoryRepository).save(unverified);
    }

    @Test
    void should_not_save_directory_when_already_verified() {
        givenDirectory(SchoolDirectory.createByAdmin(
            "THPT-C", "Trường THPT C", "03", "Đà Nẵng", "Hải Châu",
            "thptc.edu.vn", "56 Phố C", Instant.now(), currentUserId));

        useCase.execute(commandWith(directoryId, null, null, null, null));

        verify(schoolDirectoryRepository, never()).save(any());
    }

    @Test
    void should_use_normalized_self_declared_info_when_no_directory_id() {
        useCase.execute(commandWith(null, "  thpt-d  ", "  Trường   THPT D ", " 78 Phố D ", " THPTD.EDU.VN "));

        verify(schoolDirectoryRepository, never()).findById(any());
        var provisioned = captureProvisioned();
        assertThat(provisioned.schoolCode()).isEqualTo("THPT-D");
        assertThat(provisioned.schoolName()).isEqualTo("Trường THPT D");
        assertThat(provisioned.schoolAddress()).isEqualTo("78 Phố D");
        assertThat(provisioned.schoolDomain()).isEqualTo("thptd.edu.vn");
    }

    @Test
    void should_return_created_school_id() {
        givenDirectory(SchoolDirectory.createByAdmin(
            "THPT-E", "Trường THPT E", "04", "Cần Thơ", "Ninh Kiều",
            null, "90 Phố E", Instant.now(), currentUserId));

        assertThat(useCase.execute(commandWith(directoryId, null, null, null, null)))
            .isEqualTo(createdSchoolId);
    }

    @Test
    void should_throw_when_neither_directory_nor_self_declared_info_given() {
        assertThatThrownBy(() -> useCase.execute(commandWith(null, null, "Trường THPT F", "12 Phố F", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("danh mục trường");

        verify(provisionSchoolService, never()).provision(any());
    }

    @Test
    void should_throw_when_directory_not_found() {
        when(schoolDirectoryRepository.findById(directoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(commandWith(directoryId, null, null, null, null)))
            .isInstanceOf(NotFoundException.class);

        verify(provisionSchoolService, never()).provision(any());
    }

    @Test
    void should_pass_valid_avatar_url_through_to_provisioning() {
        var command = commandWith(null, "THPT-A", "THPT A", "1 Phố A", "thpta.edu.vn", VALID_AVATAR_URL);

        useCase.execute(command);

        assertThat(captureProvisioned().avatarUrl()).isEqualTo(VALID_AVATAR_URL);
    }

    /** Ô này trước đây nhận URL bất kỳ -- chính là lỗ mà AvatarUrlPolicy sinh ra để bịt. */
    @Test
    void should_reject_avatar_url_from_foreign_host() {
        var command = commandWith(null, "THPT-A", "THPT A", "1 Phố A", "thpta.edu.vn",
            "https://evil.example/pixel.gif");

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class);
        verify(provisionSchoolService, never()).provision(any());
    }

    @Test
    void should_create_school_when_avatar_url_is_blank() {
        var command = commandWith(null, "THPT-A", "THPT A", "1 Phố A", "thpta.edu.vn", "   ");

        useCase.execute(command);

        assertThat(captureProvisioned().avatarUrl()).isNull();
    }

    private void givenDirectory(SchoolDirectory directory) {
        when(schoolDirectoryRepository.findById(directoryId)).thenReturn(Optional.of(directory));
    }

    private ProvisionSchoolCommand captureProvisioned() {
        var captor = ArgumentCaptor.forClass(ProvisionSchoolCommand.class);
        verify(provisionSchoolService).provision(captor.capture());
        return captor.getValue();
    }

    private CreateSchoolCommand commandWith(
            UUID schoolDirectoryId, String code, String name, String address, String domain) {
        return commandWith(schoolDirectoryId, code, name, address, domain, null);
    }

    private CreateSchoolCommand commandWith(
            UUID schoolDirectoryId, String code, String name, String address, String domain,
            String adminAvatarUrl) {
        return new CreateSchoolCommand(
            schoolDirectoryId,
            code,
            name,
            address,
            domain,
            500,
            "Admin@Truong.Edu.VN",
            "0901 234 567",
            "  Nguyễn  Văn A ",
            LocalDate.of(1985, 6, 15),
            "  1 Phố Quản Trị ",
            adminAvatarUrl
        );
    }
}
