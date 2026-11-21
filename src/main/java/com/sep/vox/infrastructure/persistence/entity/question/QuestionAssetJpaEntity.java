package com.sep.vox.infrastructure.persistence.entity.question;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "question_assets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_question_assets_question_id", columnNames = {"question_id"})
    },
    indexes = {
        @Index(columnList = "question_id, question_asset_order", name = "idx_question_assets_question_question_asset_order", unique = true),
        @Index(columnList = "question_id", name = "idx_question_assets_question")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionAssetJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "type", length = 100, nullable = false, check = {
        @CheckConstraint(
            name = "chk_question_assets_type_valid", 
            constraint = "type IN ('AUDIO', 'IMAGE', 'VIDEO', 'TEXT_PASSAGE')"
        )
    })
    private String type;

    @Column(name = "url", length = 4096)
    private String url;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "question_asset_order", nullable = false)
    private int order;
}
