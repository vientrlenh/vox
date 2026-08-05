package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.exam.CanViewExamBlueprintDataUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamDetailsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewMyExamRoleUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.ViewExamPaperDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.dto.QuestionSelectionSpecDto;

import graphql.schema.DataFetchingEnvironment;

/**
 * Trang sửa mã đề phải phân biệt được ô FIXED (khoá) với ô SELECTION (tự chọn câu). Trước đây
 * ExamPaperItem không lộ slotType nên frontend chỉ biết dựa vào blueprintSlotId và ẩn nút gán câu
 * cho cả hai loại.
 */
class ExamPaperItemSlotFieldTests {

    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();

    private ExamController controller;

    @BeforeEach
    void setUp() {
        controller = new ExamController(
            mock(ViewExamsUseCase.class),
            mock(ViewExamDetailsUseCase.class),
            mock(ViewExamPaperDetailsUseCase.class),
            mock(ViewExamStatusCountsUseCase.class),
            mock(ViewMyExamRoleUseCase.class),
            mock(CanViewExamBlueprintDataUseCase.class),
            mock(UserContextPort.class)
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_expose_the_slot_type_of_a_blueprint_backed_item() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, ExamBlueprintSlotDto>getDataLoader("examBlueprintSlotById")).thenReturn(loader);
        when(loader.load(SLOT_ID)).thenReturn(CompletableFuture.completedFuture(slot("SELECTION", null)));

        var result = controller.examPaperItemSlotType(item(SLOT_ID), env);

        assertThat(result.join()).isEqualTo("SELECTION");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_expose_the_selection_criteria_of_a_selection_slot() {
        var spec = new QuestionSelectionSpecDto("READ_ALOUD", null, null, null, UUID.randomUUID());
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, ExamBlueprintSlotDto>getDataLoader("examBlueprintSlotById")).thenReturn(loader);
        when(loader.load(SLOT_ID)).thenReturn(CompletableFuture.completedFuture(slot("SELECTION", spec)));

        var result = controller.examPaperItemSelectionSpec(item(SLOT_ID), env);

        assertThat(result.join()).isSameAs(spec);
    }

    /** Câu soạn tay không sinh từ blueprint — không được tốn thêm một lượt query slot. */
    @Test
    void should_return_null_slot_type_for_an_item_without_a_blueprint_slot() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        var result = controller.examPaperItemSlotType(item(null), env);

        assertThat(result.join()).isNull();
        verify(env, never()).getDataLoader(any());
    }

    /** Slot đã bị xoá: mapped loader trả null, và trang vẫn phải cho gán câu (fail-open). */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_return_null_slot_type_when_the_slot_row_is_missing() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, ExamBlueprintSlotDto>getDataLoader("examBlueprintSlotById")).thenReturn(loader);
        when(loader.load(SLOT_ID)).thenReturn(CompletableFuture.completedFuture(null));

        var result = controller.examPaperItemSlotType(item(SLOT_ID), env);

        assertThat(result.join()).isNull();
    }

    private ExamPaperItemDto item(UUID blueprintSlotId) {
        return new ExamPaperItemDto(ITEM_ID, blueprintSlotId, SECTION_ID, PAPER_ID, null, 1, BigDecimal.ONE);
    }

    private ExamBlueprintSlotDto slot(String slotType, QuestionSelectionSpecDto spec) {
        return new ExamBlueprintSlotDto(
            SLOT_ID, SECTION_ID, UUID.randomUUID(), 1, BigDecimal.ONE,
            null, null, slotType, null, spec, null, null, null, null);
    }
}
