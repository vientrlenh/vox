package com.sep.vox.domain.model.exam;

import java.util.UUID;

public class ExamPaperItem {
    private UUID id;
    private UUID sectionId;
    private UUID questionId;
    private int order;
    private int weight;
}
