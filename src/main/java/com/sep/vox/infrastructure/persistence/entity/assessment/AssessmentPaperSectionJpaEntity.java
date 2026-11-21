package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_paper_sections")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentPaperSectionJpaEntity extends BaseEntity {

    @Column(name = "paper_id", nullable = false, updatable = false)
    private UUID paperId;

    @Column(name = "section_order", nullable = false)
    private int order;

    @Column(name = "title", nullable = false, length = 1024)
    private String title;

    @Column(name = "instruction", length = 2048)
    private String instruction;

    @Column(name = "section_time_limit_seconds")
    private Integer sectionTimeLimitSeconds;

    @Column(name = "weight", precision = 8, scale = 4)
    private BigDecimal weight;

}
