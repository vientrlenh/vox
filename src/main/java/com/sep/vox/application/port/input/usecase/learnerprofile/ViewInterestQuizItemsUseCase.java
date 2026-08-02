package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.personalization.InterestQuizItemRepository;
import com.sep.vox.domain.repository.personalization.TopicInterestEventRepository;
import com.sep.vox.infrastructure.service.InterestQuizGenerationClient;

/**
 * Gói 13 -- sinh quiz sở thích theo tình huống bằng AI (Verbalized Sampling, không cá nhân hoá
 * theo lịch sử). Xem task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md mục 3.
 */
@Service
public class ViewInterestQuizItemsUseCase implements IUseCase<Void, List<InterestQuizItem>> {

    private static final int QUIZ_ITEM_COUNT = 7;

    private final InterestQuizItemRepository quizItemRepository;
    private final TopicInterestEventRepository topicInterestEventRepository;
    private final InterestQuizGenerationClient generationClient;
    private final UserContextPort userContextPort;

    public ViewInterestQuizItemsUseCase(
            InterestQuizItemRepository quizItemRepository,
            TopicInterestEventRepository topicInterestEventRepository,
            InterestQuizGenerationClient generationClient,
            UserContextPort userContextPort) {
        this.quizItemRepository = quizItemRepository;
        this.topicInterestEventRepository = topicInterestEventRepository;
        this.generationClient = generationClient;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<InterestQuizItem> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();

        if (quizItemRepository.hasQuizItemsForStudent(studentId)) {
            return toResponse(
                quizItemRepository.findActiveQuizItemsForStudent(studentId, QUIZ_ITEM_COUNT)
            );
        }

        var sharedPool = quizItemRepository.findActiveQuizItems(QUIZ_ITEM_COUNT);

        // Đã có tín hiệu interest thật (từ tương tác/luyện tập trước đó) -- không cần sinh quiz
        // riêng nữa, dùng bộ tĩnh gốc. Cá nhân hoá không nên nằm ở bước sinh quiz (mục 0 của
        // task 13), nó thuộc bước chọn chủ đề luyện tập (đã đúng, không đụng vào).
        if (!topicInterestEventRepository.findByStudent(studentId).isEmpty()) {
            return toResponse(sharedPool);
        }

        var existingStatements = sharedPool.stream()
            .flatMap(item -> item.statements().stream())
            .toList();
        var generated = generationClient.generate(QUIZ_ITEM_COUNT, existingStatements);
        if (generated.isEmpty()) {
            // Python/LLM không sẵn sàng -- fallback bộ tĩnh gốc thay vì trả rỗng cho học sinh.
            return toResponse(sharedPool);
        }
        quizItemRepository.saveGeneratedForStudent(studentId, generated);
        return toResponse(
            quizItemRepository.findActiveQuizItemsForStudent(studentId, QUIZ_ITEM_COUNT)
        );
    }

    private List<InterestQuizItem> toResponse(List<InterestQuizSeedItem> items) {
        return LearnerProfileResponseMapper.toResponse(items);
    }
}
