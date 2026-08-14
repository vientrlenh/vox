package com.sep.vox.application.port.output;

import java.time.Duration;

/**
 * Tham số sinh đề luyện mà application cần đọc. Giá trị thật bind từ application.yaml
 * ({@code app.personalization.generation.*}) ở PracticeGenerationProperties -- record đó implement
 * interface này nên application không phải biết tới @ConfigurationProperties.
 */
public interface PracticeGenerationConfigPort {

    Integer paperTargetQuestionCount();

    Integer onlineCandidateCount();

    Duration onlineBudget();
}
