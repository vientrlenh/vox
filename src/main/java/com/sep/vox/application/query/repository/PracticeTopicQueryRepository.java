package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicNameCardInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;

public interface PracticeTopicQueryRepository {

    List<RankedTopicInfo> findRankedTopics(UUID studentId, String goal);

    List<TopicSearchRowInfo> searchTopics(UUID studentId, String pattern, String normalized);

    /** Nạp lại chủ đề còn active theo id -- hydrate kết quả tìm bằng vector từ Postgres. */
    List<TopicSearchRowInfo> findActiveByIds(UUID studentId, java.util.Collection<UUID> topicIds);

    Optional<TopicSearchRowInfo> findRandomActiveTopic(UUID studentId);

    List<TopicSearchRowInfo> findSavedTopics(UUID studentId);

    /**
     * Danh thiếp (id, tên, chiều) của chủ đề đang hoạt động -- cho phép chống trùng theo tên.
     *
     * <p>Thay {@code findAllActive()} cũ (gỡ 2026-08-11) vốn trả entity đầy đủ kèm cột
     * {@code description} kiểu TEXT, trong khi chỗ dùng chỉ đọc ba trường này.
     */
    List<TopicNameCardInfo> findActiveNameCards();

    /**
     * PUBLISHED topics in the question bank (question_bank/question_topic) of exactly the student's
     * current school + grade -- the topic source for ASSESSMENT_PREP practice. A bank not
     * attached to any grade applies to every grade in that school.
     */
    List<QuestionTopicInfo> findPublishedAssessmentTopics(UUID schoolId, UUID gradeId);
}
