package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.personalization.InterestQuizItemJpaEntity;

public interface SpringDataInterestQuizItemRepository
        extends JpaRepository<InterestQuizItemJpaEntity, UUID> {

    List<InterestQuizItemJpaEntity> findByIdAndActiveTrue(UUID id);

    // Takes the whole pool (soft cap via Top50), not Top7: the balanced selector needs
    // more candidates than the number of questions it will ask in order to pick a set that covers all dimensions evenly.
    List<InterestQuizItemJpaEntity> findTop50ByActiveTrueOrderById();

    List<InterestQuizItemJpaEntity> findTop50ByStudentIdAndActiveTrueOrderById(UUID studentId);

    /**
     * Whether this student has a set of questions that is STILL USABLE.
     *
     * <p>{@code AndActiveTrue} is required: since the quiz retake button exists, the old set gets {@code active}
     * switched off rather than being deleted (submitted answers still point to its ids). Asking by {@code studentId}
     * alone would, after a retake, still return {@code true} even though every question is already off -- the student
     * would receive an EMPTY list and onboarding would hang.
     *
     * <p>While nobody has retaken, every row is {@code active = true}, so behavior is identical to the old version.
     */
    boolean existsByStudentIdAndActiveTrue(UUID studentId);

    /**
     * Switches off the student's existing question set, does NOT delete it.
     *
     * <p>Deleting would leave submitted answers without a reference and make it impossible to trace why the interest profile
     * turned out as it did. Switching off makes {@code findActiveQuizItem} no longer see it, but the data stays intact.
     */
    @Modifying
    @Query("UPDATE InterestQuizItemJpaEntity i SET i.active = false "
        + "WHERE i.studentId = :studentId AND i.active = true")
    int deactivateByStudentId(@Param("studentId") UUID studentId);
}
