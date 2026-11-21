package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_blueprint_slots")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentBlueprintSlotJpaEntity extends BaseEntity {

    @Column(name = "section_id", updatable = false)
    private UUID sectionId;

    @Column(name = "blueprint_version_id", updatable = false)
    private UUID blueprintVersionId;

    @Column(name = "slot_order", nullable = false)
    private int order;

    @Column(name = "weight", nullable = false, precision = 3, scale = 2)
    private BigDecimal weight;

    @Column(name = "prep_time_seconds_override")
    private Integer prepTimeSecondsOverride;

    @Column(name = "response_time_seconds_override")
    private Integer responseTimeSecondsOverride;

    // These three columns must be updatable: UpdateAssessmentBlueprintSlotUseCase allows changing the slot type and editing the
    // criteria while the version is still DRAFT. Previously updatable=false made those changes silently
    // not persist, and the question picker then filtered by the old criteria.
    @Column(name = "slot_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_blueprint_slots_type_valid",
            constraint = "slot_type IN ('FIXED', 'SELECTION')"
        )
    })
    private String slotType;

    @Column(name = "fixed_question_id")
    private UUID fixedQuestionId;

    @Column(name = "selection_spec", columnDefinition = "TEXT")
    private String selectionSpec;

}
