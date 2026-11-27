package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperDraft;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperQuestion;

import tools.jackson.databind.json.JsonMapper;

/**
 * The paper-building session moved from RAM to Redis (2026-08-11), so it now has to go through JSON
 * and come back intact. When it lived in a {@code ConcurrentHashMap} it was the same object and
 * nobody ever serialized it -- a field that cannot be rebuilt would not show up at build time but
 * in the middle of a student's practice, as exactly the 404 that was just fixed.
 *
 * <p>Uses the very {@code JsonMapper} that {@code RedisCacheManagerRepository} uses.
 */
class PracticePaperDraftEntrySerializationTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void draft_dang_READY_phai_di_qua_json_roi_ve_nguyen_ven() {
        UUID draftId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        PracticePlanningResponses.PracticePaper paper = new PracticePaper(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "KEYWORD",
            600,
            720,
            900,
            List.of(new PracticePaperQuestion(
                UUID.randomUUID(),
                1,
                "What did you do last weekend?",
                "COHERENCE",
                "organisation",
                3,
                120,
                45,
                List.of("kể theo trình tự thời gian", "nêu cảm nhận")
            ))
        );
        PracticePaperDraftService.Entry original = new PracticePaperDraftService.Entry(
            studentId, PracticePaperDraft.ready(draftId, paper)
        );

        PracticePaperDraftService.Entry restored = jsonMapper.readValue(
            jsonMapper.writeValueAsString(original),
            PracticePaperDraftService.Entry.class
        );

        // usingRecursiveComparison: these entries are records, but a deep comparison is still safer
        // -- a record's equals() compares field by field, and when a nested List differs, the
        // recursive comparison's error message points straight at the field that differs.
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
        assertThat(restored.draft().paper().questions().get(0).suggestedIdeas())
            .containsExactly("kể theo trình tự thời gian", "nêu cảm nhận");
    }

    @Test
    void draft_dang_PREPARING_va_FAILED_khong_co_paper_van_phai_ve_duoc() {
        UUID draftId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        for (PracticePlanningResponses.PracticePaperDraft draft : List.of(
                PracticePaperDraft.preparing(draftId),
                PracticePaperDraft.failed(draftId, "Bạn đã dùng hết hạn mức luyện tập hôm nay.", "QUOTA_EXCEEDED"))) {
            PracticePaperDraftService.Entry original = new PracticePaperDraftService.Entry(studentId, draft);

            PracticePaperDraftService.Entry restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(original),
                PracticePaperDraftService.Entry.class
            );

            assertThat(restored).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
