package com.sep.vox.application.port.input.usecase.topicsuggestion;

import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.topicsuggestion.ViewSynchronousTopicOffersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Service
@RequiredArgsConstructor
public class ViewSynchronousTopicOffersUseCase implements IUseCase<ViewSynchronousTopicOffersQuery, List<PracticeTopicOffer>> {

    private final TopicSuggestionService topicSuggestionRepository;
    private final UserContextPort userContextPort;

    /**
     * NOT @Transactional -- and this is the crux, not an oversight.
     *
     * {@code TopicSuggestionService.synchronousOffers} deliberately dropped @Transactional with the note
     * "this one is the hottest path... the most load-bearing fix of the three", because it calls the LLM
     * to propose topics and then indexes into the vector store -- tens of seconds. But this use case wraps
     * @Transactional around it, so the transaction stays open during that call: dropping it in the
     * lower layer is completely undone.
     *
     * Symptom measured: HikariCP reported "Apparent connection leak detected" on thread
     * practice-gen-2 with exactly the stack ViewSynchronousTopicOffersUseCase.execute, when the topic store
     * was empty so every opening turn had to generate fresh topics.
     *
     * Atomicity is not lost: each repository call below transacts on its own through the Spring
     * Data proxy. This is a read plus an independent creation of each topic, with no invariant that
     * needs them to live or die together.
     */
    @Override
    public List<PracticeTopicOffer> execute(ViewSynchronousTopicOffersQuery input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        return topicSuggestionRepository.synchronousOffers(studentId, input.requestedCount());
    }
}
