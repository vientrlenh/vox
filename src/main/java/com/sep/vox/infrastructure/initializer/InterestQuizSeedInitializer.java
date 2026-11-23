package com.sep.vox.infrastructure.initializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class InterestQuizSeedInitializer implements ApplicationRunner {

    private final InterestQuizItemRepository quizItemRepository;
    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource resource = new ClassPathResource("practice/interest-quiz-seed.json");
        String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<SeedItemJson> rawItems = jsonMapper.readValue(json, new TypeReference<List<SeedItemJson>>() {
        });
        List<InterestQuizSeedItem> items = new ArrayList<InterestQuizSeedItem>(rawItems.size());
        for (int index = 0; index < rawItems.size(); index++) {
            SeedItemJson item = rawItems.get(index);
            items.add(InterestQuizSeedItem.builder()
                    .id(UUID.nameUUIDFromBytes(("interest-quiz-" + index).getBytes(StandardCharsets.UTF_8)))
                    .dimensionPerStatement(item.dimensionPerStatement())
                    .statements(item.statements())
                    .note(item.note())
                    .build());
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
