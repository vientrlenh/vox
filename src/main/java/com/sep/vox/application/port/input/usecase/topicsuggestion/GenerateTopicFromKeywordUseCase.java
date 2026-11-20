package com.sep.vox.application.port.input.usecase.topicsuggestion;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.command.GenerateTopicFromKeywordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
public class GenerateTopicFromKeywordUseCase implements IUseCase<GenerateTopicFromKeywordCommand, TopicFromKeywordResult> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    public GenerateTopicFromKeywordUseCase(
            TopicSuggestionService topicSuggestionRepository,
            UserContextPort userContextPort) {
        this.topicSuggestionRepository = topicSuggestionRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * KHÔNG @Transactional -- cùng lý do đã ghi ở {@link ViewSynchronousTopicOffersUseCase},
     * và đây là chỗ SÓT của lần sửa đó: ba use case cùng gọi {@code TopicSuggestionService}
     * nhưng chỉ một cái được gỡ.
     *
     * {@code TopicSuggestionService.generateFromKeyword} đã cố ý bỏ @Transactional ("Not
     * &#64;Transactional -- same reason as ... above") vì nó gọi {@code generationClient.propose}
     * -- một lượt LLM 10-40 giây -- rồi {@code generationClient.index} sang vector store. Bọc
     * @Transactional ở đây thì transaction mở suốt chừng ấy thời gian và GIỮ một connection
     * của pool, nên công sức bỏ nó ở tầng dưới bị vô hiệu hoàn toàn.
     *
     * Triệu chứng đo được 2026-08-05 18:17: HikariCP "Apparent connection leak detected" trên
     * thread tomcat-handler-60, stack đúng
     * {@code GenerateTopicFromKeywordUseCase$$SpringCGLIB$$0.execute} -> TransactionInterceptor
     * -> {@code JpaTransactionManager.doBegin}. Ngưỡng cảnh báo là 5 giây; một lượt sinh chủ đề
     * vượt xa.
     *
     * Không mất tính nguyên tử: phần DUY NHẤT cần nguyên tử -- tạo topic + ghi nhận lượt hạn
     * mức tuần -- đã nằm trong {@code KeywordTopicPersistenceService.createTopicAndRecordRequest}
     * với @Transactional riêng. Các lệnh còn lại là đọc, hoặc một lần
     * {@code recordKeywordRequest} độc lập, tự transact qua Spring Data proxy.
     */
    @Override
    public TopicFromKeywordResult execute(GenerateTopicFromKeywordCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.generateFromKeyword(studentId, input.keyword());
    }
}
