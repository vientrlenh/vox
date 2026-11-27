package com.sep.vox.application.port.input.usecase.topicsuggestion;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.command.topicsuggestion.GenerateTopicFromKeywordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
@RequiredArgsConstructor
public class GenerateTopicFromKeywordUseCase implements IUseCase<GenerateTopicFromKeywordCommand, TopicFromKeywordResult> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    /**
     * NOT @Transactional -- same reason as recorded in {@link ViewSynchronousTopicOffersUseCase},
     * and this is the spot that fix MISSED: three use cases call {@code TopicSuggestionService}
     * but only one of them was fixed.
     *
     * {@code TopicSuggestionService.generateFromKeyword} deliberately dropped @Transactional ("Not
     * &#64;Transactional -- same reason as ... above") because it calls {@code generationClient.propose}
     * -- an LLM round trip of 10-40 seconds -- and then {@code generationClient.index} into the vector store. Wrapping
     * it in @Transactional here keeps the transaction open for that whole time and HOLDS a pool
     * connection, so dropping it in the lower layer is completely undone.
     *
     * Symptom measured 2026-08-05 18:17: HikariCP "Apparent connection leak detected" on
     * thread tomcat-handler-60, with exactly the stack
     * {@code GenerateTopicFromKeywordUseCase$$SpringCGLIB$$0.execute} -> TransactionInterceptor
     * -> {@code JpaTransactionManager.doBegin}. The warning threshold is 5 seconds; one topic generation
     * far exceeds it.
     *
     * Atomicity is not lost: the ONLY part that needs it -- creating the topic + recording the weekly
     * quota request -- already sits in {@code KeywordTopicPersistenceService.createTopicAndRecordRequest}
     * with its own @Transactional. The remaining calls are reads, or a single independent
     * {@code recordKeywordRequest} that transacts on its own through the Spring Data proxy.
     */
    @Override
    public TopicFromKeywordResult execute(GenerateTopicFromKeywordCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.generateFromKeyword(studentId, input.keyword());
    }
}
