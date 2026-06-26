package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

public class ExamBlueprintSlot {
    private UUID id;
    private UUID sectionId;
    private UUID blueprintVersionId;
    private int order;
    private BigDecimal weight;
    private Integer prepTimeSecondsOverride; // đè default
    private Integer responseTimeSecondsOverride;

    private ExamBlueprintSlotType slotType; 
    private UUID fixedQuestionId; // set khi FIXED
    private QuestionSelectionSpec selectionSpec; // set khi SELECTION
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
