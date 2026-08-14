package com.sep.vox.application.port.output;

/**
 * Tham số quiz sở thích mà application cần đọc. Giá trị thật bind từ application.yaml
 * ({@code app.personalization.quiz.*}) ở InterestQuizProperties.
 */
public interface InterestQuizConfigPort {

    Integer itemCount();
}
