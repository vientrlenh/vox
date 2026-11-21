package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.infrastructure.persistence.adapter.AssessmentPaperRepositoryImpl;
import com.sep.vox.infrastructure.security.CustomUserDetails;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import(AssessmentPaperRepositoryImpl.class)
class AssessmentPaperAuditingTests {

    private static final String SYSTEM_ACTOR = "System";
    private static final int UUID_VERSION_7 = 7;

    @Autowired
    private AssessmentPaperRepository assessmentPaperRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNobodyIsLoggedIn_thenNewRowHasVersionSevenIdEqualTimestampsAndSystemActor() {
        AssessmentPaper stored = reload(createPaper("PAPER_AUDIT_SYSTEM"));

        assertThat(stored.getId().version()).isEqualTo(UUID_VERSION_7);
        assertThat(stored.getCreatedAt()).isNotNull().isEqualTo(stored.getUpdatedAt());
        assertThat(stored.getCreatedBy()).isEqualTo(SYSTEM_ACTOR);
        assertThat(stored.getUpdatedBy()).isEqualTo(SYSTEM_ACTOR);
    }

    @Test
    void whenAUserIsLoggedIn_thenBothActorsAreTheUserIdText() {
        UUID userId = UUID.randomUUID();
        logIn(userId);

        AssessmentPaper stored = reload(createPaper("PAPER_AUDIT_USER"));

        assertThat(stored.getCreatedBy()).isEqualTo(userId.toString());
        assertThat(stored.getUpdatedBy()).isEqualTo(userId.toString());
        assertThat(stored.isCreatedBy(userId)).isTrue();
    }

    @Test
    void whenUpdated_thenCreationValuesStayAndUpdateValuesMove() {
        UUID creatorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        logIn(creatorId);
        AssessmentPaper created = reload(createPaper("PAPER_AUDIT_UPDATE"));

        logIn(editorId);
        created.setVariant(2);
        assessmentPaperRepository.save(created);
        AssessmentPaper updated = reload(created);

        assertThat(updated.getVariant()).isEqualTo(2);
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo(creatorId.toString());
        assertThat(updated.getUpdatedAt()).isAfter(created.getUpdatedAt());
        assertThat(updated.getUpdatedBy()).isEqualTo(editorId.toString());
    }

    private AssessmentPaper createPaper(String code) {
        return assessmentPaperRepository.save(AssessmentPaper.builder()
            .assessmentId(UUID.randomUUID())
            .code(code)
            .variant(1)
            .status(AssessmentPaperStatus.DRAFT)
            .build());
    }

    private AssessmentPaper reload(AssessmentPaper paper) {
        entityManager.flush();
        entityManager.clear();
        return assessmentPaperRepository.findById(paper.getId()).orElseThrow();
    }

    private static void logIn(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.builder().id(userId).build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
