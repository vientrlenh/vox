package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
public class QuestionAssetJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
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

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "question_asset_order", nullable = false)
    private int order;

    protected QuestionAssetJpaEntity() {}

    public QuestionAssetJpaEntity(UUID id, UUID questionId, String title, Integer durationSeconds, String altText,
            String type, String url, String transcript, String description, int order) {
        this.id = id;
        this.questionId = questionId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.altText = altText;
        this.type = type;
        this.url = url;
        this.transcript = transcript;
        this.description = description;
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    
}
