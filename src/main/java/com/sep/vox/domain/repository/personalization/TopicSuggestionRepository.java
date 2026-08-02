package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.TopicSuggestion;

public interface TopicSuggestionRepository {

    Optional<TopicSuggestion> findById(UUID id);

    TopicSuggestion save(TopicSuggestion suggestion);

    List<TopicSuggestion> findPendingByStudentId(UUID studentId);

    /** Khoá ghi (PESSIMISTIC_WRITE) -- dùng khi phản hồi 1 gợi ý, tránh 2 request cùng xử lý 1 dòng. */
    Optional<TopicSuggestion> findByIdAndStudentIdAndStatusForUpdate(
        UUID id,
        UUID studentId,
        String status
    );

    int countByStudentIdAndStatus(UUID studentId, String status);

    List<TopicSuggestion> findByStudentIdAndStatus(UUID studentId, String status);

    int countWeeklyKeywordRequests(UUID studentId);

    List<UUID> findStudentsDueForSuggestionRefresh(int limit);

    List<StudentTranscript> findRecentTranscripts(UUID studentId);

    record StudentTranscript(UUID sessionId, String transcript) {
    }
}
