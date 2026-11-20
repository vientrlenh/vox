package com.sep.vox.domain.model.exam;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Chốt DANH SÁCH loại cảnh báo buộc bài phải qua tay người soát.
 *
 * <p>Sinh ra sau một sự cố thật. Cửa này từng hỏi {@code level = 'CRITICAL'}, và khi vox-streaming
 * hạ MULTIPLE_PERSONS từ CRITICAL xuống WARNING ({@code 4cc6598}) thì một phiên thi có người thứ hai
 * ngồi cùng lặng lẽ thôi bị đẩy sang người soát. Không test nào đỏ: các test của
 * {@code RecordExamAttemptEvaluationUseCase} mock thẳng repository, nên chúng chốt "có cảnh báo thì
 * phải soát" mà không chốt "cảnh báo nào thì tính".
 *
 * <p>Đây là chỗ chốt vế còn thiếu đó. Nó cố ý KHÔNG suy ra danh sách từ bất cứ đâu -- viết thẳng
 * từng tên ra, vì mục đích của nó là bắt người sửa phải sửa hai chỗ và nhìn thấy quyết định mình
 * đang đổi.
 */
class ExamProctoringAlertIntegrityTypesTests {

    /**
     * MULTIPLE_PERSONS là ca đã hỏng, nên nó đứng riêng: mức của nó là WARNING mà vẫn phải nằm trong
     * nhóm này. Đúng cái nghịch lý biểu kiến đó là điều luật này khẳng định -- "giám thị không cần
     * chạy tới ngay" và "kết quả không còn đáng tin" là hai chuyện khác nhau.
     */
    @Test
    void a_second_person_in_frame_forces_review_even_though_its_alert_level_is_only_warning() {
        assertThat(ExamProctoringAlert.INTEGRITY_ALERT_TYPES).contains("MULTIPLE_PERSONS");
    }

    @Test
    void the_integrity_group_is_exactly_the_three_types_that_question_who_answered() {
        assertThat(ExamProctoringAlert.INTEGRITY_ALERT_TYPES)
            .containsExactlyInAnyOrder("MULTIPLE_PERSONS", "PHONE_DETECTED", "PROHIBITED_OBJECT");
    }

    /**
     * Vế còn lại của quyết định, và là vế giữ cho việc soát còn có nghĩa: những loại xảy ra thường
     * xuyên trong một ca thi thật KHÔNG được lọt vào. Một phiên có 16 lượt PERSON_MISSING là số đo
     * thật; tính chúng vào thì gần như mọi bài đều rơi sang chờ soát.
     */
    @Test
    void routine_exam_noise_stays_out_so_that_review_keeps_meaning_something() {
        assertThat(ExamProctoringAlert.INTEGRITY_ALERT_TYPES)
            .doesNotContain(
                "PERSON_MISSING",
                "WINDOW_FOCUS_LOST",
                "CAMERA_SIGNAL_LOST",
                "CAMERA_SIGNAL_RESTORED",
                "STREAM_DROPPED",
                "RECORDING_INCOMPLETE",
                "RECORDING_TRUNCATED",
                "UNCOOPERATIVE_CANDIDATE");
    }

    /**
     * Danh sách được so bằng {@code UPPER(TRIM(...))} trong JPQL, nên một tên viết thường ở đây sẽ
     * không bao giờ khớp bản ghi nào -- và hỏng theo kiểu im lặng nhất có thể: cửa soát vẫn chạy,
     * chỉ là không bắt được gì.
     */
    @Test
    void every_type_is_stored_upper_case_because_the_query_compares_upper_case() {
        assertThat(ExamProctoringAlert.INTEGRITY_ALERT_TYPES)
            .allSatisfy(type -> assertThat(type).isEqualTo(type.toUpperCase().trim()));
    }
}
