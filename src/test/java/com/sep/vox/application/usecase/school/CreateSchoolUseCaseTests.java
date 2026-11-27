package com.sep.vox.application.usecase.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.school.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.school.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.school.CreateSchoolUseCase;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

/**
 * Creating a school without a register form. The point to pin down: when a directory entry is given, ALL school
 * information must come ENTIRELY from the directory (not mixed with the self-declared part), and an unverified
 * directory entry is still usable -- that is the whole reason this API exists next to the RegisterForm flow.
 */
class CreateSchoolUseCaseTests {

    private SchoolDirectoryRepository schoolDirectoryRepository;
    private ProvisionSchoolService provisionSchoolService;
    private CreateSchoolUseCase useCase;

    private static final String ALLOWED_AVATAR_HOSTS = "firebasestorage.googleapis.com";
    private static final String VALID_AVATAR_URL =
        "https://firebasestorage.googleapis.com/v0/b/vox.appspot.com/o/avatars%2Fa%2Fb.png?alt=media";

    private final UUID directoryId = UUID.randomUUID();
    private final UUID createdSchoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolDirectoryRepository = mock(SchoolDirectoryRepository.class);
        provisionSchoolService = mock(ProvisionSchoolService.class);
        useCase = new CreateSchoolUseCase(
            schoolDirectoryRepository,
            provisionSchoolService,
            () -> ALLOWED_AVATAR_HOSTS
        );
        when(provisionSchoolService.provision(any())).thenReturn(createdSchoolId);
    }

    @Test
    void should_take_all_school_info_from_directory_when_directory_id_given() {
        givenDirectory(SchoolDirectory.createByAdmin(
            "THPT-A",
            "Trường THPT A",
            "01",
            "Hà Nội",
            "Ba Đình",
            "thpta.edu.vn",
            "12 Phố A"));

        // The self-declared part below deliberately differs from the catalog -- it must not make it
        // into the result.
        useCase.execute(commandWith(directoryId, "TU-KHAI", "Tên tự khai", "Địa chỉ tự khai", "tukhai.vn"));

        ProvisionSchoolCommand provisioned = captureProvisioned();
        assertThat(provisioned.schoolCode()).isEqualTo("THPT-A");
        assertThat(provisioned.schoolName()).isEqualTo("Trường THPT A");
        assertThat(provisioned.schoolAddress()).isEqualTo("12 Phố A");
        assertThat(provisioned.schoolDomain()).isEqualTo("thpta.edu.vn");
    }

    @Test
    void should_create_from_unverified_directory_and_mark_it_verified() {
        SchoolDirectory unverified = SchoolDirectory.createByUserSubmitted(
            "THPT-B",
            "Trường THPT B",
            "02",
            "Huế",
            "Phú Vang",
            "thptb.edu.vn",
            "34 Phố B");
        assertThat(unverified.isVerified()).isFalse();
        givenDirectory(unverified);

        useCase.execute(commandWith(directoryId, null, null, null, null));

        assertThat(unverified.isVerified()).isTrue();
        verify(schoolDirectoryRepository).save(unverified);
    }

    @Test
    void should_not_save_directory_when_already_verified() {
        givenDirectory(SchoolDirectory.createByAdmin(
            "THPT-C",
            "Trường THPT C",
            "03",
            "Đà Nẵng",
            "Hải Châu",
            "thptc.edu.vn",
            "56 Phố C"));

        useCase.execute(commandWith(directoryId, null, null, null, null));

        verify(schoolDirectoryRepository, never()).save(any());
    }

    @Test
    void should_use_normalized_self_declared_info_when_no_directory_id() {
        useCase.execute(commandWith(null, "  thpt-d  ", "  Trường   THPT D ", " 78 Phố D ", " THPTD.EDU.VN "));

        verify(schoolDirectoryRepository, never()).findById(any());
        ProvisionSchoolCommand provisioned = captureProvisioned();
        assertThat(provisioned.schoolCode()).isEqualTo("THPT-D");
        assertThat(provisioned.schoolName()).isEqualTo("Trường THPT D");
        assertThat(provisioned.schoolAddress()).isEqualTo("78 Phố D");
        assertThat(provisioned.schoolDomain()).isEqualTo("thptd.edu.vn");
    }

    @Test
    void should_return_created_school_id() {
        givenDirectory(SchoolDirectory.createByAdmin(
            "THPT-E",
            "Trường THPT E",
            "04",
            "Cần Thơ",
            "Ninh Kiều",
            null,
            "90 Phố E"));

        assertThat(useCase.execute(commandWith(directoryId, null, null, null, null)))
            .isEqualTo(createdSchoolId);
    }

    @Test
    void should_throw_when_neither_directory_nor_self_declared_info_given() {
        assertThatThrownBy(() -> useCase.execute(commandWith(null, null, "Trường THPT F", "12 Phố F", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("school directory entry");

        verify(provisionSchoolService, never()).provision(any());
    }

    @Test
    void should_throw_when_directory_not_found() {
        when(schoolDirectoryRepository.findById(directoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(commandWith(directoryId, null, null, null, null)))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(provisionSchoolService, never()).provision(any());
    }

    @Test
    void should_pass_valid_avatar_url_through_to_provisioning() {
        CreateSchoolCommand command = commandWith(null, "THPT-A", "THPT A", "1 Phố A", "thpta.edu.vn", VALID_AVATAR_URL);

        useCase.execute(command);

        assertThat(captureProvisioned().avatarUrl()).isEqualTo(VALID_AVATAR_URL);
    }

    /**
     * This field used to accept any URL -- exactly the hole AvatarUrlPolicy was created to plug.
     */
    @Test
    void should_reject_avatar_url_from_foreign_host() {
        CreateSchoolCommand command = commandWith(null, "THPT-A", "THPT A", "1 Phố A", "thpta.edu.vn",
            "https://evil.example/pixel.gif");

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class);
        verify(provisionSchoolService, never()).provision(any());
    }

    @Test
    void should_create_school_when_avatar_url_is_blank() {
        CreateSchoolCommand command = commandWith(null, "THPT-A", "THPT A", "1 Phố A", "thpta.edu.vn", "   ");

        useCase.execute(command);

        assertThat(captureProvisioned().avatarUrl()).isNull();
    }

    private void givenDirectory(SchoolDirectory directory) {
        when(schoolDirectoryRepository.findById(directoryId)).thenReturn(Optional.of(directory));
    }

    private ProvisionSchoolCommand captureProvisioned() {
        ArgumentCaptor<ProvisionSchoolCommand> captor = ArgumentCaptor.forClass(ProvisionSchoolCommand.class);
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
