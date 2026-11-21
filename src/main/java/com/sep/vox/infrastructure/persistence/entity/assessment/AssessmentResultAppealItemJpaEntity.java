package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_result_appeal_items", indexes = {
    @Index(columnList = "appeal_id, paper_item_id", name = "uq_appeal_item", unique = true),
    @Index(columnList = "appeal_id", name = "idx_assessment_result_appeal_items_appeal")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentResultAppealItemJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "appeal_id", nullable = false, updatable = false)
    private UUID appealId;

    /** The part under appeal. */
    @Column(name = "paper_item_id", nullable = false, updatable = false)
    private UUID paperItemId;

    /** The response that belongs to that part: the grader listens to it again and regrades it. */
    @Column(name = "response_id", nullable = false, updatable = false)
    private UUID responseId;

    /** Score published for this part: NULL until the appeal is published. */
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;
}
