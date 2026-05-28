package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;
import com.sep.vox.infrastructure.persistence.adapter.RegisterFormRepositoryImpl;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    RegisterFormRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RegisterFormRepositoryTests {

    @Autowired
    private RegisterFormRepository registerFormRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenSave_thenReturnsPersistedRegisterForm() {
        var registerForm = newRegisterForm("111111111", "0987654311", "register-save@example.com", "save-school.edu.vn");

        var saved = registerFormRepository.save(registerForm);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIdentityNumber().value()).isEqualTo("111111111");
        assertThat(saved.getContactEmail().value()).isEqualTo("register-save@example.com");
        assertThat(saved.getStatus()).isEqualTo(RegisterFormStatus.PENDING);
    }

    @Test
    void whenFindById_thenReturnsRegisterForm() {
        var saved = registerFormRepository.save(
            newRegisterForm("222222222", "0987654312", "register-find@example.com", "find-school.edu.vn")
        );

        var found = registerFormRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getContactEmail().value()).isEqualTo("register-find@example.com");
    }

    @Test
    void whenFindAll_thenReturnsPagedRegisterForms() {
        registerFormRepository.save(newRegisterForm("333333333", "0987654313", "register-page-1@example.com", "page-1-school.edu.vn"));
        registerFormRepository.save(newRegisterForm("444444444", "0987654314", "register-page-2@example.com", "page-2-school.edu.vn"));
        registerFormRepository.save(newRegisterForm("555555555", "0987654315", "register-page-3@example.com", "page-3-school.edu.vn"));

        var found = registerFormRepository.findAll(new PageRequest(1, 2));

        assertThat(found.content()).hasSize(2);
        assertThat(found.page()).isZero();
        assertThat(found.size()).isEqualTo(2);
        assertThat(found.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(found.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void whenFindByIdForUpdate_thenReturnsRegisterForm() {
        var saved = registerFormRepository.save(
            newRegisterForm("666666666", "0987654316", "register-lock@example.com", "lock-school.edu.vn")
        );

        var found = registerFormRepository.findByIdForUpdate(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void whenUpdateApprovedRegisterFormForPendingForm_thenApprovesForm() {
        var saved = registerFormRepository.save(
            newRegisterForm("777777777", "0987654317", "register-approve@example.com", "approve-school.edu.vn")
        );
        entityManager.flush();

        var updatedBy = UUID.randomUUID();
        var updatedAt = OffsetDateTime.now();
        var updated = registerFormRepository.updateApprovedRegisterForm(saved.getId(), updatedBy, updatedAt);
        entityManager.clear();

        var found = registerFormRepository.findById(saved.getId());
        assertThat(updated).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(RegisterFormStatus.APPROVED);
        assertThat(found.get().getUpdatedBy()).isEqualTo(updatedBy);
    }

    @Test
    void whenUpdateRejectedRegisterFormForPendingForm_thenRejectsForm() {
        var saved = registerFormRepository.save(
            newRegisterForm("888888888", "0987654318", "register-reject@example.com", "reject-school.edu.vn")
        );
        entityManager.flush();

        var updatedBy = UUID.randomUUID();
        var updated = registerFormRepository.updateRejectedRegisterForm(saved.getId(), updatedBy, "Invalid documents", OffsetDateTime.now());
        entityManager.clear();

        var found = registerFormRepository.findById(saved.getId());
        assertThat(updated).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(RegisterFormStatus.REJECTED);
        assertThat(found.get().getReason()).isEqualTo("Invalid documents");
        assertThat(found.get().getUpdatedBy()).isEqualTo(updatedBy);
    }

    @Test
    void whenUpdateApprovedRegisterFormForNonPendingForm_thenDoesNotUpdate() {
        var saved = registerFormRepository.save(
            new RegisterForm(
                new FullName("Approved User"),
                new IdentityNumber("999999999"),
                new Phone("0987654319"),
                new Email("register-approved-existing@example.com"),
                new DateOfBirth(LocalDate.of(1990, 1, 1)),
                "Ho Chi Minh City",
                new SchoolDomain("approved-existing-school.edu.vn"),
                "Approved Existing School",
                "Ho Chi Minh City",
                new PostalCode("700000"),
                "Principal",
                new StudentCount(100),
                null,
                RegisterFormStatus.APPROVED,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                UUID.randomUUID()
            )
        );
        entityManager.flush();

        var updated = registerFormRepository.updateApprovedRegisterForm(saved.getId(), UUID.randomUUID(), OffsetDateTime.now());

        assertThat(updated).isZero();
    }

    private static RegisterForm newRegisterForm(String identityNumber, String phone, String email, String schoolDomain) {
        var now = OffsetDateTime.now();
        return new RegisterForm(
            new FullName("Test User"),
            new IdentityNumber(identityNumber),
            new Phone(phone),
            new Email(email),
            new DateOfBirth(LocalDate.of(1990, 1, 1)),
            "Ho Chi Minh City",
            new SchoolDomain(schoolDomain),
            "Repository Test School",
            "Ho Chi Minh City",
            new PostalCode("700000"),
            "Principal",
            new StudentCount(100),
            null,
            RegisterFormStatus.PENDING,
            now,
            now,
            null
        );
    }
}
