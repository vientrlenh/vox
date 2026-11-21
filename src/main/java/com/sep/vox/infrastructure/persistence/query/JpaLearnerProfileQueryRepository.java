package com.sep.vox.infrastructure.persistence.query;

import com.sep.vox.infrastructure.persistence.entity.personalization.LearnerProfileJpaEntity;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.LearnerProfileInfo;
import com.sep.vox.application.query.repository.LearnerProfileQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLearnerProfileRepository;

@Repository
public class JpaLearnerProfileQueryRepository
        implements LearnerProfileQueryRepository {

    private final SpringDataLearnerProfileRepository repository;

    /** Rỗng = tự chọn khung đang hoạt động. Cùng thuộc tính mà
     * {@code PracticeTopicOfferEnrichmentService} dùng -- hai nơi phải ghim cùng một khung. */
    private final String practiceFrameworkVersionCode;

    public JpaLearnerProfileQueryRepository(
            SpringDataLearnerProfileRepository repository,
            @Value("${app.practice.framework-version-code:}") String practiceFrameworkVersionCode) {
        this.repository = repository;
        this.practiceFrameworkVersionCode =
            practiceFrameworkVersionCode == null || practiceFrameworkVersionCode.isBlank()
                ? null
                : practiceFrameworkVersionCode.trim();
    }

    /**
     * The target band now comes from the ACTIVE FRAMEWORK, no longer looked up in {@code assessment_policies}
     * by the student's class -- and when missing it returns null instead of throwing.
     *
     * <p>The old path was wrong on two stacked levels.
     *
     * <p><b>Wrong scope.</b> This was the last piece of practice still tied to the school scope, even though
     * the rest had moved to the system-wide framework since V13 (see
     * {@code PracticeTopicOfferEnrichmentService.frameworkBandCount/frameworkBandLadder}). Keeping it
     * meant the number of bands came from the framework while the suggested band came from the policy -- two sources that may point to
     * two different scales without anyone noticing.
     *
     * <p><b>Throwing in the wrong place.</b> The four use cases that call this function all follow "WRITE first, then READ back to build
     * the response", and all four are {@code @Transactional}. Throwing at the read step rolls back the write step too.
     * Measured in practice: a student of THPT Trần Thông submitted the interest quiz, {@code interest_quiz_items} still had
     * 7 rows but {@code learner_profiles} had 0 rows. The cause was only that the school's policy pointed
     * at a school year with no classes -- one leftover admin misconfiguration swallowed the student's work,
     * and dragged topic generation, schedule viewing and result viewing down with it.
     *
     * <p>What is missing is only a LABEL: the suggested band's code and name on the difficulty selection screen. The student can still choose the
     * band themselves, so null here leaves the UI without a suggestion but breaks nothing.
     */
    @Override
    public LearnerProfileInfo findCurrent(UUID studentId) {
        LearnerProfileJpaEntity current = repository
            .findByStudentId(studentId)
            .orElse(null);
        FrameworkResultBandJpaEntity target = repository.findDefaultTargetBand(practiceFrameworkVersionCode)
            .stream()
            .findFirst()
            .orElse(null);
        return new LearnerProfileInfo(
            current == null || current.getGoalType() == null
                ? "ABILITY_IMPROVEMENT"
                : current.getGoalType(),
            target == null ? null : target.getCode(),
            target == null ? null : label(target),
            current == null || current.isAutoUpdateInterest(),
            current == null ? null : current.getQuizCompletedAt()
        );
    }

    /** Khung chưa điền nhãn thì lấy mã làm nhãn -- thà hiện "B2" còn hơn hiện ô trống. */
    private static String label(FrameworkResultBandJpaEntity band) {
        return band.getLabel() == null || band.getLabel().isBlank()
            ? band.getCode()
            : band.getLabel();
    }
}
