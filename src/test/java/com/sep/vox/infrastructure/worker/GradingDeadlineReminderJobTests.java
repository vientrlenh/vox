package com.sep.vox.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Câu select có LIMIT, nên một lượt chạy phải đi hết tồn đọng bằng nhiều lô — nếu không
 * thì mail "nhắc TRƯỚC hạn" có thể tới sau khi đã quá hạn, đúng thứ job này sinh ra để
 * tránh.
 */
class GradingDeadlineReminderJobTests {

    private GradingDeadlineReminderBatch batch;
    private GradingDeadlineReminderJob job;

    @BeforeEach
    void setUp() {
        batch = mock(GradingDeadlineReminderBatch.class);
        job = new GradingDeadlineReminderJob(batch);
    }

    @Test
    void should_keep_draining_batches_until_there_is_nothing_left() {
        when(batch.remindOnce(any())).thenReturn(200, 200, 37, 0);

        job.remind();

        verify(batch, times(4)).remindOnce(any());
    }

    @Test
    void should_stop_after_a_single_empty_batch() {
        when(batch.remindOnce(any())).thenReturn(0);

        job.remind();

        verify(batch, times(1)).remindOnce(any());
    }

    @Test
    void should_use_one_threshold_for_the_whole_run() {
        when(batch.remindOnce(any())).thenReturn(1, 1, 0);
        var threshold = ArgumentCaptor.forClass(OffsetDateTime.class);

        job.remind();

        // Tính lại ngưỡng mỗi lô sẽ làm cửa sổ trôi dần và kéo vào phân công chưa tới
        // lúc nhắc.
        verify(batch, times(3)).remindOnce(threshold.capture());
        assertThat(threshold.getAllValues()).containsOnly(threshold.getAllValues().getFirst());
    }

    @Test
    void should_stop_the_run_instead_of_spinning_when_a_batch_blows_up() {
        when(batch.remindOnce(any())).thenThrow(new IllegalStateException("DB sập"));

        assertThatCode(() -> job.remind()).doesNotThrowAnyException();

        // Lô sau hỏng vì cùng nguyên nhân — để lượt 15 phút sau thử lại.
        verify(batch, times(1)).remindOnce(any());
    }

    @Test
    void should_cap_the_number_of_passes_in_one_run() {
        when(batch.remindOnce(any())).thenReturn(200);

        job.remind();

        // Dòng cứ quay lại (bug hoặc dữ liệu lạ) không được làm lượt chạy kéo dài vô hạn.
        verify(batch, times(20)).remindOnce(any());
    }
}
