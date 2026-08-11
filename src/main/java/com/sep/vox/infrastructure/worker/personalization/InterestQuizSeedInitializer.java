package com.sep.vox.infrastructure.worker.personalization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.personalization.InterestQuizItemRepository;

@Component
public class InterestQuizSeedInitializer implements ApplicationRunner {

    private final InterestQuizItemRepository quizItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterestQuizSeedInitializer(InterestQuizItemRepository quizItemRepository) {
        this.quizItemRepository = quizItemRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        var resource = new ClassPathResource("practice/interest-quiz-seed.json");
        var json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var rawItems = objectMapper.readValue(json, new TypeReference<List<SeedItemJson>>() {
        });
        var items = new java.util.ArrayList<InterestQuizSeedItem>(rawItems.size());
        for (var index = 0; index < rawItems.size(); index++) {
            var item = rawItems.get(index);
            items.add(new InterestQuizSeedItem(
                UUID.nameUUIDFromBytes(("interest-quiz-" + index).getBytes(StandardCharsets.UTF_8)),
                item.dimensionPerStatement(),
                item.statements(),
                item.note()
            ));
        }
        quizItemRepository.seedQuizItemsIfEmpty(items);
    }

    private record SeedItemJson(
        @JsonProperty("dimension_per_statement")
        List<String> dimensionPerStatement,
        List<String> statements,
        String note
    ) {
    }
}
