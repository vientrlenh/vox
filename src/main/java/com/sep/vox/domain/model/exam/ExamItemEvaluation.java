package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.valueobject.EvaluationSignals;

public class ExamItemEvaluation {
    private UUID id;
    private UUID responseId;
    private UUID paperItemId;
    private ExamEvaluationEngineType engineType;
    private String gradedByModel; // tên model chấm
    private Integer sampleCount; // số mẫu nếu loại chấm là ensemble
    private UUID reviewerId; // người chấm (human)
    private BigDecimal rawItemScore; // điểm trước khi áp dụng rule
    private BigDecimal itemScore; // điểm sau khi áp dụng rule
    private BigDecimal overallConfidence; // độ tự tin của điểm AI
    private boolean requiresHumanReview; // tự đẩy cho người chấm nếu độ tự tin thấp
    private String reviewReasonCode;
    private boolean markedInvalid;
    private boolean requiresRetake;
    private EvaluationSignals signals;
    private ExamItemEvaluationStatus status; 

    
}
