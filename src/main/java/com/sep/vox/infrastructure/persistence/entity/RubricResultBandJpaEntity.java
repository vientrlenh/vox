package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "rubric_result_bands", indexes = {
    @Index(columnList = "rubric_version_id", name = "idx_rubric_result_bands_version_id")
}, check = {
    @CheckConstraint(
        name = "chk_rubric_result_bands_score_min_max_valid",
        constraint = "score_min <= score_max"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class RubricResultBandJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    // columnDefinition = "numeric" (no precision/scale declared): band boundaries need arbitrary
    // precision (e.g. 3.999 to avoid overlapping a next band that starts at 4). A fixed scale, however
    // wide, only moves the ceiling up; unbounded NUMERIC avoids repeating the rounding-on-save bug
    // (see RubricResultBandValidator -- touching a boundary also counts as an overlap).
    @Column(name = "score_min", nullable = false, columnDefinition = "numeric")
    private BigDecimal scoreMin;

    @Column(name = "score_max", nullable = false, columnDefinition = "numeric")
    private BigDecimal scoreMax;

    @Column(name = "result_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_rubric_result_bands_order_valid",
            constraint = "result_order > 0"
        )
    })
    private Integer order;

}
