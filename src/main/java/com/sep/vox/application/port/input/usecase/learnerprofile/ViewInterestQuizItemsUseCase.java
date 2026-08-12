package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.infrastructure.properties.InterestQuizProperties;
import com.sep.vox.application.port.input.service.InterestQuizItemSelector;
import com.sep.vox.application.port.input.service.InterestQuizScorer;
import com.sep.vox.infrastructure.service.InterestQuizGenerationClient;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;
import com.sep.vox.domain.repository.TopicInterestEventRepository;

/**
 * Gói 13 -- sinh quiz sở thích theo tình huống bằng AI (Verbalized Sampling, không cá nhân hoá
 * theo lịch sử). Xem task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md mục 3.
 */
@Service
public class ViewInterestQuizItemsUseCase implements IUseCase<Void, List<InterestQuizItem>> {

    private final InterestQuizProperties quizProperties;
    private final InterestQuizItemRepository quizItemRepository;
    private final TopicInterestEventRepository topicInterestEventRepository;
    private final InterestQuizGenerationClient generationClient;
    private final UserContextPort userContextPort;
    private final InterestQuizScorer interestQuizScorer;
    private final InterestQuizItemSelector itemSelector;

    public ViewInterestQuizItemsUseCase(
            InterestQuizItemRepository quizItemRepository,
            TopicInterestEventRepository topicInterestEventRepository,
            InterestQuizGenerationClient generationClient,
            UserContextPort userContextPort,
            InterestQuizScorer interestQuizScorer,
            InterestQuizItemSelector itemSelector,
            InterestQuizProperties quizProperties) {
        this.quizProperties = quizProperties;
        this.itemSelector = itemSelector;
        this.quizItemRepository = quizItemRepository;
        this.topicInterestEventRepository = topicInterestEventRepository;
        this.generationClient = generationClient;
        this.userContextPort = userContextPort;
        this.interestQuizScorer = interestQuizScorer;
    }

    // Không @Transactional -- generationClient.generate() gọi LLM chậm (Python agents,
    // 10-20s). Bọc trong transaction từng gây HikariCP cạn pool dưới tải (xem
    // BuildPracticePaperUseCase/PracticeQuestionGenerationService, cùng đợt sửa). Mỗi lệnh gọi
    // repository bên dưới tự transact riêng qua Spring Data proxy.
    @Override
    public List<InterestQuizItem> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();

        if (quizItemRepository.hasQuizItemsForStudent(studentId)) {
            return toResponse(selectForStudent(studentId));
        }

        var sharedPool = selectBalanced(quizItemRepository.findAllActiveQuizItems());

        // Đã có tín hiệu interest thật (từ tương tác/luyện tập trước đó) -- không cần sinh quiz
        // riêng nữa, dùng bộ tĩnh gốc. Cá nhân hoá không nên nằm ở bước sinh quiz (mục 0 của
        // task 13), nó thuộc bước chọn chủ đề luyện tập (đã đúng, không đụng vào).
        if (!topicInterestEventRepository.findByStudent(studentId).isEmpty()) {
            return toResponse(sharedPool);
        }

        var existingStatements = sharedPool.stream()
            .flatMap(item -> item.getStatements().stream())
            .toList();
        // Sinh ĐỒNG BỘ rồi ghi rồi trả -- học sinh nhận đúng bộ riêng ngay lượt đầu.
        //
        // Không tách phần ghi ra chạy nền: chỗ chậm là LLM chứ không phải INSERT (7 dòng,
        // vài mili giây). Tách ra không tiết kiệm được gì đáng kể, mà lại vỡ chỗ khác --
        // id do DB sinh (uuidv7, insertable=false) nên trả trước khi ghi thì mọi item có
        // id null, học sinh nộp đáp án theo itemId là hỏng.
        //
        // Phần thật sự phải sửa là tốc độ: bản cũ gọi MỘT lượt LLM cho cả 7 item và bắt
        // model tự cân bằng chiều -- 46,8 giây đo được, vượt cả trần Tomcat lẫn trần client.
        // Nay Python chia thành 7 lượt song song, mỗi lượt một item với bộ ba chiều và bối
        // cảnh đã phân công sẵn, ở mức suy luận thấp.
        var generated = generationClient.generate(
            quizProperties.itemCount(),
            existingStatements,
            interestQuizScorer.quizDimensionCodes()
        );
        if (generated.isEmpty()) {
            // Python/LLM không sẵn sàng -- fallback bộ tĩnh gốc thay vì trả rỗng cho học sinh.
            return toResponse(sharedPool);
        }
        quizItemRepository.saveGeneratedForStudent(studentId, generated);
        return toResponse(selectForStudent(studentId));
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
        var pool = new LinkedHashMap<UUID, InterestQuizSeedItem>();
        for (var item : quizItemRepository.findAllActiveQuizItemsForStudent(studentId)) {
            pool.put(item.getId(), item);
        }
        for (var item : quizItemRepository.findAllActiveQuizItems()) {
            pool.putIfAbsent(item.getId(), item);
        }
        return selectBalanced(List.copyOf(pool.values()));
    }

    /** Loại item chứa chiều không còn hỏi được + phủ đều các chiều trong đúng ngân sách câu
     * hỏi (app.personalization.quiz.item-count) -- xem InterestQuizItemSelector để biết vì sao
     * cần cả hai. */
    private List<InterestQuizSeedItem> selectBalanced(List<InterestQuizSeedItem> pool) {
        return itemSelector.select(pool, quizProperties.itemCount());
    }

    private List<InterestQuizItem> toResponse(List<InterestQuizSeedItem> items) {
        return LearnerProfileResponseMapper.toResponse(items);
    }
}
