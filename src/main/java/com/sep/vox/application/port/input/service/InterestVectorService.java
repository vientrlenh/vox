package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.TopicInterestEvent;
import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.DimensionInterestScoreRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.TopicInterestEventRepository;
import com.sep.vox.domain.repository.TopicInterestScoreRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

/**
 * Tính vector sở thích chủ đề (EMA theo topic) và vector sở thích theo dimension.
 * Gộp từ PracticePlanningRepositoryImpl vì đây là thuật toán chạm nhiều bảng/nhiều aggregate
 * (interest event, interest score, dimension score, learner profile, topic) -- không phải
 * việc của một adapter CRUD đơn lẻ.
 */
@Service
public class InterestVectorService {

    private final TopicInterestEventRepository eventRepository;
    private final TopicInterestScoreRepository scoreRepository;
    private final DimensionInterestScoreRepository dimensionScoreRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticeTopicRepository topicRepository;

    public InterestVectorService(
            TopicInterestEventRepository eventRepository,
            TopicInterestScoreRepository scoreRepository,
            DimensionInterestScoreRepository dimensionScoreRepository,
            LearnerProfileRepository learnerProfileRepository,
            PracticeTopicRepository topicRepository) {
        this.eventRepository = eventRepository;
        this.scoreRepository = scoreRepository;
        this.dimensionScoreRepository = dimensionScoreRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public void appendInterestEvent(
            UUID studentId,
            UUID topicId,
            UUID sessionId,
            String eventType,
            double signal) {
        eventRepository.save(studentId, topicId, sessionId, eventType, signal);
    }

    @Transactional
    public void recordSessionOutcome(
            UUID studentId,
            UUID topicId,
            UUID sessionId,
            String origin,
            String diagnosis,
            boolean completed,
            int spokenSeconds,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds) {
        if (!completed && !"BORED".equals(diagnosis)) {
            return;
        }
        // MỘT đường thẳng nối "bỏ dở" (0 giây nói) tới "trọn buổi", thay cho hai mức rời
        // nhau ngăn bởi vách đứng ở đúng 1 giây -- xem SessionDiagnosisPolicy.signal.
        //
        // Nhánh bỏ dở luôn có spokenSeconds == 0 (ABANDONED <=> gradedSeconds == 0) nên nó tự
        // rơi vào đầu dưới của cùng công thức; không cần rẽ nhánh riêng, và nhờ vậy hai đầu
        // không thể trôi lệch nhau khi sau này chỉnh bảng giá trị.
        var signal = SessionDiagnosisPolicy.signal(origin, spokenSeconds);
        appendInterestEvent(studentId, topicId, sessionId, "SESSION_OUTCOME", signal);
        var alreadyRecorded = new HashSet<UUID>();
        alreadyRecorded.add(topicId);
        appendSkippedOffers(studentId, sessionId, offeredTopicIds, 0.30, alreadyRecorded);
        appendSkippedOffers(studentId, sessionId, previousOfferedTopicIds, 0.20, alreadyRecorded);
        recomputeInterest(studentId);
    }

    @Transactional
    public void recomputeInterest(UUID studentId) {
        var events = eventRepository.findByStudent(studentId);
        var scores = new LinkedHashMap<UUID, Double>();
        var sessions = new HashMap<UUID, Set<UUID>>();
        // Bằng chứng KHÔNG gắn phiên -- xem chú thích ở nhánh else bên dưới.
        var standalone = new HashMap<UUID, Integer>();
        var last = new HashMap<UUID, Instant>();
        for (var event : events) {
            // ĐIỂM thì mọi sự kiện đều tính -- kể cả "được chào mà không chọn", đó chính là
            // thông tin âm ta muốn học.
            scores.compute(
                event.getTopicId(),
                (key, value) -> 0.3 * event.getSignal() + 0.7 * (value == null ? 0.5 : value)
            );
            // sessions_mentioned CŨNG tính mọi sự kiện -- nó là LƯỢNG BẰNG CHỨNG, không phải
            // số buổi đã luyện.
            //
            // Nó chỉ đi vào đúng một chỗ: gamma = n/(n+2) trong ViewPracticeTopicOffersUseCase,
            // trả lời "tôi biết bao nhiêu về chủ đề này để dám tin điểm riêng của nó thay vì
            // mượn điểm của cả chiều". Một lần chào rồi bị bỏ qua LÀ bằng chứng về chủ đề đó.
            //
            // Lọc nó ra là tự bắn vào chân: chủ đề chưa luyện bao giờ sẽ mãi n = 0 -> gamma = 0
            // -> interest = d_t hoàn toàn -> điểm riêng s_t (đang bị tín hiệu 0,30 kéo xuống)
            // KHÔNG được dùng một chút nào. Tức là bỏ qua bao nhiêu lần cũng không đổi thứ hạng.
            if (event.getSessionId() != null) {
                sessions.computeIfAbsent(event.getTopicId(), ignored -> new HashSet<>())
                    .add(event.getSessionId());
            } else {
                // Sự kiện KHÔNG gắn phiên nào -- hiện chỉ có TOPIC_DISMISSED (học sinh bấm loại
                // thẻ ở màn chọn chủ đề). Mỗi lần bấm là một bằng chứng riêng, phải đếm.
                //
                // Bỏ nhánh này thì tính năng loại thẻ CHẠY MÀ VÔ TÁC DỤNG: mentions đứng yên ở 0
                // -> gamma = 0/(0+2) = 0 -> interest = điểm_chiều hoàn toàn -> điểm riêng vừa bị
                // hạ không được dùng một chút nào. Bấm loại bao nhiêu lần thứ hạng cũng y nguyên.
                standalone.merge(event.getTopicId(), 1, (current, delta) -> current + delta);
            }
            // Chỉ LẦN CUỐI mới phải lọc theo loại.
            //
            // last_mentioned_at đi vào recency = e^(-dt/7 ngày), rồi vào hệ số phạt
            // (1 - 0,4·recency) với ý nghĩa "vừa luyện xong thì khoan mời lại". Chủ đề chỉ mới
            // HIỆN LÊN rồi bị lướt qua chưa hề được luyện, nên đóng dấu "vừa mới đây" cho nó là
            // phạt 40% một việc chưa xảy ra.
            //
            // Vô hại chừng nào bảng chỉ có SESSION_OUTCOME; từ lúc OFFERED_NOT_CHOSEN xuất hiện
            // thì thành lỗi thật. Cột này nullable và SQL xếp hạng đã có
            // CASE WHEN last_mentioned_at IS NULL THEN 0.0 -- chưa luyện thì recency = 0, không
            // phạt, đúng nghĩa.
            if (event.isSessionOutcome()) {
                last.put(event.getTopicId(), event.getOccurredAt());
            }
        }
        var newScores = new ArrayList<TopicInterestScoreEntry>();
        for (var entry : scores.entrySet()) {
            newScores.add(new TopicInterestScoreEntry(
                entry.getKey(),
                entry.getValue(),
                sessions.getOrDefault(entry.getKey(), Set.of()).size()
                    + standalone.getOrDefault(entry.getKey(), 0),
                last.get(entry.getKey())
            ));
        }
        scoreRepository.replaceForStudent(studentId, newScores);
        recomputeDimensionInterest(studentId, events);
    }

    private void appendSkippedOffers(
            UUID studentId,
            UUID sessionId,
            List<UUID> topicIds,
            double signal,
            Set<UUID> alreadyRecorded) {
        for (var offeredId : topicIds == null ? List.<UUID>of() : topicIds) {
            if (offeredId != null && alreadyRecorded.add(offeredId)) {
                appendInterestEvent(studentId, offeredId, sessionId, "OFFERED_NOT_CHOSEN", signal);
            }
        }
    }

    private void recomputeDimensionInterest(
            UUID studentId,
            List<TopicInterestEvent> events) {
        var profile = learnerProfileRepository.findCurrent(studentId).orElse(null);
        if (profile == null) {
            return;
        }
        var profileId = profile.getId();
        dimensionScoreRepository.primeBaselineFromScoreWhereMissing(profileId);
        // Chỉ hỏi chiều của những chủ đề THẬT SỰ có trong sự kiện của học sinh này, thay vì nạp
        // cả bảng như bản cũ (findAllTopicDimensions -> findAll(), entity đầy đủ, không lọc gì,
        // chạy sau MỖI buổi luyện). Kết quả y hệt: id nào không có trong bảng vẫn trả null và rơi
        // vào nhánh `continue` ngay dưới, đúng như trước.
        var dimensions = topicRepository.findDimensionsByIds(
            events.stream()
                .map(event -> event.getTopicId())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet())
        );
        var scores = new HashMap<>(dimensionScoreRepository.findByLearnerProfile(profileId));
        for (var event : events) {
            var dimension = dimensions.get(event.getTopicId());
            if (dimension == null) {
                continue;
            }
            scores.compute(
                dimension,
                (key, value) -> 0.1 * event.getSignal() + 0.9 * (value == null ? 0.5 : value)
            );
        }
        for (var entry : scores.entrySet()) {
            dimensionScoreRepository.upsertScore(profileId, entry.getKey(), entry.getValue());
        }
    }
}
