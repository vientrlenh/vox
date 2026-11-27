package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.application.port.input.service.InterestQuizItemSelector;
import com.sep.vox.application.port.input.service.InterestQuizScorer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.InterestQuizConfigPort;
import com.sep.vox.application.port.output.InterestQuizGenerationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;
import com.sep.vox.domain.repository.TopicInterestEventRepository;

/**
 * Gói 13 -- sinh quiz sở thích theo tình huống bằng AI (Verbalized Sampling, không cá nhân hoá
 * theo lịch sử). Xem task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md mục 3.
 */
@Service
@RequiredArgsConstructor
public class ViewInterestQuizItemsUseCase implements IUseCase<Void, List<InterestQuizItem>> {

    private final InterestQuizItemRepository quizItemRepository;
    private final TopicInterestEventRepository topicInterestEventRepository;
    private final InterestQuizGenerationPort quizGenerationPort;
    private final UserContextPort userContextPort;
    private final InterestQuizScorer interestQuizScorer;
    private final InterestQuizItemSelector itemSelector;
    private final InterestQuizConfigPort quizConfig;
    private final LearnerProfileRepository learnerProfileRepository;

    // Không @Transactional -- quizGenerationPort.generate() gọi LLM chậm (Python agents,
    // 10-20s). Bọc trong transaction từng gây HikariCP cạn pool dưới tải (xem
    // BuildPracticePaperUseCase/PracticeQuestionGenerationService, cùng đợt sửa). Mỗi lệnh gọi
    // repository bên dưới tự transact riêng qua Spring Data proxy.
    @Override
    public List<InterestQuizItem> execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();

        if (quizItemRepository.hasQuizItemsForStudent(studentId)) {
            return toResponse(selectForStudent(studentId));
        }

        List<InterestQuizSeedItem> sharedPool = selectBalanced(quizItemRepository.findAllActiveQuizItems());

        // Đã có tín hiệu interest thật (từ tương tác/luyện tập trước đó) -- không cần sinh quiz
        // riêng nữa, dùng bộ tĩnh gốc. Cá nhân hoá không nên nằm ở bước sinh quiz (mục 0 của
        // task 13), nó thuộc bước chọn chủ đề luyện tập (đã đúng, không đụng vào).
        if (!topicInterestEventRepository.findByStudent(studentId).isEmpty()) {
            return toResponse(sharedPool);
        }

        return generateForStudent(studentId, sharedPool);
    }

    /**
     * Làm lại quiz theo yêu cầu của học sinh.
     *
     * <p>Luật: bộ cũ CHƯA nộp xong thì trả lại ĐÚNG bộ đó để làm tiếp -- sinh bộ mới giữa chừng
     * là vứt công em ấy đã làm, và những đáp án đã chọn sẽ trỏ tới các item vừa bị tắt. Chỉ khi
     * {@code quizCompletedAt} đã có mới tắt bộ cũ và sinh bộ mới.
     *
     * <p>Không {@code @Transactional}: giống {@link #execute} -- có lời gọi LLM 10-20 giây bên
     * trong, giữ transaction suốt quãng đó từng làm cạn pool kết nối.
     */
    public List<InterestQuizItem> regenerate() {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();

        Boolean completed = learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
        if (!completed) {
            // Chưa xong -> làm tiếp bộ cũ, không đụng gì tới dữ liệu.
            return execute(null);
        }

        quizItemRepository.deactivateGeneratedForStudent(studentId);
        List<InterestQuizSeedItem> sharedPool = selectBalanced(quizItemRepository.findAllActiveQuizItems());
        return generateForStudent(studentId, sharedPool);
    }

    /** Generates the student's own set and saves it. When generation returns nothing, falls back to the shared static set instead of returning empty. */
    private List<InterestQuizItem> generateForStudent(
            UUID studentId, List<InterestQuizSeedItem> sharedPool) {
        List<InterestQuizSeedItem> generated = generateQuizItems(sharedPool);
        if (generated.isEmpty()) {
            // Python/LLM not available -- fall back to the original static set instead of returning empty to the student.
            return toResponse(sharedPool);
        }
        quizItemRepository.saveGeneratedForStudent(studentId, generated);
        return toResponse(selectForStudent(studentId));
    }

    // Generates SYNCHRONOUSLY, then writes, then returns -- the student gets their own set on the very first call.
    //
    // The write is not moved to the background: the slow part is the LLM, not the INSERT (7 rows, a few
    // milliseconds). Splitting it saves nothing noticeable and breaks elsewhere -- the id is generated by the DB
    // (uuidv7, insertable=false), so returning before the write leaves every item with a null id and the student
    // submitting answers by itemId breaks.
    //
    // What really had to be fixed is speed: the old version made ONE LLM call for all 7 items and made the model
    // balance the dimensions itself -- 46.8 seconds measured, over both the Tomcat and the client ceiling.
    // Python now splits it into 7 parallel calls, each for one item with its dimension triple and context already
    // assigned, at a low reasoning level.
    private List<InterestQuizSeedItem> generateQuizItems(List<InterestQuizSeedItem> sharedPool) {
        List<String> existingStatements = sharedPool.stream()
            .flatMap(item -> item.getStatements().stream())
            .toList();
        return quizGenerationPort.generate(
            quizConfig.itemCount(),
            existingStatements,
            interestQuizScorer.quizDimensionCodes()
        );
    }

    /**
     * Ứng viên = item sinh riêng cho học sinh này + CẢ kho dùng chung.
     *
     * Không chỉ lấy item riêng, vì hai lý do đều làm học sinh kẹt cứng:
     * 1. Item riêng của học sinh cũ chỉ gắn các chiều CÓ Ở THỜI ĐIỂM sinh. Admin thêm chiều
     *    mới về sau thì kho dùng chung được bổ sung, nhưng item riêng thì không -- chiều mới
     *    sẽ không bao giờ được hỏi những học sinh đã onboard.
     * 2. Nếu một chiều bị tắt, mọi item riêng chứa chiều đó bị loại; học sinh có thể còn lại
     *    dưới 5 item, mà submitQuiz bắt buộc 5-7 câu -> không nộp được, không có đường ra.
     *
     * Item riêng xếp TRƯỚC nên khi điểm phủ bằng nhau chúng vẫn được ưu tiên (bộ chọn chỉ
     * thay ứng viên khi điểm CAO HƠN hẳn, nên hoà thì giữ cái đứng trước).
     */
    private List<InterestQuizSeedItem> selectForStudent(UUID studentId) {
        Map<UUID, InterestQuizSeedItem> pool = new LinkedHashMap<UUID, InterestQuizSeedItem>();
        for (InterestQuizSeedItem item : quizItemRepository.findAllActiveQuizItemsForStudent(studentId)) {
            pool.put(item.getId(), item);
        }
        for (InterestQuizSeedItem item : quizItemRepository.findAllActiveQuizItems()) {
            pool.putIfAbsent(item.getId(), item);
        }
        return selectBalanced(List.copyOf(pool.values()));
    }

    /** Loại item chứa chiều không còn hỏi được + phủ đều các chiều trong đúng ngân sách câu
     * hỏi (app.personalization.quiz.item-count) -- xem InterestQuizItemSelector để biết vì sao
     * cần cả hai. */
    private List<InterestQuizSeedItem> selectBalanced(List<InterestQuizSeedItem> pool) {
        return itemSelector.select(pool, quizConfig.itemCount());
    }

    private List<InterestQuizItem> toResponse(List<InterestQuizSeedItem> items) {
        return LearnerProfileResponseMapper.toResponse(items);
    }
}
