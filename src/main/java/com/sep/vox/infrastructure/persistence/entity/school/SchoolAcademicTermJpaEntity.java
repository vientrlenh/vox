package com.sep.vox.infrastructure.persistence.entity.school;

import java.time.LocalDate;
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
@Table(name = "school_academic_terms", indexes = {
    @Index(columnList = "academic_year_id, term_order", name = "idx_school_academic_terms_year_order", unique = true)
}, check = {
    @CheckConstraint(
        name = "chk_school_academic_terms_range_valid",
        constraint = "start_date <= end_date"
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class SchoolAcademicTermJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private UUID academicYearId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "term_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_school_academic_terms_order_positive",
            constraint = "term_order > 0"
        )
    })
    private Integer order;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

}
