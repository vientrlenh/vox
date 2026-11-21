package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_paper_items")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentPaperItemJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "blueprint_slot_id", updatable = false)
    private UUID blueprintSlotId;

    @Column(name = "section_id", updatable = false)
    private UUID sectionId;

    @Column(name = "paper_id", updatable = false)
    private UUID paperId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "item_order", nullable = false)
    private int order;

    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;
}
