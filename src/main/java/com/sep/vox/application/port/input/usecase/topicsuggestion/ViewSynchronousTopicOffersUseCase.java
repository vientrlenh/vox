package com.sep.vox.application.port.input.usecase.topicsuggestion;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewSynchronousTopicOffersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
public class ViewSynchronousTopicOffersUseCase implements IUseCase<ViewSynchronousTopicOffersQuery, List<PracticeTopicOffer>> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    public ViewSynchronousTopicOffersUseCase(
            TopicSuggestionService topicSuggestionRepository,
            UserContextPort userContextPort) {
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * KHÔNG @Transactional -- và đây là điểm mấu chốt, không phải chuyện bỏ sót.
     *
     * {@code TopicSuggestionService.synchronousOffers} đã cố ý bỏ @Transactional với chú thích
     * "this one is the hottest path... the most load-bearing fix of the three", vì nó gọi LLM
     * đề xuất chủ đề rồi index sang vector store -- hàng chục giây. Nhưng use case này lại bọc
     * @Transactional ra ngoài, nên transaction vẫn mở suốt cuộc gọi đó: công sức bỏ nó ở tầng
     * dưới bị vô hiệu hoàn toàn.
     *
     * Triệu chứng đo được: HikariCP báo "Apparent connection leak detected" trên thread
     * practice-gen-2 với đúng stack ViewSynchronousTopicOffersUseCase.execute, khi kho chủ đề
     * còn trống nên mọi lượt mở màn đều phải sinh mới.
     *
     * Không mất tính nguyên tử: mỗi lệnh gọi repository bên dưới tự transact riêng qua Spring
     * Data proxy. Chỗ này vốn là đọc + tạo từng chủ đề độc lập, không có bất biến nào cần
     * chúng cùng sống cùng chết.
     */
    @Override
    public List<PracticeTopicOffer> execute(ViewSynchronousTopicOffersQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.synchronousOffers(studentId, input.requestedCount());
    }
}
