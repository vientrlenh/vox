package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;
import com.sep.vox.infrastructure.persistence.adapter.SchoolRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SchoolRepository schoolRepository;

    @Test
    void whenSave_thenReturnsPersistedSchool() {
        var school = newSchool("VOX_HCM", "Vox Ho Chi Minh", "vox-hcm.edu.vn", "school-hcm@example.com", "0987654301");

        var saved = schoolRepository.save(school);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("VOX_HCM");
        assertThat(saved.getDomain().value()).isEqualTo("vox-hcm.edu.vn");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void whenFindById_thenReturnsSchool() {
        var saved = schoolRepository.save(newSchool("VOX_DN", "Vox Da Nang", "vox-dn.edu.vn", "school-dn@example.com", "0987654302"));

        var found = schoolRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("VOX_DN");
    }

    @Test
    void whenFindByCode_thenReturnsSchool() {
        schoolRepository.save(newSchool("VOX_HN", "Vox Ha Noi", "vox-hn.edu.vn", "school-hn@example.com", "0987654303"));

        var found = schoolRepository.findByCode("VOX_HN");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Vox Ha Noi");
    }

    @Test
    void whenFindByDomain_thenReturnsSchool() {
        schoolRepository.save(newSchool("VOX_CT", "Vox Can Tho", "vox-ct.edu.vn", "school-ct@example.com", "0987654304"));

        var found = schoolRepository.findByDomain("vox-ct.edu.vn");

        assertThat(found).isPresent();
        assertThat(found.get().getCode().value()).isEqualTo("VOX_CT");
    }

    @Test
    void whenFindAll_thenReturnsPagedSchools() {
        schoolRepository.save(newSchool("VOX_PG_1", "Vox Page 1", "vox-pg-1.edu.vn", "school-pg1@example.com", "0987654305"));
        schoolRepository.save(newSchool("VOX_PG_2", "Vox Page 2", "vox-pg-2.edu.vn", "school-pg2@example.com", "0987654306"));
        schoolRepository.save(newSchool("VOX_PG_3", "Vox Page 3", "vox-pg-3.edu.vn", "school-pg3@example.com", "0987654307"));

        var pageRequest = new PageRequest(1, 2);
        var found = schoolRepository.findAll(pageRequest.page(), pageRequest.size());

        assertThat(found.content()).hasSize(2);
        assertThat(found.page()).isEqualTo(1);
        assertThat(found.size()).isEqualTo(2);
        assertThat(found.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(found.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void whenExistsById_thenReturnsTrueOnlyForExistingSchool() {
        var saved = schoolRepository.save(newSchool("VOX_EXIST", "Vox Exists", "vox-exist.edu.vn", "school-exist@example.com", "0987654308"));

        assertThat(schoolRepository.existsById(saved.getId())).isTrue();
        assertThat(schoolRepository.existsById(UUID.randomUUID())).isFalse();
    }

    private static School newSchool(String code, String name, String domain, String email, String phone) {
        var now = OffsetDateTime.now();
        return new School(
            new SchoolCode(code),
            name,
            "Repository test school",
            new Phone(phone),
            new Email(email),
            new SchoolDomain(domain),
            "Ho Chi Minh City",
            new StudentCount(100),
            true,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
