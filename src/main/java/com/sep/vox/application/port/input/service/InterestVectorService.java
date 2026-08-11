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
import com.sep.vox.domain.repository.personalization.DimensionInterestScoreRepository;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.TopicInterestEventRepository;
import com.sep.vox.domain.repository.personalization.TopicInterestScoreRepository;

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
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds) {
        if (!completed && !"BORED".equals(diagnosis)) {
            return;
        }
        // EPSILON xếp CÙNG NHÓM với EXPLORATION, không rơi vào default nữa (sửa 2026-08-06).
        //
        // Trước đây EPSILON không có case nên nhận 0.95 -- y hệt học sinh tự chọn. Đúng cái
        // "dương giả" mà chú thích ở BuildPracticePaperUseCase.resolveOrigin nói là đã tránh
        // được; thực ra nhận diện xong rồi vẫn tính 0.95, nên vế đó của chú thích sai.
        //
        // Vì sao cùng nhóm: cả hai đều là HỆ THỐNG đưa chủ đề tới, không phải bằng chứng học
        // sinh vốn thích nó. Và với ε-greedy thì đây là điều kiện để phép đo có nghĩa -- cả
        // điểm của slot thăm dò bằng điểm của slot khai thác thì chính thứ mà thăm dò sinh ra
        // để đo lại bị nhiễu bởi ưu thế của lựa chọn có sẵn.
        //
        // Chấp nhận đánh đổi: chủ đề chỉ từng vào phiên qua đường thăm dò thì lần đầu tối đa
        // chỉ được 0.60, thấp hơn chủ đề tự chọn. Muốn lên cao hơn phải được học sinh CHỦ ĐỘNG
        // chọn ở lần sau -- đúng thứ ta muốn coi là bằng chứng thật.
        var signal = completed
            ? switch (origin) {
                case "KEYWORD" -> 1.00;
                case "EXPLORATION", "RANDOM", "EPSILON" -> 0.60;
                default -> 0.95;
            }
            : switch (origin) {
                case "KEYWORD" -> 0.20;
                case "EXPLORATION", "RANDOM", "EPSILON" -> 0.10;
                default -> 0.15;
            };
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
        var dimensions = topicRepository.findAllTopicDimensions();
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
