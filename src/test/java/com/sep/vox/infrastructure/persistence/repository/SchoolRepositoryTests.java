package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
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

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCode().value()).isEqualTo("VOX_CT");
    }

    /**
     * Nhiều cơ sở của cùng một trường dùng chung tên miền -- lưu được và tra ra ĐỦ, không nổ
     * NonUniqueResultException. Đây là lý do findByDomain trả List thay vì Optional, và là lý do
     * V34 bỏ ràng buộc unique (code, domain).
     */
    @Test
    void whenFindByDomain_withMultipleCampuses_thenReturnsAll() {
        schoolRepository.save(newSchool("VOX_MC_1", "Vox Co So 1", "vox-mc.edu.vn", "school-mc1@example.com", "0987654321"));
        schoolRepository.save(newSchool("VOX_MC_2", "Vox Co So 2", "vox-mc.edu.vn", "school-mc2@example.com", "0987654322"));

        var found = schoolRepository.findByDomain("vox-mc.edu.vn");

        assertThat(found).hasSize(2);
        assertThat(found).extracting(school -> school.getCode().value())
            .containsExactlyInAnyOrder("VOX_MC_1", "VOX_MC_2");
    }

    @Test
    void whenFindAll_thenReturnsPagedSchools() {
        schoolRepository.save(newSchool("VOX_PG_1", "Vox Page 1", "vox-pg-1.edu.vn", "school-pg1@example.com", "0987654305"));
        schoolRepository.save(newSchool("VOX_PG_2", "Vox Page 2", "vox-pg-2.edu.vn", "school-pg2@example.com", "0987654306"));
        schoolRepository.save(newSchool("VOX_PG_3", "Vox Page 3", "vox-pg-3.edu.vn", "school-pg3@example.com", "0987654307"));


        var found = schoolRepository.findAll(1, 2, null, null);

        assertThat(found.content()).hasSize(2);
        assertThat(found.page()).isEqualTo(1);
        assertThat(found.size()).isEqualTo(2);
        assertThat(found.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(found.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void whenFindAll_withSearch_thenMatchesCodeOrName() {
        schoolRepository.save(newSchool("VOX_SEARCH_A", "Truong Alpha", "vox-search-a.edu.vn", "school-search-a@example.com", "0987654320"));
        schoolRepository.save(newSchool("VOX_SEARCH_B", "Truong Beta", "vox-search-b.edu.vn", "school-search-b@example.com", "0987654321"));

        var byCode = schoolRepository.findAll(1, 10, "search_a", null);
        var byName = schoolRepository.findAll(1, 10, "beta", null);

        assertThat(byCode.content()).extracting(school -> school.getCode().value()).containsExactly("VOX_SEARCH_A");
        assertThat(byName.content()).extracting(school -> school.getCode().value()).containsExactly("VOX_SEARCH_B");
    }

    @Test
    void whenFindAll_withIsActiveFilter_thenOnlyMatchingStatusReturned() {
        var active = newSchool("VOX_ACTIVE_1", "Truong Dang Hoat Dong", "vox-active-1.edu.vn", "school-active1@example.com", "0987654322");
        var inactive = newSchool("VOX_INACTIVE_1", "Truong Da Khoa", "vox-inactive-1.edu.vn", "school-inactive1@example.com", "0987654323");
        inactive.setActive(false);
        schoolRepository.save(active);
        schoolRepository.save(inactive);

        var activeOnly = schoolRepository.findAll(1, 50, null, true);
        var inactiveOnly = schoolRepository.findAll(1, 50, null, false);

        assertThat(activeOnly.content()).extracting(school -> school.getCode().value()).contains("VOX_ACTIVE_1");
        assertThat(activeOnly.content()).extracting(school -> school.getCode().value()).doesNotContain("VOX_INACTIVE_1");
        assertThat(inactiveOnly.content()).extracting(school -> school.getCode().value()).contains("VOX_INACTIVE_1");
        assertThat(inactiveOnly.content()).extracting(school -> school.getCode().value()).doesNotContain("VOX_ACTIVE_1");
    }

    @Test
    void whenExistsById_thenReturnsTrueOnlyForExistingSchool() {
        var saved = schoolRepository.save(newSchool("VOX_EXIST", "Vox Exists", "vox-exist.edu.vn", "school-exist@example.com", "0987654308"));

        assertThat(schoolRepository.existsById(saved.getId())).isTrue();
        assertThat(schoolRepository.existsById(UUID.randomUUID())).isFalse();
    }

    private static School newSchool(String code, String name, String domain, String email, String phone) {
        var now = Instant.now();
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
