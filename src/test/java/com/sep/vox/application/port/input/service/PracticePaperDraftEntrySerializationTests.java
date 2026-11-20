package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperDraft;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperQuestion;

import tools.jackson.databind.json.JsonMapper;

/**
 * Phiên dựng đề chuyển từ RAM sang Redis (2026-08-11), nên nó bắt đầu phải đi qua JSON rồi quay về
 * nguyên vẹn. Trước đây nằm trong {@code ConcurrentHashMap} thì cùng một object, không ai serialize
 * bao giờ -- một trường không dựng lại được sẽ không lộ ra lúc build mà lộ ra giữa buổi luyện của
 * học sinh, dưới dạng đúng cái 404 vừa đi sửa.
 *
 * <p>Dùng chính {@code JsonMapper} mà {@code RedisCacheManagerRepository} dùng.
 */
class PracticePaperDraftEntrySerializationTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void draft_dang_READY_phai_di_qua_json_roi_ve_nguyen_ven() {
        var draftId = UUID.randomUUID();
        var studentId = UUID.randomUUID();
        var paper = new PracticePaper(
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
        var original = new PracticePaperDraftService.Entry(
            studentId, PracticePaperDraft.ready(draftId, paper)
        );

        var restored = jsonMapper.readValue(
            jsonMapper.writeValueAsString(original),
            PracticePaperDraftService.Entry.class
        );

        // usingRecursiveComparison: các bản ghi này là record nhưng so sánh sâu vẫn an toàn hơn --
        // equals() của record so từng trường, và một List lồng bên trong so sai thì thông báo lỗi
        // của recursive comparison chỉ thẳng ra trường nào lệch.
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
        assertThat(restored.draft().paper().questions().get(0).suggestedIdeas())
            .containsExactly("kể theo trình tự thời gian", "nêu cảm nhận");
    }

    @Test
    void draft_dang_PREPARING_va_FAILED_khong_co_paper_van_phai_ve_duoc() {
        var draftId = UUID.randomUUID();
        var studentId = UUID.randomUUID();

        for (var draft : List.of(
                PracticePaperDraft.preparing(draftId),
                PracticePaperDraft.failed(draftId, "Bạn đã dùng hết hạn mức luyện tập hôm nay.", "QUOTA_EXCEEDED"))) {
            var original = new PracticePaperDraftService.Entry(studentId, draft);

            var restored = jsonMapper.readValue(
                jsonMapper.writeValueAsString(original),
                PracticePaperDraftService.Entry.class
            );

            assertThat(restored).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
