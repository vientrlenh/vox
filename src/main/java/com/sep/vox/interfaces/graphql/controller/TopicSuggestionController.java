package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.GenerateTopicFromKeywordCommand;
import com.sep.vox.application.port.input.command.RespondToTopicSuggestionCommand;
import com.sep.vox.application.port.input.usecase.topicsuggestion.GenerateTopicFromKeywordUseCase;
import com.sep.vox.application.port.input.usecase.topicsuggestion.RespondToTopicSuggestionUseCase;
import com.sep.vox.application.port.input.usecase.topicsuggestion.ViewPendingTopicSuggestionsUseCase;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicSuggestion;

@Controller
public class TopicSuggestionController {

    private final RespondToTopicSuggestionUseCase respondToTopicSuggestionUseCase;
    private final GenerateTopicFromKeywordUseCase generateTopicFromKeywordUseCase;
    private final ViewPendingTopicSuggestionsUseCase viewPendingTopicSuggestionsUseCase;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public TopicSuggestionController(
            RespondToTopicSuggestionUseCase respondToTopicSuggestionUseCase,
            GenerateTopicFromKeywordUseCase generateTopicFromKeywordUseCase,
            ViewPendingTopicSuggestionsUseCase viewPendingTopicSuggestionsUseCase,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.respondToTopicSuggestionUseCase = respondToTopicSuggestionUseCase;
        this.generateTopicFromKeywordUseCase = generateTopicFromKeywordUseCase;
        this.viewPendingTopicSuggestionsUseCase = viewPendingTopicSuggestionsUseCase;
        this.practiceGenerationExecutor = practiceGenerationExecutor;
    }

    /**
     * Trả {@code CompletableFuture} -- cùng lý do đã ghi ở
     * {@code PracticePlanningController.practiceTopicOffers}: {@code respond} khi NHẬN gợi ý sẽ
     * gọi {@code generationClient.index} sang vector store, một cuộc gọi mạng. Chạy async đẩy
     * servlet sang chế độ bất đồng bộ, và đó là lúc OSIV nhả EntityManager + connection DB.
     */
    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CompletableFuture<Boolean> respondToTopicSuggestion(
            @Argument("suggestionId") UUID suggestionId,
            @Argument("accept") boolean accept) {
        return CompletableFuture.supplyAsync(
            () -> respondToTopicSuggestionUseCase.execute(
                new RespondToTopicSuggestionCommand(suggestionId, accept)
            ),
            practiceGenerationExecutor
        );
    }

    /**
     * Trả {@code CompletableFuture} -- đây là chỗ RÒ CONNECTION nặng nhất của cả controller,
     * đo được 2026-08-05 18:32.
     *
     * <p>Vì sao gỡ {@code @Transactional} ở use case KHÔNG đủ: {@code spring.jpa.open-in-view}
     * để mặc định (true), nên Hibernate Session -- kèm connection của nó -- được giữ suốt cả
     * HTTP request mà chẳng cần transaction nào. Trình tự thật trong
     * {@code TopicSuggestionService.generateFromKeyword}:
     *
     * <pre>
     *   :131  findNearExistingTopic -> findAllActive     LẤY connection
     *   :142  weeklyRequestCount                          giữ
     *   :154  generationClient.propose  (LLM 10-40 giây)  VẪN GIỮ
     * </pre>
     *
     * HikariCP cảnh báo ở ngưỡng 5000ms, và không có cách sắp xếp lại thân hàm để né -- kiểm
     * hạn mức, kiểm trùng, kiểm mục tiêu đều cần DB và đều phải chạy TRƯỚC khi hỏi LLM.
     *
     * <p>Chạy trên {@code practiceGenerationExecutor} thì toàn bộ phần đó diễn ra ngoài thread
     * request, nên request thread không mở session nào để mà giữ. Executor là
     * {@code DelegatingSecurityContextAsyncTaskExecutor} nên {@code UserContextPort} bên trong
     * vẫn đọc được người dùng hiện tại; {@code @PreAuthorize} vẫn chạy sớm trên thread request.
     *
     * <p>Không cần tách 2 pha kiểu {@code PracticePaperDraftService}: ở đó phải tách vì client
     * cần phản hồi NGAY để hiện "đang soạn đề", còn ở đây học sinh vốn đã chấp nhận chờ sau khi
     * bấm "Nhờ AI soạn chủ đề". Vấn đề cần giải chỉ là connection, không phải thời gian chờ.
     */
    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CompletableFuture<TopicFromKeywordResult> generateTopicFromKeyword(
            @Argument("keyword") String keyword) {
        return CompletableFuture.supplyAsync(
            () -> generateTopicFromKeywordUseCase.execute(
                new GenerateTopicFromKeywordCommand(keyword)
            ),
            practiceGenerationExecutor
        );
    }

    /**
     * Trước là field {@code suggestions} gắn vào {@code InterestProfile}; type đó đã bỏ cùng
     * trang hồ sơ sở thích nên chuyển thành query mức trên. Chưa có client nào gọi -- xem
     * ghi chú trong topic-suggestion.graphqls.
     */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<TopicSuggestion> myPendingTopicSuggestions() {
        return viewPendingTopicSuggestionsUseCase.execute(null);
    }
}
