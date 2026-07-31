package com.sep.vox.infrastructure.initializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import com.sep.vox.domain.valueobject.scoringruleaction.CapCriterionScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFinalScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.RequireHumanReviewParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ConfidenceThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.DurationThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.RatioThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.WordCountThresholdParams;

/**
 * Seed một trường THPT hoàn chỉnh cho môi trường phát triển, đi từ cơ cấu tổ chức
 * tới ba kỳ thi đã lên lịch xong và sẵn sàng điểm danh.
 *
 * <p><b>Khối lượng:</b> 1 trường, 3 khối (10/11/12), mỗi khối 2 lớp, mỗi lớp 10 học
 * sinh (60 HS), 6 giáo viên, 1 quản trị trường, 4 phòng thi.
 *
 * <p><b>Trọng tâm — mức đánh giá theo khối.</b> Khung năng lực ngoại ngữ 6 bậc Việt
 * Nam được seed đầy đủ 6 bậc × 5 tiêu chí. Mỗi cặp (tiêu chí, bậc) là một
 * {@link FrameworkCriterionBand} mang bộ dấu hiệu dương/âm riêng, và <em>cùng một mã
 * dấu hiệu sẽ gắt dần khi bậc tăng</em>: ngưỡng trong mô tả siết lại và
 * {@link FrameworkCriterionSignalImportance} nâng từ LOW lên HIGH. Ví dụ dấu hiệu
 * {@code PRON_FINAL_DROP} (rụng phụ âm cuối) ở Bậc 1 chỉ tính khi vượt 50% số từ và
 * xếp mức LOW, tới Bậc 5 chỉ cần vượt 10% và đã là HIGH. Ba
 * {@link AssessmentPolicy} trỏ tới ba bậc mục tiêu khác nhau — khối 10 → Bậc 3 (B1),
 * khối 11 → Bậc 4 (B2), khối 12 → Bậc 5 (C1) — nên cùng một màn trình diễn sẽ được
 * đối chiếu với bộ dấu hiệu khắt khe hơn hẳn khi học sinh lên lớp.
 *
 * <p><b>Lưu ý về hiện trạng code.</b> Hai thứ dưới đây được seed đúng ý nghĩa nghiệp
 * vụ nhưng luồng chấm hiện chưa đọc tới, nên đây là dữ liệu chuẩn bị sẵn chứ chưa có
 * hiệu lực khi chấm:
 * <ul>
 *   <li>{@link ScoringRule} chưa có engine — không use case nào đọc rule trong luồng
 *       chấm, và {@code ExamItemRuleHit} chưa được ghi ở đâu.</li>
 *   <li>{@link AssessmentPolicyStrictness} chỉ được gán khi tạo/sửa policy, không
 *       tham gia tính điểm.</li>
 * </ul>
 * {@code passingScore} để {@code null} theo chủ đích: việc phân loại đạt/không đạt
 * dựa vào bậc mục tiêu và dấu hiệu của bậc, không dựa vào một ngưỡng điểm cứng.
 *
 * <p><b>Điểm dừng:</b> mã đề đã {@code LOCKED}, thí sinh đã được phân đề và xếp ca,
 * ca thi {@code PUBLISHED} đã có giám thị, kỳ thi ở {@code SCHEDULED}. Bước tiếp theo
 * trên UI là điểm danh rồi giám thị phát OTP.
 *
 * <p>Initializer tắt mặc định ({@code sample-data.enabled=true} để bật) và tự bỏ qua
 * nếu trường đã được seed, nên chạy lại nhiều lần là an toàn.
 */
@Component
@Order(4)
@ConditionalOnProperty(prefix = "sample-data", name = "enabled", havingValue = "true")
public class SampleSchoolDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleSchoolDataInitializer.class);

    private static final String SAMPLE_SCHOOL_CODE = "SAMPLE01";
    private static final String SAMPLE_SCHOOL_DOMAIN = "sample.edu.vn";
    private static final String ENGLISH_CODE = "ENG";
    private static final String ACADEMIC_YEAR = "2025-2026";
    private static final String FRAMEWORK_CODE = "KNLNN-VN";
    private static final String FRAMEWORK_VERSION_CODE = "KNLNN-VN-V1";
    private static final String RUBRIC_CODE = "RUB-" + SAMPLE_SCHOOL_CODE + "-SPEAKING";
    private static final String RUBRIC_VERSION_CODE = RUBRIC_CODE + "-V1";

    private static final String SCHOOL_ADMIN_ROLE_CODE = "SCHOOL_ADMIN";
    private static final String TEACHER_ROLE_CODE = "TEACHER";
    private static final String STUDENT_ROLE_CODE = "STUDENT";
    private static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";

    private static final int CLASSES_PER_GRADE_LEVEL = 2;
    private static final int STUDENTS_PER_CLASS = 10;
    private static final int PAPER_VARIANTS = 2;
    private static final int SCHEDULES_PER_EXAM = 2;
    /** 4 câu × (30s chuẩn bị + 90s trả lời) = 480s, cộng biên cho thao tác vào/ra phòng thi. */
    private static final int EXAM_DURATION_SECONDS = 900;

    private static final FrameworkCriterionSignalImportance LOW = FrameworkCriterionSignalImportance.LOW;
    private static final FrameworkCriterionSignalImportance MEDIUM = FrameworkCriterionSignalImportance.MEDIUM;
    private static final FrameworkCriterionSignalImportance HIGH = FrameworkCriterionSignalImportance.HIGH;

    // ---------------------------------------------------------------------------------
    // Khung năng lực ngoại ngữ 6 bậc dùng cho Việt Nam
    // ---------------------------------------------------------------------------------

    /** Sáu bậc của khung, kèm mốc CEFR tương đương để đối chiếu. */
    private static final List<ResultBandSeed> FRAMEWORK_BANDS = List.of(
        new ResultBandSeed("BAC1", "Bậc 1 (A1)", "Sử dụng được các cụm cố định trong tình huống rất quen thuộc.", 1),
        new ResultBandSeed("BAC2", "Bậc 2 (A2)", "Trao đổi được thông tin đơn giản về chủ đề hằng ngày.", 2),
        new ResultBandSeed("BAC3", "Bậc 3 (B1)", "Trình bày liền mạch về chủ đề quen thuộc, nêu được lý do.", 3),
        new ResultBandSeed("BAC4", "Bậc 4 (B2)", "Lập luận rõ ràng, chi tiết về nhiều chủ đề, kể cả trừu tượng.", 4),
        new ResultBandSeed("BAC5", "Bậc 5 (C1)", "Diễn đạt linh hoạt, hiệu quả cho mục đích học thuật và nghề nghiệp.", 5),
        new ResultBandSeed("BAC6", "Bậc 6 (C2)", "Diễn đạt tinh tế, chính xác gần như người bản ngữ có học vấn.", 6)
    );

    /**
     * Năm tiêu chí và mô tả của từng bậc.
     *
     * <p>Đọc theo cột dọc sẽ thấy ý đồ chính của bộ seed: cùng một mã dấu hiệu âm
     * (ví dụ {@code FLU_LONG_PAUSE}) xuất hiện ở mọi bậc nhưng ngưỡng kích hoạt siết
     * dần và mức quan trọng tăng dần, nên bậc mục tiêu càng cao thì cùng một lỗi càng
     * bị trừ nặng.
     */
    private static final List<CriterionSeed> FRAMEWORK_CRITERIA = List.of(
        new CriterionSeed("PRONUNCIATION", "Phát âm và trọng âm",
            "Độ dễ hiểu của âm, trọng âm từ, trọng âm câu và ngữ điệu.", 1, List.of(
            new CriterionBandSeed("BAC1",
                "Phát âm ở mức từ rời; người nghe đã quen lỗi phát âm của người Việt mới hiểu được phần lớn.",
                List.of(
                    sig("PRON_WORD_CLEAR", "Phát âm rõ ở các từ đơn quen thuộc đã học", LOW, "so khớp từng từ với transcript"),
                    sig("PRON_EFFORT", "Có nỗ lực bắt chước âm chuẩn dù chưa ổn định", LOW, "nghe mẫu 2-3 từ bất kỳ")
                ),
                List.of(
                    sig("PRON_FINAL_DROP", "Rụng phụ âm cuối ở trên 50% số từ nội dung", LOW, "đếm phụ âm cuối trên transcript đã căn thời gian"),
                    sig("PRON_UNINTELLIGIBLE", "Trên 40% lượt nói người nghe không đoán được nội dung", MEDIUM, "tỉ lệ từ ASR gắn cờ không nhận dạng")
                )),
            new CriterionBandSeed("BAC2",
                "Phát âm đủ để truyền đạt ý đơn giản; trọng âm từ còn sai nhưng ít cản trở người nghe.",
                List.of(
                    sig("PRON_WORD_CLEAR", "Phát âm ổn định ở vốn từ quen thuộc, người nghe không phải hỏi lại", LOW, "so khớp từng từ với transcript"),
                    sig("PRON_WORD_STRESS", "Đặt đúng trọng âm ở các từ hai âm tiết thông dụng", LOW, "kiểm tra trọng âm từ trên audio")
                ),
                List.of(
                    sig("PRON_FINAL_DROP", "Rụng phụ âm cuối ở trên 40% số từ nội dung", LOW, "đếm phụ âm cuối trên transcript đã căn thời gian"),
                    sig("PRON_UNINTELLIGIBLE", "Trên 30% lượt nói người nghe không đoán được nội dung", MEDIUM, "tỉ lệ từ ASR gắn cờ không nhận dạng")
                )),
            new CriterionBandSeed("BAC3",
                "Phát âm dễ hiểu xuyên suốt; lỗi còn nhưng người nghe hiếm khi phải hỏi lại.",
                List.of(
                    sig("PRON_WORD_STRESS", "Trọng âm từ đúng ở phần lớn từ đa âm tiết", MEDIUM, "kiểm tra trọng âm từ trên audio"),
                    sig("PRON_SENTENCE_RHYTHM", "Bắt đầu có nhịp câu, không đọc đều từng từ một", LOW, "khoảng cách giữa các từ trong bản căn thời gian")
                ),
                List.of(
                    sig("PRON_FINAL_DROP", "Rụng phụ âm cuối ở trên 30% số từ nội dung", MEDIUM, "đếm phụ âm cuối trên transcript đã căn thời gian"),
                    sig("PRON_UNINTELLIGIBLE", "Trên 20% lượt nói người nghe không đoán được nội dung", HIGH, "tỉ lệ từ ASR gắn cờ không nhận dạng")
                )),
            new CriterionBandSeed("BAC4",
                "Phát âm rõ và tự nhiên; trọng âm câu đã phục vụ việc làm nổi bật thông tin.",
                List.of(
                    sig("PRON_SENTENCE_RHYTHM", "Nhịp câu tự nhiên, biết giảm nhẹ từ chức năng", MEDIUM, "khoảng cách giữa các từ trong bản căn thời gian"),
                    sig("PRON_INTONATION", "Ngữ điệu phân biệt được câu hỏi, liệt kê và kết thúc ý", MEDIUM, "đường cao độ ở cuối mỗi mệnh đề")
                ),
                List.of(
                    sig("PRON_FINAL_DROP", "Rụng phụ âm cuối ở trên 20% số từ nội dung", MEDIUM, "đếm phụ âm cuối trên transcript đã căn thời gian"),
                    sig("PRON_FLAT_INTONATION", "Ngữ điệu phẳng trên 40% số mệnh đề, nghe như đang đọc thuộc", MEDIUM, "độ lệch cao độ trong từng mệnh đề")
                )),
            new CriterionBandSeed("BAC5",
                "Phát âm chính xác và linh hoạt; dùng trọng âm, ngữ điệu để thể hiện sắc thái ý.",
                List.of(
                    sig("PRON_INTONATION", "Ngữ điệu chủ động thể hiện thái độ, nhấn mạnh, tương phản", HIGH, "đường cao độ ở cuối mỗi mệnh đề"),
                    sig("PRON_CONNECTED_SPEECH", "Có nối âm, nuốt âm tự nhiên như lời nói liên tục", MEDIUM, "ranh giới từ trong bản căn thời gian")
                ),
                List.of(
                    sig("PRON_FINAL_DROP", "Rụng phụ âm cuối ở trên 10% số từ nội dung", HIGH, "đếm phụ âm cuối trên transcript đã căn thời gian"),
                    sig("PRON_FLAT_INTONATION", "Ngữ điệu phẳng trên 25% số mệnh đề", HIGH, "độ lệch cao độ trong từng mệnh đề")
                )),
            new CriterionBandSeed("BAC6",
                "Phát âm tinh tế, gần như bản ngữ có học vấn; điều chỉnh được theo mục đích giao tiếp.",
                List.of(
                    sig("PRON_CONNECTED_SPEECH", "Nối âm, đồng hoá âm tự nhiên và nhất quán", HIGH, "ranh giới từ trong bản căn thời gian"),
                    sig("PRON_REGISTER_CONTROL", "Điều chỉnh được độ trang trọng của cách phát âm theo ngữ cảnh", HIGH, "đối chiếu với yêu cầu ngữ cảnh của đề")
                ),
                List.of(
                    sig("PRON_FINAL_DROP", "Rụng phụ âm cuối ở trên 5% số từ nội dung", HIGH, "đếm phụ âm cuối trên transcript đã căn thời gian"),
                    sig("PRON_FLAT_INTONATION", "Ngữ điệu phẳng trên 15% số mệnh đề", HIGH, "độ lệch cao độ trong từng mệnh đề")
                ))
        )),

        new CriterionSeed("FLUENCY", "Độ trôi chảy",
            "Tốc độ nói, độ dài đoạn nói liền mạch, mức độ ngập ngừng và tự sửa.", 2, List.of(
            new CriterionBandSeed("BAC1",
                "Nói thành từng cụm ngắn, ngắt quãng nhiều để tìm từ.",
                List.of(
                    sig("FLU_SHORT_CHUNK", "Nói được cụm 3-4 từ liền mạch", LOW, "độ dài đoạn giữa hai khoảng lặng"),
                    sig("FLU_KEEPS_GOING", "Cố gắng nói tiếp sau khi ngập ngừng thay vì bỏ lượt", LOW, "có lời nói sau khoảng lặng dài")
                ),
                List.of(
                    sig("FLU_LONG_PAUSE", "Khoảng lặng trên 5 giây xuất hiện quá 4 lần", LOW, "phân đoạn khoảng lặng trong audio"),
                    sig("FLU_SILENCE_RATIO", "Tỉ lệ im lặng vượt 55% thời lượng bài nói", MEDIUM, "tổng khoảng lặng chia thời lượng")
                )),
            new CriterionBandSeed("BAC2",
                "Duy trì được lượt nói ngắn; ngập ngừng còn rõ khi chuyển ý.",
                List.of(
                    sig("FLU_SHORT_CHUNK", "Nói được cụm 5-7 từ liền mạch", LOW, "độ dài đoạn giữa hai khoảng lặng"),
                    sig("FLU_SELF_REPAIR", "Tự sửa được lỗi mà không làm đứt hẳn mạch nói", LOW, "vị trí lặp/sửa trên transcript")
                ),
                List.of(
                    sig("FLU_LONG_PAUSE", "Khoảng lặng trên 4 giây xuất hiện quá 4 lần", LOW, "phân đoạn khoảng lặng trong audio"),
                    sig("FLU_SILENCE_RATIO", "Tỉ lệ im lặng vượt 45% thời lượng bài nói", MEDIUM, "tổng khoảng lặng chia thời lượng")
                )),
            new CriterionBandSeed("BAC3",
                "Nói liên tục ở độ dài vừa phải; ngập ngừng chủ yếu khi tìm cấu trúc.",
                List.of(
                    sig("FLU_SUSTAINED_TURN", "Duy trì lượt nói trên 30 giây không cần nhắc", MEDIUM, "thời lượng đoạn nói liên tục"),
                    sig("FLU_SELF_REPAIR", "Tự sửa gọn, không lặp lại cả câu", MEDIUM, "vị trí lặp/sửa trên transcript")
                ),
                List.of(
                    sig("FLU_LONG_PAUSE", "Khoảng lặng trên 3 giây xuất hiện quá 3 lần", MEDIUM, "phân đoạn khoảng lặng trong audio"),
                    sig("FLU_SILENCE_RATIO", "Tỉ lệ im lặng vượt 35% thời lượng bài nói", MEDIUM, "tổng khoảng lặng chia thời lượng")
                )),
            new CriterionBandSeed("BAC4",
                "Nói trôi chảy ở nhịp gần tự nhiên; ngập ngừng không cản trở người nghe.",
                List.of(
                    sig("FLU_SUSTAINED_TURN", "Duy trì lượt nói trên 60 giây có cấu trúc", MEDIUM, "thời lượng đoạn nói liên tục"),
                    sig("FLU_NATURAL_RATE", "Tốc độ nói trong khoảng 110-160 từ/phút", MEDIUM, "số từ chia thời lượng nói")
                ),
                List.of(
                    sig("FLU_LONG_PAUSE", "Khoảng lặng trên 3 giây xuất hiện quá 2 lần", MEDIUM, "phân đoạn khoảng lặng trong audio"),
                    sig("FLU_SILENCE_RATIO", "Tỉ lệ im lặng vượt 25% thời lượng bài nói", HIGH, "tổng khoảng lặng chia thời lượng")
                )),
            new CriterionBandSeed("BAC5",
                "Nói lưu loát, gần như không phải cố gắng; ngập ngừng chỉ để chọn ý.",
                List.of(
                    sig("FLU_NATURAL_RATE", "Tốc độ ổn định 130-170 từ/phút, biết chủ động chậm lại để nhấn ý", HIGH, "số từ chia thời lượng nói"),
                    sig("FLU_STRATEGIC_PAUSE", "Ngừng đúng chỗ để tạo hiệu quả diễn đạt", MEDIUM, "vị trí khoảng lặng so với ranh giới mệnh đề")
                ),
                List.of(
                    sig("FLU_LONG_PAUSE", "Khoảng lặng trên 2 giây xuất hiện quá 2 lần", HIGH, "phân đoạn khoảng lặng trong audio"),
                    sig("FLU_SILENCE_RATIO", "Tỉ lệ im lặng vượt 18% thời lượng bài nói", HIGH, "tổng khoảng lặng chia thời lượng")
                )),
            new CriterionBandSeed("BAC6",
                "Nói tự nhiên hoàn toàn; nhịp điệu phục vụ ý đồ chứ không phải do tìm từ.",
                List.of(
                    sig("FLU_STRATEGIC_PAUSE", "Kiểm soát nhịp một cách có chủ đích xuyên suốt", HIGH, "vị trí khoảng lặng so với ranh giới mệnh đề"),
                    sig("FLU_EFFORTLESS", "Không có dấu hiệu phải tìm từ ngay cả với chủ đề trừu tượng", HIGH, "mật độ từ đệm trên transcript")
                ),
                List.of(
                    sig("FLU_LONG_PAUSE", "Khoảng lặng trên 2 giây xuất hiện quá 1 lần", HIGH, "phân đoạn khoảng lặng trong audio"),
                    sig("FLU_SILENCE_RATIO", "Tỉ lệ im lặng vượt 12% thời lượng bài nói", HIGH, "tổng khoảng lặng chia thời lượng")
                ))
        )),

        new CriterionSeed("VOCABULARY", "Từ vựng",
            "Độ rộng, độ chính xác và tính phù hợp ngữ cảnh của vốn từ.", 3, List.of(
            new CriterionBandSeed("BAC1",
                "Vốn từ giới hạn ở cụm cố định và chủ đề cá nhân rất quen.",
                List.of(
                    sig("VOC_BASIC_RANGE", "Dùng đúng các từ cơ bản về bản thân, gia đình, trường lớp", LOW, "đối chiếu danh sách từ vựng cơ bản"),
                    sig("VOC_FORMULAIC", "Dùng được cụm cố định đã học đúng tình huống", LOW, "so khớp cụm cố định trên transcript")
                ),
                List.of(
                    sig("VOC_CODE_SWITCH", "Chèn tiếng Việt ở trên 30% số câu", LOW, "tỉ lệ chuyển mã trên transcript"),
                    sig("VOC_REPETITION", "Trên 45% từ nội dung là lặp lại của nhau", LOW, "tỉ lệ từ khác nhau trên tổng số từ")
                )),
            new CriterionBandSeed("BAC2",
                "Đủ từ cho nhu cầu hằng ngày; diễn đạt vòng vo khi thiếu từ.",
                List.of(
                    sig("VOC_BASIC_RANGE", "Bao phủ được các chủ đề sinh hoạt thường ngày", LOW, "đối chiếu danh sách từ vựng cơ bản"),
                    sig("VOC_PARAPHRASE", "Biết diễn đạt vòng khi chưa nhớ từ chính xác", LOW, "cụm mô tả thay cho từ đích")
                ),
                List.of(
                    sig("VOC_CODE_SWITCH", "Chèn tiếng Việt ở trên 20% số câu", MEDIUM, "tỉ lệ chuyển mã trên transcript"),
                    sig("VOC_REPETITION", "Trên 40% từ nội dung là lặp lại của nhau", LOW, "tỉ lệ từ khác nhau trên tổng số từ")
                )),
            new CriterionBandSeed("BAC3",
                "Đủ từ để bàn về chủ đề quen thuộc, có một số cụm cố định dùng đúng.",
                List.of(
                    sig("VOC_TOPIC_RANGE", "Dùng đúng từ chuyên biệt của chủ đề đề bài đưa ra", MEDIUM, "đối chiếu từ khoá của chủ đề"),
                    sig("VOC_PARAPHRASE", "Diễn đạt vòng hiệu quả, người nghe vẫn nắm được ý", MEDIUM, "cụm mô tả thay cho từ đích")
                ),
                List.of(
                    sig("VOC_CODE_SWITCH", "Chèn tiếng Việt ở trên 12% số câu", MEDIUM, "tỉ lệ chuyển mã trên transcript"),
                    sig("VOC_REPETITION", "Trên 32% từ nội dung là lặp lại của nhau", MEDIUM, "tỉ lệ từ khác nhau trên tổng số từ")
                )),
            new CriterionBandSeed("BAC4",
                "Vốn từ rộng, dùng được cụm cố định và từ trừu tượng ở mức chính xác tốt.",
                List.of(
                    sig("VOC_COLLOCATION", "Kết hợp từ tự nhiên, đúng cụm quen dùng", MEDIUM, "đối chiếu ngân hàng cụm kết hợp"),
                    sig("VOC_ABSTRACT", "Diễn đạt được khái niệm trừu tượng bằng từ phù hợp", MEDIUM, "đối chiếu từ khoá trừu tượng của đề")
                ),
                List.of(
                    sig("VOC_CODE_SWITCH", "Chèn tiếng Việt ở trên 6% số câu", HIGH, "tỉ lệ chuyển mã trên transcript"),
                    sig("VOC_WRONG_COLLOCATION", "Trên 15% cụm kết hợp dùng sai gây hiểu lệch ý", MEDIUM, "so khớp cụm với ngân hàng kết hợp")
                )),
            new CriterionBandSeed("BAC5",
                "Vốn từ phong phú và chính xác; xử lý được sắc thái và cách nói ẩn ý.",
                List.of(
                    sig("VOC_COLLOCATION", "Kết hợp từ chuẩn xác kể cả ở đăng ký ngôn ngữ trang trọng", HIGH, "đối chiếu ngân hàng cụm kết hợp"),
                    sig("VOC_NUANCE", "Chọn từ phân biệt được sắc thái gần nghĩa", HIGH, "đối chiếu cặp từ gần nghĩa trong ngữ cảnh")
                ),
                List.of(
                    sig("VOC_CODE_SWITCH", "Có bất kỳ chuyển mã sang tiếng Việt nào ngoài tên riêng", HIGH, "tỉ lệ chuyển mã trên transcript"),
                    sig("VOC_WRONG_COLLOCATION", "Trên 8% cụm kết hợp dùng sai", HIGH, "so khớp cụm với ngân hàng kết hợp")
                )),
            new CriterionBandSeed("BAC6",
                "Làm chủ vốn từ, kể cả thành ngữ và cách nói mang tính văn hoá.",
                List.of(
                    sig("VOC_NUANCE", "Chọn từ tinh tế, nhất quán với đăng ký ngôn ngữ của toàn bài", HIGH, "đối chiếu cặp từ gần nghĩa trong ngữ cảnh"),
                    sig("VOC_IDIOMATIC", "Dùng thành ngữ, cách nói bản ngữ đúng ngữ cảnh", HIGH, "đối chiếu ngân hàng thành ngữ")
                ),
                List.of(
                    sig("VOC_CODE_SWITCH", "Có bất kỳ chuyển mã sang tiếng Việt nào ngoài tên riêng", HIGH, "tỉ lệ chuyển mã trên transcript"),
                    sig("VOC_WRONG_COLLOCATION", "Trên 4% cụm kết hợp dùng sai", HIGH, "so khớp cụm với ngân hàng kết hợp")
                ))
        )),

        new CriterionSeed("GRAMMAR", "Ngữ pháp",
            "Độ đa dạng và độ chính xác của cấu trúc câu.", 4, List.of(
            new CriterionBandSeed("BAC1",
                "Chủ yếu là cụm học thuộc; cấu trúc câu còn rất hạn chế.",
                List.of(
                    sig("GRA_SIMPLE_SENTENCE", "Ghép được câu đơn có chủ ngữ và động từ", LOW, "phân tích cú pháp transcript"),
                    sig("GRA_MEMORISED", "Dùng đúng mẫu câu đã học thuộc", LOW, "so khớp mẫu câu")
                ),
                List.of(
                    sig("GRA_ERROR_RATE", "Trên 45% mệnh đề có lỗi ngữ pháp", LOW, "số mệnh đề lỗi trên tổng mệnh đề"),
                    sig("GRA_BLOCKING_ERROR", "Trên 5 lỗi khiến người nghe hiểu sai ý", MEDIUM, "lỗi được gắn cờ cản trở giao tiếp")
                )),
            new CriterionBandSeed("BAC2",
                "Dùng được câu đơn ổn định, bắt đầu ghép câu bằng liên từ cơ bản.",
                List.of(
                    sig("GRA_SIMPLE_SENTENCE", "Câu đơn đúng ở phần lớn lượt nói", LOW, "phân tích cú pháp transcript"),
                    sig("GRA_BASIC_CONNECTOR", "Nối câu bằng and, but, because đúng chức năng", LOW, "danh sách liên từ trên transcript")
                ),
                List.of(
                    sig("GRA_ERROR_RATE", "Trên 38% mệnh đề có lỗi ngữ pháp", LOW, "số mệnh đề lỗi trên tổng mệnh đề"),
                    sig("GRA_BLOCKING_ERROR", "Trên 4 lỗi khiến người nghe hiểu sai ý", MEDIUM, "lỗi được gắn cờ cản trở giao tiếp")
                )),
            new CriterionBandSeed("BAC3",
                "Kiểm soát tốt cấu trúc quen thuộc; câu phức bắt đầu xuất hiện.",
                List.of(
                    sig("GRA_COMPLEX_SENTENCE", "Dùng được mệnh đề phụ thuộc đúng cấu trúc", MEDIUM, "phân tích cú pháp transcript"),
                    sig("GRA_TENSE_CONTROL", "Dùng đúng thì ở các mốc thời gian rõ ràng", MEDIUM, "đối chiếu thì với mốc thời gian trong câu")
                ),
                List.of(
                    sig("GRA_ERROR_RATE", "Trên 28% mệnh đề có lỗi ngữ pháp", MEDIUM, "số mệnh đề lỗi trên tổng mệnh đề"),
                    sig("GRA_BLOCKING_ERROR", "Trên 2 lỗi khiến người nghe hiểu sai ý", HIGH, "lỗi được gắn cờ cản trở giao tiếp")
                )),
            new CriterionBandSeed("BAC4",
                "Dùng đa dạng cấu trúc với độ chính xác cao; lỗi hiếm và tự sửa được.",
                List.of(
                    sig("GRA_COMPLEX_SENTENCE", "Câu phức đa dạng, không lặp một khuôn duy nhất", MEDIUM, "phân tích cú pháp transcript"),
                    sig("GRA_SELF_CORRECTION", "Nhận ra và sửa lỗi của chính mình ngay trong lượt nói", MEDIUM, "vị trí sửa lỗi trên transcript")
                ),
                List.of(
                    sig("GRA_ERROR_RATE", "Trên 18% mệnh đề có lỗi ngữ pháp", MEDIUM, "số mệnh đề lỗi trên tổng mệnh đề"),
                    sig("GRA_BLOCKING_ERROR", "Có bất kỳ lỗi nào khiến người nghe hiểu sai ý", HIGH, "lỗi được gắn cờ cản trở giao tiếp")
                )),
            new CriterionBandSeed("BAC5",
                "Kiểm soát ngữ pháp nhất quán ở cấu trúc phức tạp, kể cả khi nói về ý trừu tượng.",
                List.of(
                    sig("GRA_ADVANCED_STRUCTURE", "Dùng đảo ngữ, giả định, bị động đúng mục đích", HIGH, "phân tích cú pháp transcript"),
                    sig("GRA_SELF_CORRECTION", "Sửa lỗi kín đáo, không làm gãy mạch nói", HIGH, "vị trí sửa lỗi trên transcript")
                ),
                List.of(
                    sig("GRA_ERROR_RATE", "Trên 10% mệnh đề có lỗi ngữ pháp", HIGH, "số mệnh đề lỗi trên tổng mệnh đề"),
                    sig("GRA_BLOCKING_ERROR", "Có bất kỳ lỗi nào khiến người nghe hiểu sai ý", HIGH, "lỗi được gắn cờ cản trở giao tiếp")
                )),
            new CriterionBandSeed("BAC6",
                "Ngữ pháp chính xác gần như tuyệt đối, kể cả trong lời nói tự phát dài.",
                List.of(
                    sig("GRA_ADVANCED_STRUCTURE", "Cấu trúc phức tạp dùng nhuần nhuyễn và có chủ đích", HIGH, "phân tích cú pháp transcript"),
                    sig("GRA_CONSISTENT_ACCURACY", "Giữ độ chính xác ổn định suốt bài nói dài", HIGH, "phân bố lỗi theo mốc thời gian")
                ),
                List.of(
                    sig("GRA_ERROR_RATE", "Trên 5% mệnh đề có lỗi ngữ pháp", HIGH, "số mệnh đề lỗi trên tổng mệnh đề"),
                    sig("GRA_BLOCKING_ERROR", "Có bất kỳ lỗi nào khiến người nghe hiểu sai ý", HIGH, "lỗi được gắn cờ cản trở giao tiếp")
                ))
        )),

        new CriterionSeed("COHERENCE", "Mạch lạc và liên kết ý",
            "Bố cục câu trả lời, liên kết ý và mức độ bám sát yêu cầu đề.", 5, List.of(
            new CriterionBandSeed("BAC1",
                "Ý rời rạc, chủ yếu là liệt kê từng câu độc lập.",
                List.of(
                    sig("DIS_ON_TOPIC", "Nội dung nói có liên quan tới đề bài", LOW, "đối chiếu từ khoá đề bài"),
                    sig("DIS_MIN_CONTENT", "Nêu được ít nhất một ý cụ thể", LOW, "trích ý chính từ transcript")
                ),
                List.of(
                    sig("DIS_OFF_TOPIC", "Trên 50% nội dung lạc khỏi yêu cầu đề", LOW, "tỉ lệ câu không khớp chủ đề"),
                    sig("DIS_NO_STRUCTURE", "Không nhận ra được mở đầu hay kết thúc ý", LOW, "phát hiện ranh giới đoạn")
                )),
            new CriterionBandSeed("BAC2",
                "Có chuỗi ý đơn giản nối bằng liên từ cơ bản.",
                List.of(
                    sig("DIS_SEQUENCE", "Sắp xếp ý theo trình tự dễ theo dõi", LOW, "thứ tự ý trên transcript"),
                    sig("DIS_MIN_CONTENT", "Nêu được từ hai ý cụ thể trở lên", LOW, "trích ý chính từ transcript")
                ),
                List.of(
                    sig("DIS_OFF_TOPIC", "Trên 40% nội dung lạc khỏi yêu cầu đề", MEDIUM, "tỉ lệ câu không khớp chủ đề"),
                    sig("DIS_NO_STRUCTURE", "Không có câu mở hoặc câu chốt ý", LOW, "phát hiện ranh giới đoạn")
                )),
            new CriterionBandSeed("BAC3",
                "Câu trả lời có bố cục nhận ra được, ý chính kèm được lý do hoặc ví dụ.",
                List.of(
                    sig("DIS_SUPPORTED_POINT", "Mỗi ý chính có ít nhất một lý do hoặc ví dụ", MEDIUM, "cặp ý chính và ý hỗ trợ trên transcript"),
                    sig("DIS_COHESION", "Dùng từ nối để dẫn dắt giữa các ý", MEDIUM, "danh sách từ nối trên transcript")
                ),
                List.of(
                    sig("DIS_OFF_TOPIC", "Trên 30% nội dung lạc khỏi yêu cầu đề", MEDIUM, "tỉ lệ câu không khớp chủ đề"),
                    sig("DIS_MISSING_PROMPT_PART", "Bỏ sót một phần yêu cầu của đề", MEDIUM, "đối chiếu từng vế yêu cầu trong đề")
                )),
            new CriterionBandSeed("BAC4",
                "Lập luận rõ ràng, phát triển ý có chiều sâu và liên kết chặt.",
                List.of(
                    sig("DIS_ARGUMENT_DEVELOPMENT", "Phát triển ý qua nhiều bước chứ không dừng ở nêu ý", MEDIUM, "độ sâu chuỗi lập luận"),
                    sig("DIS_COHESION", "Liên kết mạch lạc, không lặp một từ nối duy nhất", MEDIUM, "phân bố từ nối trên transcript")
                ),
                List.of(
                    sig("DIS_OFF_TOPIC", "Trên 20% nội dung lạc khỏi yêu cầu đề", HIGH, "tỉ lệ câu không khớp chủ đề"),
                    sig("DIS_MISSING_PROMPT_PART", "Bỏ sót một phần yêu cầu của đề", HIGH, "đối chiếu từng vế yêu cầu trong đề")
                )),
            new CriterionBandSeed("BAC5",
                "Bố cục chặt chẽ, có định hướng người nghe và xử lý được ý phản biện.",
                List.of(
                    sig("DIS_SIGNPOSTING", "Định hướng người nghe bằng câu dẫn và câu chuyển", HIGH, "phát hiện câu dẫn hướng"),
                    sig("DIS_COUNTER_ARGUMENT", "Nêu và phản hồi được quan điểm đối lập", HIGH, "phát hiện cặp lập luận đối lập")
                ),
                List.of(
                    sig("DIS_OFF_TOPIC", "Trên 12% nội dung lạc khỏi yêu cầu đề", HIGH, "tỉ lệ câu không khớp chủ đề"),
                    sig("DIS_UNBALANCED", "Một phần yêu cầu chiếm dưới 20% thời lượng so với phần còn lại", MEDIUM, "phân bổ thời lượng theo từng vế yêu cầu")
                )),
            new CriterionBandSeed("BAC6",
                "Bài nói được tổ chức tinh tế, mạch lạc tự nhiên tới mức người nghe không nhận ra cấu trúc.",
                List.of(
                    sig("DIS_SEAMLESS_STRUCTURE", "Cấu trúc chặt nhưng không lộ khuôn mẫu", HIGH, "phân bố câu dẫn hướng"),
                    sig("DIS_RHETORICAL_CONTROL", "Điều chỉnh trọng tâm lập luận theo mục đích giao tiếp", HIGH, "đối chiếu ý đồ với yêu cầu đề")
                ),
                List.of(
                    sig("DIS_OFF_TOPIC", "Trên 6% nội dung lạc khỏi yêu cầu đề", HIGH, "tỉ lệ câu không khớp chủ đề"),
                    sig("DIS_UNBALANCED", "Một phần yêu cầu chiếm dưới 30% thời lượng so với phần còn lại", HIGH, "phân bổ thời lượng theo từng vế yêu cầu")
                ))
        ))
    );

    // ---------------------------------------------------------------------------------
    // Khung chấm điểm của trường (rubric) — thang 0..10
    // ---------------------------------------------------------------------------------

    /** Trọng số cộng lại đúng 1.00; đây là trọng số gộp 5 tiêu chí thành điểm một câu. */
    private static final List<RubricCriterionSeed> RUBRIC_CRITERIA = List.of(
        new RubricCriterionSeed("PRONUNCIATION", "Phát âm và trọng âm", "0.25", 1,
            "I go to school by bike every morning.", "Phát âm rõ, có phụ âm cuối, trọng âm từ đúng.", "8.0"),
        new RubricCriterionSeed("FLUENCY", "Độ trôi chảy", "0.25", 2,
            "Well, I think... I think the main reason is that we have more free time.", "Có ngập ngừng nhưng vẫn giữ được mạch nói.", "6.5"),
        new RubricCriterionSeed("VOCABULARY", "Từ vựng", "0.20", 3,
            "The festival attracts a huge crowd of visitors every spring.", "Dùng cụm kết hợp tự nhiên, không lặp từ.", "7.5"),
        new RubricCriterionSeed("GRAMMAR", "Ngữ pháp", "0.15", 4,
            "If I had more time, I would join the debate club.", "Câu điều kiện loại hai dùng đúng và tự nhiên.", "8.5"),
        new RubricCriterionSeed("COHERENCE", "Mạch lạc và liên kết ý", "0.15", 5,
            "There are two reasons. First, ... Second, ... So overall I believe ...", "Bố cục rõ, có câu chốt ý.", "8.0")
    );

    /**
     * Dải điểm phân loại trên thang 0..10.
     *
     * <p>Không được chồng lấn: {@code ExamSessionResultCalculator.resolveRubricResultBand}
     * lấy dải ĐẦU TIÊN theo {@code order} chứa tổng điểm.
     */
    private static final List<RubricBandSeed> RUBRIC_RESULT_BANDS = List.of(
        new RubricBandSeed("KEM", "Kém", "Chưa đáp ứng yêu cầu tối thiểu của bậc mục tiêu.", "0.00", "3.99", 1),
        new RubricBandSeed("YEU", "Yếu", "Còn cách bậc mục tiêu một khoảng rõ rệt.", "4.00", "5.49", 2),
        new RubricBandSeed("TB", "Trung bình", "Tiệm cận bậc mục tiêu, còn lỗi hệ thống.", "5.50", "6.99", 3),
        new RubricBandSeed("KHA", "Khá", "Đạt bậc mục tiêu ở phần lớn tiêu chí.", "7.00", "8.49", 4),
        new RubricBandSeed("TOT", "Tốt", "Đạt và vượt bậc mục tiêu một cách ổn định.", "8.50", "10.00", 5)
    );

    // ---------------------------------------------------------------------------------
    // Ba khối, ba bậc mục tiêu
    // ---------------------------------------------------------------------------------

    /**
     * Bậc mục tiêu tăng dần theo khối. Đây là chỗ duy nhất cần sửa nếu trường muốn
     * đổi kỳ vọng đầu ra của một khối.
     */
    private static final List<GradeLevelSeed> GRADE_LEVELS = List.of(
        new GradeLevelSeed("K10", "Khối 10", 1, 2010, "BAC3", AssessmentPolicyStrictness.LENIENT),
        new GradeLevelSeed("K11", "Khối 11", 2, 2009, "BAC4", AssessmentPolicyStrictness.STANDARD),
        new GradeLevelSeed("K12", "Khối 12", 3, 2008, "BAC5", AssessmentPolicyStrictness.STRICT)
    );

    /**
     * Luật chấm theo khối — ngưỡng siết dần cùng bậc mục tiêu.
     *
     * <p>Chưa có engine nào đọc bảng này khi chấm (xem javadoc của lớp). Seed ở đây để
     * dữ liệu sẵn sàng và để thấy rõ ý đồ: cùng một điều kiện, khối trên bị bắt lỗi sớm hơn.
     */
    private static final Map<String, List<ScoringRuleSeed>> SCORING_RULES_BY_GRADE_LEVEL = Map.of(
        "K10", List.of(
            new ScoringRuleSeed("TOO_SHORT", "Trả lời quá ngắn",
                ScoringRuleConditionType.DURATION_LESS_THAN, new DurationThresholdParams(15),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("4.00")),
                10, ScoringRuleSeverity.BLOCKING, true),
            new ScoringRuleSeed("THIN_CONTENT", "Nội dung quá mỏng",
                ScoringRuleConditionType.WORD_COUNT_LESS_THAN, new WordCountThresholdParams(30),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("5.50")),
                20, ScoringRuleSeverity.WARNING, false),
            new ScoringRuleSeed("OFF_TOPIC", "Lạc đề",
                ScoringRuleConditionType.OFF_TOPIC_RATIO_GREATER_THAN, new RatioThresholdParams(new BigDecimal("0.50")),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("3.00")),
                30, ScoringRuleSeverity.BLOCKING, false),
            new ScoringRuleSeed("CODE_SWITCH", "Chuyển mã sang tiếng Việt",
                ScoringRuleConditionType.CODE_SWITCHING_RATIO_GREATER_THAN, new RatioThresholdParams(new BigDecimal("0.30")),
                ScoringRuleActionType.CAP_CRITERION_SCORE, new CapCriterionScoreParams("VOCABULARY", new BigDecimal("5.00")),
                40, ScoringRuleSeverity.WARNING, false),
            new ScoringRuleSeed("LOW_CONFIDENCE", "Độ tin cậy chấm thấp",
                ScoringRuleConditionType.AI_CONFIDENCE_LESS_THAN, new ConfidenceThresholdParams(new BigDecimal("0.70")),
                ScoringRuleActionType.REQUIRE_HUMAN_REVIEW, new RequireHumanReviewParams("LOW_AI_CONFIDENCE"),
                50, ScoringRuleSeverity.INFO, false)
        ),
        "K11", List.of(
            new ScoringRuleSeed("TOO_SHORT", "Trả lời quá ngắn",
                ScoringRuleConditionType.DURATION_LESS_THAN, new DurationThresholdParams(20),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("4.00")),
                10, ScoringRuleSeverity.BLOCKING, true),
            new ScoringRuleSeed("THIN_CONTENT", "Nội dung quá mỏng",
                ScoringRuleConditionType.WORD_COUNT_LESS_THAN, new WordCountThresholdParams(45),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("5.50")),
                20, ScoringRuleSeverity.WARNING, false),
            new ScoringRuleSeed("OFF_TOPIC", "Lạc đề",
                ScoringRuleConditionType.OFF_TOPIC_RATIO_GREATER_THAN, new RatioThresholdParams(new BigDecimal("0.40")),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("3.00")),
                30, ScoringRuleSeverity.BLOCKING, false),
            new ScoringRuleSeed("CODE_SWITCH", "Chuyển mã sang tiếng Việt",
                ScoringRuleConditionType.CODE_SWITCHING_RATIO_GREATER_THAN, new RatioThresholdParams(new BigDecimal("0.20")),
                ScoringRuleActionType.CAP_CRITERION_SCORE, new CapCriterionScoreParams("VOCABULARY", new BigDecimal("4.50")),
                40, ScoringRuleSeverity.WARNING, false),
            new ScoringRuleSeed("LOW_CONFIDENCE", "Độ tin cậy chấm thấp",
                ScoringRuleConditionType.AI_CONFIDENCE_LESS_THAN, new ConfidenceThresholdParams(new BigDecimal("0.75")),
                ScoringRuleActionType.REQUIRE_HUMAN_REVIEW, new RequireHumanReviewParams("LOW_AI_CONFIDENCE"),
                50, ScoringRuleSeverity.INFO, false)
        ),
        "K12", List.of(
            new ScoringRuleSeed("TOO_SHORT", "Trả lời quá ngắn",
                ScoringRuleConditionType.DURATION_LESS_THAN, new DurationThresholdParams(25),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("4.00")),
                10, ScoringRuleSeverity.BLOCKING, true),
            new ScoringRuleSeed("THIN_CONTENT", "Nội dung quá mỏng",
                ScoringRuleConditionType.WORD_COUNT_LESS_THAN, new WordCountThresholdParams(60),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("5.50")),
                20, ScoringRuleSeverity.WARNING, false),
            new ScoringRuleSeed("OFF_TOPIC", "Lạc đề",
                ScoringRuleConditionType.OFF_TOPIC_RATIO_GREATER_THAN, new RatioThresholdParams(new BigDecimal("0.30")),
                ScoringRuleActionType.CAP_FINAL_SCORE, new CapFinalScoreParams(new BigDecimal("3.00")),
                30, ScoringRuleSeverity.BLOCKING, false),
            new ScoringRuleSeed("CODE_SWITCH", "Chuyển mã sang tiếng Việt",
                ScoringRuleConditionType.CODE_SWITCHING_RATIO_GREATER_THAN, new RatioThresholdParams(new BigDecimal("0.10")),
                ScoringRuleActionType.CAP_CRITERION_SCORE, new CapCriterionScoreParams("VOCABULARY", new BigDecimal("4.00")),
                40, ScoringRuleSeverity.WARNING, false),
            new ScoringRuleSeed("LOW_CONFIDENCE", "Độ tin cậy chấm thấp",
                ScoringRuleConditionType.AI_CONFIDENCE_LESS_THAN, new ConfidenceThresholdParams(new BigDecimal("0.80")),
                ScoringRuleActionType.REQUIRE_HUMAN_REVIEW, new RequireHumanReviewParams("LOW_AI_CONFIDENCE"),
                50, ScoringRuleSeverity.INFO, false)
        )
    );

    // ---------------------------------------------------------------------------------
    // Nhân sự và phòng thi
    // ---------------------------------------------------------------------------------

    private static final MemberSeed SCHOOL_ADMIN = new MemberSeed(
        "admin.sample@vox.edu.vn", "0900000001", "Nguyen Thi Quan Tri",
        Gender.FEMALE, LocalDate.of(1982, 5, 20));

    private static final List<MemberSeed> TEACHERS = List.of(
        new MemberSeed("teacher1.sample@vox.edu.vn", "0900000011", "Nguyen Van Minh", Gender.MALE, LocalDate.of(1988, 3, 12)),
        new MemberSeed("teacher2.sample@vox.edu.vn", "0900000012", "Tran Thi Lan", Gender.FEMALE, LocalDate.of(1990, 7, 8)),
        new MemberSeed("teacher3.sample@vox.edu.vn", "0900000013", "Le Hoang Nam", Gender.MALE, LocalDate.of(1986, 11, 2)),
        new MemberSeed("teacher4.sample@vox.edu.vn", "0900000014", "Pham Thu Ha", Gender.FEMALE, LocalDate.of(1992, 1, 25)),
        new MemberSeed("teacher5.sample@vox.edu.vn", "0900000015", "Vo Minh Duc", Gender.MALE, LocalDate.of(1989, 9, 17)),
        new MemberSeed("teacher6.sample@vox.edu.vn", "0900000016", "Dang Thi Mai", Gender.FEMALE, LocalDate.of(1991, 4, 30))
    );

    private static final List<RoomSeed> ROOMS = List.of(
        new RoomSeed("P101", "Phòng thi nói 101", "Phòng lab 20 máy, có tai nghe chống ồn"),
        new RoomSeed("P102", "Phòng thi nói 102", "Phòng lab 20 máy, có tai nghe chống ồn"),
        new RoomSeed("P201", "Phòng thi nói 201", "Phòng lab 16 máy"),
        new RoomSeed("P202", "Phòng thi nói 202", "Phòng dự phòng cho thí sinh thi bù")
    );

    /** Họ tên học sinh sinh theo vòng lặp từ bảng này, tránh 60 record hardcode. */
    private static final List<String> STUDENT_SURNAMES = List.of(
        "Nguyen", "Tran", "Le", "Pham", "Hoang", "Vo", "Dang", "Bui", "Do", "Ngo");
    private static final List<String> STUDENT_GIVEN_NAMES = List.of(
        "An", "Binh", "Chi", "Dung", "Giang", "Hieu", "Khanh", "Linh", "Minh", "Ngoc");

    // ---------------------------------------------------------------------------------
    // Ngân hàng câu hỏi — 8 câu mỗi khối, chia đều cho 2 mã đề
    // ---------------------------------------------------------------------------------

    private static final Map<String, List<QuestionSeed>> QUESTIONS_BY_GRADE_LEVEL = Map.of(
        "K10", List.of(
            new QuestionSeed(QuestionType.SHORT_ANSWER, "Introduce yourself and the class you are studying in.",
                "Nói về tên, lớp, và một môn học em thích nhất.",
                "Giới thiệu bản thân có nêu lớp và môn học yêu thích.", "Tên; lớp; môn học thích; lý do ngắn"),
            new QuestionSeed(QuestionType.DESCRIPTION, "Describe your daily routine on a school day.",
                "Kể lại một ngày đi học bình thường của em.",
                "Mô tả trình tự các hoạt động trong ngày.", "Buổi sáng; giờ học; buổi chiều; buổi tối"),
            new QuestionSeed(QuestionType.SHORT_ANSWER, "Talk about a friend you spend the most time with.",
                "Nói về người bạn em hay gặp nhất và lý do.",
                "Giới thiệu một người bạn kèm lý do thân thiết.", "Tên bạn; tính cách; hoạt động chung"),
            new QuestionSeed(QuestionType.OPINION, "Do you prefer studying at home or at the library? Why?",
                "Nêu ý kiến của em và giải thích bằng ít nhất hai lý do.",
                "Nêu lựa chọn rõ ràng kèm hai lý do.", "Lựa chọn; lý do một; lý do hai"),
            new QuestionSeed(QuestionType.DESCRIPTION, "Describe a place in your neighbourhood you often visit.",
                "Mô tả địa điểm đó và nói vì sao em hay tới.",
                "Mô tả địa điểm quen thuộc kèm lý do.", "Tên địa điểm; đặc điểm; tần suất; lý do"),
            new QuestionSeed(QuestionType.SHORT_ANSWER, "Talk about a subject you find difficult at school.",
                "Nói về một môn học khó và cách em đang cố gắng.",
                "Nêu môn học khó kèm cách khắc phục.", "Tên môn; khó ở đâu; cách học"),
            new QuestionSeed(QuestionType.OPINION, "Should students wear uniforms at school? Why or why not?",
                "Nêu quan điểm của em và giải thích.",
                "Nêu quan điểm kèm ít nhất hai lý do.", "Quan điểm; lý do một; lý do hai"),
            new QuestionSeed(QuestionType.DESCRIPTION, "Describe a school activity you joined this year.",
                "Kể về hoạt động đó và cảm nghĩ của em.",
                "Kể lại hoạt động kèm cảm nhận.", "Hoạt động gì; khi nào; vai trò; cảm nghĩ")
        ),
        "K11", List.of(
            new QuestionSeed(QuestionType.OPINION, "Some people think social media harms teenagers. What is your view?",
                "Nêu quan điểm và bảo vệ bằng lập luận cụ thể.",
                "Nêu quan điểm rõ ràng, có lập luận và ví dụ.", "Quan điểm; lập luận chính; ví dụ; ý phản biện"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Describe a challenge you faced and how you dealt with it.",
                "Kể lại tình huống, cách xử lý và bài học rút ra.",
                "Kể có trình tự và rút ra bài học.", "Bối cảnh; khó khăn; cách xử lý; bài học"),
            new QuestionSeed(QuestionType.OPINION, "Is it better to learn a language online or in a classroom?",
                "So sánh hai hình thức và nêu lựa chọn của em.",
                "So sánh hai lựa chọn rồi chốt quan điểm.", "Ưu điểm A; ưu điểm B; lựa chọn; lý do"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Talk about a person who has influenced your study habits.",
                "Nói về người đó và ảnh hưởng cụ thể tới em.",
                "Nêu nhân vật và ảnh hưởng cụ thể, có ví dụ.", "Người đó là ai; ảnh hưởng gì; ví dụ; kết quả"),
            new QuestionSeed(QuestionType.OPINION, "Should schools replace paper exams with computer-based exams?",
                "Nêu quan điểm và cân nhắc mặt trái.",
                "Nêu quan điểm và thừa nhận mặt hạn chế.", "Quan điểm; lợi ích; hạn chế; kết luận"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Describe a trip or an event that changed how you think.",
                "Kể lại và giải thích thay đổi trong suy nghĩ.",
                "Kể có trình tự và nêu được sự thay đổi.", "Sự kiện; diễn biến; suy nghĩ trước; suy nghĩ sau"),
            new QuestionSeed(QuestionType.OPINION, "Do part-time jobs help or distract high school students?",
                "Nêu quan điểm và đưa ví dụ cụ thể.",
                "Nêu quan điểm kèm ví dụ thực tế.", "Quan điểm; lý do; ví dụ; giới hạn của quan điểm"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Talk about a skill you want to master in the next two years.",
                "Nói về kỹ năng đó và kế hoạch cụ thể.",
                "Nêu kỹ năng và kế hoạch có mốc thời gian.", "Kỹ năng; lý do; kế hoạch; mốc thời gian")
        ),
        "K12", List.of(
            new QuestionSeed(QuestionType.OPINION, "To what extent should artificial intelligence be used in education?",
                "Nêu lập trường, phân tích cả hai chiều và bảo vệ quan điểm.",
                "Lập luận hai chiều, có phản biện và kết luận.", "Lập trường; luận điểm; phản biện; phản hồi phản biện"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Discuss how your generation views success differently from your parents'.",
                "So sánh hai thế hệ và giải thích nguyên nhân.",
                "So sánh có chiều sâu, giải thích nguyên nhân.", "Quan niệm thế hệ trước; thế hệ này; nguyên nhân; đánh giá"),
            new QuestionSeed(QuestionType.OPINION, "Is economic growth worth its environmental cost? Defend your position.",
                "Bảo vệ lập trường bằng lập luận có cấu trúc.",
                "Bảo vệ lập trường bằng chuỗi lập luận.", "Lập trường; luận điểm chính; dữ liệu/ví dụ; nhượng bộ"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Explain a social issue in your community and propose a solution.",
                "Phân tích vấn đề và đề xuất giải pháp khả thi.",
                "Phân tích vấn đề và đề xuất giải pháp có tính khả thi.", "Vấn đề; nguyên nhân; giải pháp; tính khả thi"),
            new QuestionSeed(QuestionType.OPINION, "Should university education be free for everyone? Why?",
                "Nêu lập trường và xử lý ý kiến trái chiều.",
                "Lập trường rõ và phản hồi được ý kiến trái chiều.", "Lập trường; lý do; ý kiến trái chiều; phản hồi"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Describe a decision you regret and what you would do differently.",
                "Phân tích quyết định đó và rút ra nguyên tắc cho bản thân.",
                "Phân tích quyết định và rút ra nguyên tắc.", "Quyết định; bối cảnh; hệ quả; nguyên tắc rút ra"),
            new QuestionSeed(QuestionType.OPINION, "Does globalisation threaten local cultures? Argue your case.",
                "Lập luận có dẫn chứng và thừa nhận giới hạn quan điểm.",
                "Lập luận có dẫn chứng và thừa nhận giới hạn.", "Lập trường; dẫn chứng; giới hạn; kết luận"),
            new QuestionSeed(QuestionType.LONG_ANSWER, "Discuss what role your generation should play in solving climate change.",
                "Trình bày vai trò cụ thể và biện minh cho lựa chọn đó.",
                "Nêu vai trò cụ thể và biện minh.", "Vai trò; lý do; hành động cụ thể; kỳ vọng kết quả")
        )
    );

    private final SampleIdentityRepositories identity;
    private final SampleStructureRepositories structure;
    private final SampleAssessmentRepositories assessment;
    private final SampleQuestionRepositories questions;
    private final SampleExamRepositories exams;
    private final SampleSubscriptionRepositories subscriptions;

    @Value("${sample-data.password:Password@123}")
    private String password;

    public SampleSchoolDataInitializer(
            SampleIdentityRepositories identity,
            SampleStructureRepositories structure,
            SampleAssessmentRepositories assessment,
            SampleQuestionRepositories questions,
            SampleExamRepositories exams,
            SampleSubscriptionRepositories subscriptions) {
        this.identity = identity;
        this.structure = structure;
        this.assessment = assessment;
        this.questions = questions;
        this.exams = exams;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var now = Instant.now();
        var roleIds = new RoleIds(
            requireRoleId(SCHOOL_ADMIN_ROLE_CODE),
            requireRoleId(TEACHER_ROLE_CODE),
            requireRoleId(STUDENT_ROLE_CODE)
        );
        var language = identity.supportedLanguageRepository().findByCode(ENGLISH_CODE)
            .orElseThrow(() -> new IllegalStateException(
                "Thiếu ngôn ngữ " + ENGLISH_CODE + ", SupportedLanguageInitializer phải chạy trước"));

        var auditUserId = resolveSystemAdminId();
        var school = findOrCreateSchool(auditUserId, now);

        // Cơ cấu tổ chức là mốc nhận biết đã seed: có khối là coi như toàn bộ phần mở rộng
        // đã chạy, nên chạy lại ứng dụng không sinh trùng kỳ thi/lịch thi.
        if (structure.schoolGradeLevelRepository().existsBySchoolIdAndCode(school.getId(), GRADE_LEVELS.getFirst().code())) {
            LOGGER.info("Sample school {} đã được seed đầy đủ, bỏ qua", SAMPLE_SCHOOL_CODE);
            return;
        }

        var passwordHash = identity.passwordEncoderPort().hash(password);
        var schoolAdminId = ensureMember(SCHOOL_ADMIN, school.getId(), roleIds.schoolAdmin(), passwordHash, auditUserId, now, null);
        var teacherIds = TEACHERS.stream()
            .map(teacher -> ensureMember(teacher, school.getId(), roleIds.teacher(), passwordHash, auditUserId, now, null))
            .toList();

        seedSubscription(school.getId(), schoolAdminId, now);
        var rooms = seedRooms(school.getId(), schoolAdminId, now);
        var frameworkVersion = seedFramework(schoolAdminId, now);
        var rubricVersion = seedRubric(school.getId(), language, frameworkVersion, schoolAdminId, now);
        var questionBank = seedQuestionBank(school.getId(), language, schoolAdminId, now);

        var scheduledExams = 0;
        var seededStudents = 0;
        for (var gradeLevelSeed : GRADE_LEVELS) {
            var gradeLevel = seedGradeLevel(school.getId(), gradeLevelSeed, schoolAdminId, now);
            var grade = seedGrade(gradeLevel.getId(), gradeLevelSeed, schoolAdminId, now);
            var studentIds = seedClassesAndStudents(
                school.getId(), language.getId(), grade.getId(), gradeLevelSeed,
                roleIds.student(), passwordHash, schoolAdminId, now);
            seededStudents += studentIds.size();

            var policy = seedAssessmentPolicy(
                school.getId(), gradeLevel.getId(), grade.getId(), language.getId(),
                frameworkVersion, rubricVersion, gradeLevelSeed, schoolAdminId, now);
            seedScoringRules(policy.getId(), gradeLevelSeed, schoolAdminId, now);

            var questionIds = seedQuestions(questionBank.getId(), gradeLevelSeed, teacherIds, now);
            seedScheduledExam(
                school.getId(), language.getId(), gradeLevel.getId(), gradeLevelSeed,
                policy.getId(), questionIds, studentIds, teacherIds, rooms, schoolAdminId, now);
            scheduledExams++;
        }

        LOGGER.info(
            "Đã seed trường {}: {} khối, {} học sinh, {} giáo viên, {} phòng, {} kỳ thi ở trạng thái SCHEDULED",
            SAMPLE_SCHOOL_CODE, GRADE_LEVELS.size(), seededStudents, TEACHERS.size(), rooms.size(), scheduledExams
        );
    }

    // ---------------------------------------------------------------------------------
    // Trường, nhân sự, phòng
    // ---------------------------------------------------------------------------------

    private School findOrCreateSchool(UUID auditUserId, Instant now) {
        return identity.schoolRepository().findByCode(SAMPLE_SCHOOL_CODE)
            .orElseGet(() -> identity.schoolRepository().save(School.create(
                SAMPLE_SCHOOL_CODE,
                "Trường THPT Mẫu Vox",
                "Trường mẫu dùng cho môi trường phát triển",
                SCHOOL_ADMIN.phone(),
                "contact.sample@vox.edu.vn",
                SAMPLE_SCHOOL_DOMAIN,
                "12 Đường Mẫu, Quận Cầu Giấy, Hà Nội",
                GRADE_LEVELS.size() * CLASSES_PER_GRADE_LEVEL * STUDENTS_PER_CLASS,
                auditUserId,
                now
            )));
    }

    private UUID ensureMember(
            MemberSeed seed,
            UUID schoolId,
            UUID roleId,
            String passwordHash,
            UUID auditUserId,
            Instant now,
            Integer membershipYears) {
        var user = identity.userRepository().findByEmail(seed.email())
            .orElseGet(() -> createUser(seed, passwordHash, auditUserId, now));
        ensureRole(user.getId(), roleId, now);
        ensureSchoolMembership(user.getId(), schoolId, now, membershipYears);
        return user.getId();
    }

    private User createUser(MemberSeed seed, String passwordHash, UUID auditUserId, Instant now) {
        if (identity.userRepository().existsByPhone(seed.phone())) {
            throw new IllegalStateException("Số điện thoại đã được dùng, không thể seed: " + seed.phone());
        }
        return identity.userRepository().save(new User(
            new Email(seed.email()),
            passwordHash,
            new Phone(seed.phone()),
            new FullName(seed.fullName()),
            seed.gender(),
            new DateOfBirth(seed.dateOfBirth()),
            "Hà Nội",
            null,
            UserStatus.ACTIVE,
            now,
            now,
            auditUserId,
            auditUserId
        ));
    }

    private void ensureRole(UUID userId, UUID roleId, Instant now) {
        if (identity.userRoleRepository().findByUserIdAndRoleId(userId, roleId).isEmpty()) {
            identity.userRoleRepository().save(new UserRole(userId, roleId, now));
        }
    }

    private void ensureSchoolMembership(UUID userId, UUID schoolId, Instant now, Integer membershipYears) {
        var existing = identity.schoolUserRepository().findByUserId(userId);
        if (existing.isPresent()) {
            if (!existing.get().getSchoolId().equals(schoolId)) {
                throw new IllegalStateException("Tài khoản đã thuộc trường khác, không thể seed: " + userId);
            }
            return;
        }
        // Instant.plus không nhận đơn vị YEARS/MONTHS (không phải khoảng thời gian chính xác nếu
        // không có lịch), nên phải quy về múi giờ trước khi cộng theo năm.
        var endDate = membershipYears == null
            ? null
            : now.atZone(DateMapper.DEFAULT_INPUT_ZONE).plusYears(membershipYears).toInstant();
        identity.schoolUserRepository().save(SchoolUser.create(userId, schoolId, now, endDate));
    }

    private List<SchoolRoom> seedRooms(UUID schoolId, UUID createdBy, Instant now) {
        return ROOMS.stream()
            .map(room -> structure.schoolRoomRepository().save(new SchoolRoom(
                schoolId, room.code(), room.name(), room.description(), true, now, now, createdBy, createdBy)))
            .toList();
    }

    // ---------------------------------------------------------------------------------
    // Khối, năm học, lớp, học sinh
    // ---------------------------------------------------------------------------------

    private SchoolGradeLevel seedGradeLevel(UUID schoolId, GradeLevelSeed seed, UUID createdBy, Instant now) {
        return structure.schoolGradeLevelRepository().save(new SchoolGradeLevel(
            schoolId,
            seed.code(),
            seed.name(),
            seed.name() + " - chương trình tiếng Anh, bậc mục tiêu " + seed.targetBandCode(),
            seed.order(),
            SchoolGradeLevelStatus.ACTIVE,
            now, now, createdBy, createdBy
        ));
    }

    private SchoolGrade seedGrade(UUID gradeLevelId, GradeLevelSeed seed, UUID createdBy, Instant now) {
        return structure.schoolGradeRepository().save(new SchoolGrade(
            gradeLevelId,
            seed.code() + "-" + ACADEMIC_YEAR,
            seed.name() + " năm học " + ACADEMIC_YEAR,
            "Niên khoá " + ACADEMIC_YEAR + " của " + seed.name(),
            LocalDate.of(2025, 9, 5),
            LocalDate.of(2026, 5, 31),
            SchoolGradeStatus.ACTIVE,
            now, now, createdBy, createdBy
        ));
    }

    private List<UUID> seedClassesAndStudents(
            UUID schoolId,
            UUID languageId,
            UUID gradeId,
            GradeLevelSeed gradeLevelSeed,
            UUID studentRoleId,
            String passwordHash,
            UUID createdBy,
            Instant now) {
        var studentIds = new ArrayList<UUID>();
        for (var classIndex = 0; classIndex < CLASSES_PER_GRADE_LEVEL; classIndex++) {
            var classSuffix = (char) ('A' + classIndex);
            var classCode = gradeLevelSeed.classCodePrefix() + classSuffix + "1";
            var schoolClass = structure.schoolClassRepository().save(SchoolClass.create(
                schoolId,
                languageId,
                gradeId,
                classCode,
                "Lớp " + gradeLevelSeed.classCodePrefix() + classSuffix + "1",
                "Lớp tiếng Anh của " + gradeLevelSeed.name(),
                createdBy,
                now
            ));

            for (var seat = 1; seat <= STUDENTS_PER_CLASS; seat++) {
                var seed = studentSeed(gradeLevelSeed, classIndex, seat);
                var studentId = ensureMember(seed, schoolId, studentRoleId, passwordHash, createdBy, now, 3);
                structure.schoolClassUserRepository().save(
                    new SchoolClassUser(studentId, schoolClass.getId(), true, now, null, createdBy));
                studentIds.add(studentId);
            }
        }
        return studentIds;
    }

    /**
     * Sinh học sinh theo vị trí trong khối/lớp/chỗ ngồi. Số điện thoại phải khớp
     * {@code Phone}: 0 + đầu số + 8 chữ số, nên dùng tiền tố cố định {@code 09} rồi
     * nhồi 8 chữ số từ chỉ số tuyệt đối của học sinh.
     */
    private MemberSeed studentSeed(GradeLevelSeed gradeLevelSeed, int classIndex, int seat) {
        var classSuffix = (char) ('A' + classIndex);
        var slug = gradeLevelSeed.classCodePrefix().toLowerCase() + Character.toLowerCase(classSuffix) + String.format("%02d", seat);
        var absoluteIndex = gradeLevelSeed.order() * 1000 + classIndex * 100 + seat;
        var nameIndex = (classIndex * STUDENTS_PER_CLASS + seat - 1) % STUDENT_GIVEN_NAMES.size();
        var fullName = STUDENT_SURNAMES.get((absoluteIndex + nameIndex) % STUDENT_SURNAMES.size())
            + " Van " + STUDENT_GIVEN_NAMES.get(nameIndex);
        return new MemberSeed(
            "hs" + slug + ".sample@vox.edu.vn",
            "09" + String.format("%08d", absoluteIndex),
            fullName,
            seat % 2 == 0 ? Gender.FEMALE : Gender.MALE,
            LocalDate.of(gradeLevelSeed.birthYear(), 1 + (seat % 12), 1 + (seat % 27))
        );
    }

    // ---------------------------------------------------------------------------------
    // Gói dịch vụ
    // ---------------------------------------------------------------------------------

    /**
     * Hạn mức token phải đủ cho ước lượng worst-case của
     * {@code UpdateExamStatusUseCase.validatePlanLimits}: thời lượng bài thi (giây) ×
     * số thí sinh × maxAttempt, cộng dồn cho cả ba kỳ thi.
     */
    private void seedSubscription(UUID schoolId, UUID createdBy, Instant now) {
        var plan = subscriptions.subscriptionPlanRepository().save(new SubscriptionPlan(
            "Gói Trường THPT - Mẫu",
            "Gói seed cho môi trường phát triển",
            new BigDecimal("24000000"),
            365,
            60,
            500,
            true,
            PlanStatus.ACTIVE,
            1,
            now,
            createdBy
        ));

        // Dẫn xuất từ now thay vì gọi LocalDate.now(): LocalDate.now() đọc múi giờ của JVM, và hai
        // lần gọi riêng biệt còn có thể vắt qua nửa đêm nên ra hai ngày gốc khác nhau.
        var subscriptionStart = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var subscription = subscriptions.schoolSubscriptionRepository().save(new SchoolSubscription(
            schoolId,
            plan.getId(),
            subscriptionStart.minusDays(30),
            subscriptionStart.plusDays(335),
            SubscriptionStatus.ACTIVE,
            new BigDecimal("24000000"),
            null,
            now
        ));

        var candidatesPerExam = CLASSES_PER_GRADE_LEVEL * STUDENTS_PER_CLASS;
        var worstCasePerExam = EXAM_DURATION_SECONDS * candidatesPerExam;
        var allocated = worstCasePerExam * GRADE_LEVELS.size() * 4;

        subscriptions.subscriptionQuotaRepository()
            .save(new SubscriptionQuota(subscription.getId(), QuotaType.GRADING, allocated, 0));
        subscriptions.subscriptionQuotaRepository()
            .save(new SubscriptionQuota(subscription.getId(), QuotaType.CLASS_TEST, allocated / 2, 0));
        subscriptions.subscriptionQuotaRepository()
            .save(new SubscriptionQuota(subscription.getId(), QuotaType.PRACTICE, allocated / 2, 0));
    }

    // ---------------------------------------------------------------------------------
    // Khung năng lực
    // ---------------------------------------------------------------------------------

    private SeededFrameworkVersion seedFramework(UUID createdBy, Instant now) {
        var framework = assessment.frameworkRepository().findByCode(FRAMEWORK_CODE)
            .orElseGet(() -> assessment.frameworkRepository().save(new Framework(
                new FrameworkCode(FRAMEWORK_CODE),
                "Khung năng lực ngoại ngữ 6 bậc dùng cho Việt Nam",
                "Khung tham chiếu quốc gia, sáu bậc tương ứng A1 đến C2 của CEFR.",
                true, now, now, createdBy, createdBy
            )));

        var existingVersion = assessment.frameworkVersionRepository().findByCode(FRAMEWORK_VERSION_CODE);
        if (existingVersion.isPresent()) {
            return loadExistingFrameworkVersion(framework, existingVersion.get());
        }

        var version = assessment.frameworkVersionRepository().save(new FrameworkVersion(
            framework.getId(),
            FRAMEWORK_VERSION_CODE,
            "Khung năng lực ngoại ngữ 6 bậc - phiên bản 1",
            "Phiên bản seed cho kỹ năng nói, năm chỉ số đánh giá.",
            1,
            now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusMonths(1).toInstant(),
            null,
            FrameworkVersionStatus.PUBLISHED,
            now, now, createdBy, createdBy
        ));

        var bandIdsByCode = new LinkedHashMap<String, UUID>();
        for (var band : FRAMEWORK_BANDS) {
            var saved = assessment.frameworkResultBandRepository().save(new FrameworkResultBand(
                version.getId(), band.code(), band.label(), band.description(), band.order(),
                now, now, createdBy, createdBy
            ));
            bandIdsByCode.put(band.code(), saved.getId());
        }

        var criterionIdsByCode = new LinkedHashMap<String, UUID>();
        for (var criterion : FRAMEWORK_CRITERIA) {
            var savedCriterion = assessment.frameworkCriterionRepository().save(new FrameworkCriterion(
                version.getId(), criterion.code(), criterion.name(), criterion.description(), criterion.order(),
                now, now, createdBy, createdBy
            ));
            criterionIdsByCode.put(criterion.code(), savedCriterion.getId());

            for (var bandSeed : criterion.bands()) {
                var bandId = bandIdsByCode.get(bandSeed.bandCode());
                if (bandId == null) {
                    throw new IllegalStateException(
                        "Bậc " + bandSeed.bandCode() + " không có trong khung, kiểm tra lại FRAMEWORK_BANDS");
                }
                assessment.frameworkCriterionBandRepository().save(new FrameworkCriterionBand(
                    savedCriterion.getId(),
                    bandId,
                    bandSeed.descriptor(),
                    new FrameworkCriterionSignals(bandSeed.positiveSignals()),
                    new FrameworkCriterionSignals(bandSeed.negativeSignals()),
                    now, now, createdBy, createdBy
                ));
            }
        }

        return new SeededFrameworkVersion(framework.getId(), version.getId(), bandIdsByCode, criterionIdsByCode);
    }

    private SeededFrameworkVersion loadExistingFrameworkVersion(Framework framework, FrameworkVersion version) {
        if (!framework.getId().equals(version.getFrameworkId())) {
            throw new IllegalStateException(
                "Framework version " + FRAMEWORK_VERSION_CODE + " không thuộc framework " + FRAMEWORK_CODE);
        }

        var bandIdsByCode = new LinkedHashMap<String, UUID>();
        for (var band : assessment.frameworkResultBandRepository().findByFrameworkVersionId(version.getId())) {
            if (bandIdsByCode.putIfAbsent(band.getCode(), band.getId()) != null) {
                throw new IllegalStateException(
                    "Framework version " + FRAMEWORK_VERSION_CODE + " có mã bậc bị trùng: " + band.getCode());
            }
        }

        var criterionIdsByCode = new LinkedHashMap<String, UUID>();
        for (var criterion : assessment.frameworkCriterionRepository().findByFrameworkVersionId(version.getId())) {
            if (criterionIdsByCode.putIfAbsent(criterion.getCode(), criterion.getId()) != null) {
                throw new IllegalStateException(
                    "Framework version " + FRAMEWORK_VERSION_CODE + " có mã tiêu chí bị trùng: " + criterion.getCode());
            }
        }

        // Tương thích dữ liệu seed cũ trong khi dữ liệu mới dùng thống nhất COHERENCE.
        if (!criterionIdsByCode.containsKey("COHERENCE") && criterionIdsByCode.containsKey("DISCOURSE")) {
            criterionIdsByCode.put("COHERENCE", criterionIdsByCode.get("DISCOURSE"));
        }

        var missingBandCodes = FRAMEWORK_BANDS.stream()
            .map(seed -> seed.code())
            .filter(code -> !bandIdsByCode.containsKey(code))
            .toList();
        var missingCriterionCodes = FRAMEWORK_CRITERIA.stream()
            .map(seed -> seed.code())
            .filter(code -> !criterionIdsByCode.containsKey(code))
            .toList();
        var distinctCriterionIds = criterionIdsByCode.values().stream().distinct().toList();
        var criterionBandCount = assessment.frameworkCriterionBandRepository()
            .findByFrameworkCriterionIdIn(distinctCriterionIds)
            .size();
        var expectedCriterionBandCount = FRAMEWORK_BANDS.size() * FRAMEWORK_CRITERIA.size();

        if (!missingBandCodes.isEmpty() || !missingCriterionCodes.isEmpty()
                || criterionBandCount < expectedCriterionBandCount) {
            throw new IllegalStateException(
                "Framework version " + FRAMEWORK_VERSION_CODE
                    + " đã tồn tại nhưng chưa đầy đủ; không insert thêm dữ liệu trùng. Thiếu bậc="
                    + missingBandCodes + ", thiếu tiêu chí=" + missingCriterionCodes
                    + ", criterion bands=" + criterionBandCount + "/" + expectedCriterionBandCount);
        }

        LOGGER.info("Tái sử dụng framework version {} đã tồn tại", FRAMEWORK_VERSION_CODE);
        return new SeededFrameworkVersion(framework.getId(), version.getId(), bandIdsByCode, criterionIdsByCode);
    }

    // ---------------------------------------------------------------------------------
    // Khung chấm điểm của trường
    // ---------------------------------------------------------------------------------

    private UUID seedRubric(
            UUID schoolId,
            SupportedLanguage language,
            SeededFrameworkVersion frameworkVersion,
            UUID createdBy,
            Instant now) {
        var existingVersion = assessment.rubricVersionRepository().findByCode(RUBRIC_VERSION_CODE);
        if (existingVersion.isPresent()) {
            var version = existingVersion.get();
            var rubric = assessment.rubricRepository().findById(version.getRubricId())
                .orElseThrow(() -> new IllegalStateException(
                    "Rubric version " + RUBRIC_VERSION_CODE + " không còn rubric cha."));
            if (!schoolId.equals(rubric.getSchoolId())
                    || !frameworkVersion.frameworkId().equals(rubric.getFrameworkId())) {
                throw new IllegalStateException(
                    "Rubric version " + RUBRIC_VERSION_CODE + " đã được dùng bởi trường hoặc framework khác.");
            }
            LOGGER.info("Tái sử dụng rubric version {} đã tồn tại", RUBRIC_VERSION_CODE);
            return version.getId();
        }

        var rubric = assessment.rubricRepository().save(new Rubric(
            language.getId(),
            frameworkVersion.frameworkId(),
            RUBRIC_CODE,
            "Khung chấm nói - Trường THPT Mẫu Vox",
            "Ánh xạ năm tiêu chí của khung quốc gia sang thang điểm 0-10 của trường.",
            RubricOwnerType.SCHOOL,
            schoolId
        ));

        var version = assessment.rubricVersionRepository().save(new RubricVersion(
            rubric.getId(),
            1,
            RUBRIC_VERSION_CODE,
            "Khung chấm nói - phiên bản 1",
            "Thang 0-10, tổng điểm theo trung bình có trọng số.",
            RubricStatus.PUBLISHED,
            now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusMonths(1).toInstant(),
            null,
            new BigDecimal("0.00"),
            new BigDecimal("10.00"),
            RubricTotalScoreMethod.WEIGHTED_AVERAGE,
            now, now, createdBy, createdBy
        ));

        for (var criterion : RUBRIC_CRITERIA) {
            var frameworkCriterionId = frameworkVersion.criterionIdsByCode().get(criterion.code());
            if (frameworkCriterionId == null) {
                throw new IllegalStateException(
                    "Tiêu chí rubric " + criterion.code() + " không ánh xạ được sang khung năng lực");
            }
            assessment.rubricCriterionRepository().save(new RubricCriterion(
                version.getId(),
                frameworkCriterionId,
                criterion.code(),
                criterion.name(),
                "Chấm theo mô tả và dấu hiệu của bậc mục tiêu trong khung năng lực.",
                new RubricCriterionExamples(List.of(new RubricCriterionExample(
                    criterion.exampleTranscript(),
                    criterion.exampleExplanation(),
                    new BigDecimal(criterion.exampleScore())
                ))),
                new BigDecimal(criterion.weight()),
                new BigDecimal("0.00"),
                new BigDecimal("10.00"),
                criterion.order(),
                true,
                now, now, createdBy, createdBy
            ));
        }

        for (var band : RUBRIC_RESULT_BANDS) {
            assessment.rubricResultBandRepository().save(new RubricResultBand(
                version.getId(),
                band.code(),
                band.name(),
                band.description(),
                new BigDecimal(band.scoreMin()),
                new BigDecimal(band.scoreMax()),
                band.order(),
                now, now, createdBy, createdBy
            ));
        }

        return version.getId();
    }

    // ---------------------------------------------------------------------------------
    // Chính sách đánh giá và luật chấm theo khối
    // ---------------------------------------------------------------------------------

    private AssessmentPolicy seedAssessmentPolicy(
            UUID schoolId,
            UUID gradeLevelId,
            UUID gradeId,
            UUID languageId,
            SeededFrameworkVersion frameworkVersion,
            UUID rubricVersionId,
            GradeLevelSeed seed,
            UUID createdBy,
            Instant now) {
        var targetBandId = frameworkVersion.bandIdsByCode().get(seed.targetBandCode());
        if (targetBandId == null) {
            throw new IllegalStateException("Bậc mục tiêu " + seed.targetBandCode() + " không tồn tại trong khung");
        }
        return assessment.assessmentPolicyRepository().save(new AssessmentPolicy(
            schoolId,
            gradeLevelId,
            gradeId,
            null,
            languageId,
            frameworkVersion.versionId(),
            rubricVersionId,
            targetBandId,
            // passingScore để null theo chủ đích: phân loại dựa vào bậc mục tiêu và dấu
            // hiệu của bậc, không dựa vào một ngưỡng điểm cứng. Kết quả sẽ chốt ở FINAL và
            // do nhà trường tự quyết PASSED/FAILED -- xem ExamCandidateResultStatus.FINAL.
            null,
            seed.strictness(),
            1,
            AssessmentPolicyStatus.PUBLISHED,
            now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusMonths(7).toInstant(),
            null,
            now, now, createdBy, createdBy
        ));
    }

    private void seedScoringRules(UUID policyId, GradeLevelSeed seed, UUID createdBy, Instant now) {
        var rules = SCORING_RULES_BY_GRADE_LEVEL.get(seed.code());
        if (rules == null) {
            return;
        }
        for (var rule : rules) {
            assessment.scoringRuleRepository().save(new ScoringRule(
                policyId,
                rule.code(),
                rule.name(),
                rule.name() + " - ngưỡng áp cho " + seed.name() + " (bậc mục tiêu " + seed.targetBandCode() + ")",
                rule.conditionType(),
                rule.conditionParams(),
                rule.actionType(),
                rule.actionParams(),
                rule.priority(),
                rule.severity(),
                rule.stopProcessing(),
                true,
                now, now, createdBy, createdBy
            ));
        }
    }

    // ---------------------------------------------------------------------------------
    // Ngân hàng câu hỏi
    // ---------------------------------------------------------------------------------

    private QuestionBank seedQuestionBank(UUID schoolId, SupportedLanguage language, UUID createdBy, Instant now) {
        return questions.questionBankRepository().save(new QuestionBank(
            language.getId(),
            schoolId,
            "QB-" + SAMPLE_SCHOOL_CODE,
            "Ngân hàng câu hỏi nói - Trường THPT Mẫu Vox",
            "Câu hỏi thi nói chia theo khối, dùng cho kỳ thi tập trung.",
            QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED,
            now, now, createdBy, createdBy
        ));
    }

    private List<UUID> seedQuestions(
            UUID questionBankId,
            GradeLevelSeed gradeLevelSeed,
            List<UUID> teacherIds,
            Instant now) {
        var authorId = teacherIds.get(gradeLevelSeed.order() % teacherIds.size());
        var topic = questions.questionTopicRepository().save(new QuestionTopic(
            questionBankId,
            "TOPIC-" + gradeLevelSeed.code(),
            "Chủ đề nói " + gradeLevelSeed.name(),
            "Chủ đề thi nói dành cho " + gradeLevelSeed.name() + ", bậc mục tiêu " + gradeLevelSeed.targetBandCode(),
            QuestionTopicStatus.PUBLISHED,
            now, now, authorId, authorId
        ));

        var seeds = QUESTIONS_BY_GRADE_LEVEL.get(gradeLevelSeed.code());
        var questionIds = new ArrayList<UUID>(seeds.size());
        for (var index = 0; index < seeds.size(); index++) {
            var seed = seeds.get(index);
            var saved = questions.questionRepository().save(new Question(
                questionBankId,
                topic.getId(),
                "Q-" + gradeLevelSeed.code() + "-" + String.format("%02d", index + 1),
                "Nghe kỹ đề, chuẩn bị 30 giây rồi trả lời trong tối đa 90 giây.",
                seed.questionText(),
                seed.promptText(),
                "Ghi nhanh ra giấy nháp các ý chính trước khi bắt đầu nói.",
                seed.type(),
                30,
                30,
                90,
                QuestionSharing.SCHOOL_SHARED,
                null,
                false,
                // Câu hỏi của kỳ thi tập trung phải khoá theo kỳ thi: chỉ thành viên kỳ thi
                // mới xem được cho tới khi kỳ thi đóng và đề được giải phóng.
                QuestionConfidentiality.EXAM_RESTRICTED,
                null,
                QuestionStatus.PUBLISHED,
                now, now, authorId, authorId
            ));

            questions.questionEvaluationGuideRepository().save(new QuestionEvaluationGuide(
                saved.getId(),
                seed.expectedContent(),
                seed.keyPoints(),
                "Câu trả lời bám sát yêu cầu, có ý chính kèm ý hỗ trợ.",
                "Nói sang chủ đề khác hoàn toàn, hoặc chỉ nhắc lại đề bài.",
                "Chấm theo dấu hiệu của bậc mục tiêu ghi trong assessment policy của khối.",
                "Bỏ sót một vế của yêu cầu; nêu ý chính nhưng không có lý do hoặc ví dụ."
            ));
            questionIds.add(saved.getId());
        }
        return questionIds;
    }

    // ---------------------------------------------------------------------------------
    // Kỳ thi: blueprint -> exam -> mã đề -> ca thi -> thí sinh
    // ---------------------------------------------------------------------------------

    private void seedScheduledExam(
            UUID schoolId,
            UUID languageId,
            UUID gradeLevelId,
            GradeLevelSeed gradeLevelSeed,
            UUID assessmentPolicyId,
            List<UUID> questionIds,
            List<UUID> studentIds,
            List<UUID> teacherIds,
            List<SchoolRoom> rooms,
            UUID schoolAdminId,
            Instant now) {
        var blueprint = seedBlueprint(schoolId, languageId, gradeLevelId, gradeLevelSeed, schoolAdminId, now);
        var exam = exams.examRepository().save(new Exam(
            blueprint.blueprintId(),
            blueprint.versionId(),
            "EXAM-" + gradeLevelSeed.code() + "-HK1",
            "Thi nói học kỳ 1 - " + gradeLevelSeed.name(),
            "Kỳ thi nói tập trung cuối học kỳ 1, bậc mục tiêu " + gradeLevelSeed.targetBandCode() + ".",
            schoolId,
            languageId,
            ExamKind.CENTRALIZED,
            ExamDeliveryMode.LAB,
            ExamStatus.SCHEDULED,
            1,
            EXAM_DURATION_SECONDS,
            ResultDecisionMethod.HIGHEST,
            gradeLevelSeed.examStart(now),
            gradeLevelSeed.examStart(now).plus(4, ChronoUnit.HOURS),
            assessmentPolicyId,
            true,
            // Thứ tự tham số của constructor này không theo nhóm: createdAt nằm TRƯỚC hai
            // trường stream rồi mới tới updatedAt. Giữ nguyên đúng thứ tự khai báo.
            now,
            ExamRequiredStreamType.CAMERA_AND_SCREEN,
            ExamStreamTypePermission.ALL,
            now,
            schoolAdminId,
            schoolAdminId
        ));

        // Quản trị trường giữ vai trò CHAIR để có quyền khoá đề, lên lịch và công bố kết quả;
        // hai giáo viên của khối tham gia với vai trò ra đề và duyệt đề.
        exams.examMemberRepository().save(new ExamMember(exam.getId(), schoolAdminId, ExamMemberRole.CHAIR, now, schoolAdminId));
        var authorId = teacherIds.get(gradeLevelSeed.order() % teacherIds.size());
        var reviewerId = teacherIds.get((gradeLevelSeed.order() + 1) % teacherIds.size());
        exams.examMemberRepository().save(new ExamMember(exam.getId(), authorId, ExamMemberRole.AUTHOR, now, schoolAdminId));
        exams.examMemberRepository().save(new ExamMember(exam.getId(), reviewerId, ExamMemberRole.REVIEWER, now, schoolAdminId));

        var paperIds = seedPapers(exam.getId(), blueprint, questionIds, schoolAdminId, now);
        var scheduleIds = seedSchedules(exam.getId(), gradeLevelSeed, rooms, teacherIds, schoolAdminId, now);
        seedCandidates(exam.getId(), studentIds, paperIds, scheduleIds, schoolAdminId, now);
    }

    /**
     * Blueprint hai phần: phần 1 trọng số 0.40, phần 2 trọng số 0.60; mỗi phần hai slot
     * trọng số 0.50.
     *
     * <p>Tổng trọng số phải bằng 1.00 ở cả hai cấp — {@code CreateExamPaperUseCase}
     * kiểm tra bất biến này, và {@code ExamSessionResultCalculator.rollUp} cộng thẳng
     * {@code itemScore × weight} cho điểm phần nên phụ thuộc vào bất biến đó.
     */
    private SeededBlueprint seedBlueprint(
            UUID schoolId,
            UUID languageId,
            UUID gradeLevelId,
            GradeLevelSeed gradeLevelSeed,
            UUID createdBy,
            Instant now) {
        var blueprint = exams.examBlueprintRepository().save(new ExamBlueprint(
            schoolId,
            languageId,
            gradeLevelId,
            "BP-" + gradeLevelSeed.code() + "-SPEAKING",
            "Khung đề thi nói " + gradeLevelSeed.name(),
            "Hai phần, bốn câu, bậc mục tiêu " + gradeLevelSeed.targetBandCode() + ".",
            true,
            now, now, createdBy, createdBy
        ));

        var version = exams.examBlueprintVersionRepository().save(new ExamBlueprintVersion(
            blueprint.getId(),
            1,
            "BP-" + gradeLevelSeed.code() + "-SPEAKING-V1",
            "Phiên bản 1 của khung đề thi nói " + gradeLevelSeed.name(),
            ExamBlueprintVersionStatus.PUBLISHED,
            EXAM_DURATION_SECONDS,
            now.minus(14, ChronoUnit.DAYS),
            null,
            now, now, createdBy, createdBy
        ));

        var sectionSeeds = List.of(
            new BlueprintSectionSeed(1, "Phần 1 - Trả lời ngắn", "Trả lời hai câu hỏi ngắn về chủ đề quen thuộc.", "0.40"),
            new BlueprintSectionSeed(2, "Phần 2 - Trình bày và nêu ý kiến", "Trình bày ý kiến có lập luận cho hai câu hỏi.", "0.60")
        );

        var sections = new ArrayList<SeededBlueprintSection>();
        var slotOrder = 0;
        for (var sectionSeed : sectionSeeds) {
            var section = exams.examBlueprintSectionRepository().save(new ExamBlueprintSection(
                version.getId(),
                sectionSeed.order(),
                sectionSeed.title(),
                sectionSeed.instruction(),
                EXAM_DURATION_SECONDS / sectionSeeds.size(),
                new BigDecimal(sectionSeed.weight()),
                now, now, createdBy, createdBy
            ));

            var slotIds = new ArrayList<UUID>();
            for (var slotIndex = 1; slotIndex <= 2; slotIndex++) {
                slotOrder++;
                var slot = exams.examBlueprintSlotRepository().save(new ExamBlueprintSlot(
                    section.getId(),
                    version.getId(),
                    slotIndex,
                    new BigDecimal("0.50"),
                    30,
                    90,
                    // SELECTION chứ không FIXED: mỗi mã đề rút một câu khác nhau từ cùng
                    // một tiêu chí chọn, nhờ vậy hai mã đề không trùng câu hỏi.
                    ExamBlueprintSlotType.SELECTION,
                    null,
                    new QuestionSelectionSpec(null, null, gradeLevelSeed.targetBandCode(), null, null),
                    now, now, createdBy, createdBy
                ));
                slotIds.add(slot.getId());
            }
            sections.add(new SeededBlueprintSection(
                section.getId(), sectionSeed.order(), sectionSeed.title(),
                sectionSeed.instruction(), sectionSeed.weight(), slotIds));
        }

        return new SeededBlueprint(blueprint.getId(), version.getId(), sections, slotOrder);
    }

    /**
     * Sinh {@value #PAPER_VARIANTS} mã đề đã ở trạng thái {@code LOCKED} — điều kiện bắt
     * buộc của {@code AssignExamPapersUseCase} trước khi được phân đề cho thí sinh.
     *
     * <p>Mã đề thứ n lấy câu hỏi tại các chỉ số {@code slotIndex * PAPER_VARIANTS + n},
     * nên hai mã đề không dùng chung câu nào.
     */
    private List<UUID> seedPapers(
            UUID examId,
            SeededBlueprint blueprint,
            List<UUID> questionIds,
            UUID createdBy,
            Instant now) {
        var requiredQuestions = blueprint.totalSlots() * PAPER_VARIANTS;
        if (questionIds.size() < requiredQuestions) {
            throw new IllegalStateException(
                "Cần " + requiredQuestions + " câu hỏi cho " + PAPER_VARIANTS + " mã đề nhưng chỉ có " + questionIds.size());
        }

        var paperIds = new ArrayList<UUID>(PAPER_VARIANTS);
        for (var variant = 1; variant <= PAPER_VARIANTS; variant++) {
            var paper = exams.examPaperRepository().save(new ExamPaper(
                examId,
                blueprint.versionId(),
                "DE" + String.format("%02d", variant),
                variant,
                ExamPaperStatus.LOCKED,
                EXAM_DURATION_SECONDS,
                now, now, createdBy, createdBy
            ));

            var globalSlotIndex = 0;
            for (var blueprintSection : blueprint.sections()) {
                var paperSection = exams.examPaperSectionRepository().save(new ExamPaperSection(
                    paper.getId(),
                    blueprintSection.order(),
                    blueprintSection.title(),
                    blueprintSection.instruction(),
                    EXAM_DURATION_SECONDS / blueprint.sections().size(),
                    new BigDecimal(blueprintSection.weight()),
                    now, now, createdBy, createdBy
                ));

                for (var slotIndex = 0; slotIndex < blueprintSection.slotIds().size(); slotIndex++) {
                    var questionId = questionIds.get(globalSlotIndex * PAPER_VARIANTS + (variant - 1));
                    exams.examPaperItemRepository().save(new ExamPaperItem(
                        blueprintSection.slotIds().get(slotIndex),
                        paperSection.getId(),
                        paper.getId(),
                        questionId,
                        slotIndex + 1,
                        new BigDecimal("0.50")
                    ));
                    globalSlotIndex++;
                }
            }
            paperIds.add(paper.getId());
        }
        return paperIds;
    }

    /**
     * Mỗi kỳ thi có hai ca liên tiếp trong cùng một buổi, ở hai phòng khác nhau.
     *
     * <p>Ba khối được xếp vào ba ngày khác nhau nên không ca nào trùng phòng — đúng ràng
     * buộc {@code existsOverlapping} mà {@code CreateExamScheduleUseCase} kiểm tra.
     */
    private List<UUID> seedSchedules(
            UUID examId,
            GradeLevelSeed gradeLevelSeed,
            List<SchoolRoom> rooms,
            List<UUID> teacherIds,
            UUID createdBy,
            Instant now) {
        var examStart = gradeLevelSeed.examStart(now);
        var scheduleIds = new ArrayList<UUID>(SCHEDULES_PER_EXAM);

        for (var slot = 0; slot < SCHEDULES_PER_EXAM; slot++) {
            var room = rooms.get(slot % rooms.size());
            var start = examStart.plus(slot * 90L, ChronoUnit.MINUTES);
            var schedule = exams.examScheduleRepository().save(new ExamSchedule(
                examId,
                room.getId(),
                start,
                start.plus(60, ChronoUnit.MINUTES),
                ExamScheduleStatus.PUBLISHED,
                null,
                now, now, createdBy, createdBy
            ));

            // Hai giám thị mỗi ca, lấy lệch theo khối để không dồn hết vào cùng một người.
            var firstProctor = teacherIds.get((gradeLevelSeed.order() * 2 + slot) % teacherIds.size());
            var secondProctor = teacherIds.get((gradeLevelSeed.order() * 2 + slot + 1) % teacherIds.size());
            exams.examScheduleProctorRepository().save(new ExamScheduleProctor(schedule.getId(), firstProctor));
            exams.examScheduleProctorRepository().save(new ExamScheduleProctor(schedule.getId(), secondProctor));

            scheduleIds.add(schedule.getId());
        }
        return scheduleIds;
    }

    /**
     * Thí sinh được chia đều vào các ca và luân phiên mã đề, dừng ở {@code ASSIGNED}.
     *
     * <p>Bước tiếp theo trên UI là điểm danh chuyển sang {@code ATTENDED} — với kỳ thi
     * tập trung, {@code VerifyExamScheduleOtpUseCase} từ chối thí sinh chưa điểm danh.
     */
    private void seedCandidates(
            UUID examId,
            List<UUID> studentIds,
            List<UUID> paperIds,
            List<UUID> scheduleIds,
            UUID createdBy,
            Instant now) {
        var candidates = new ArrayList<ExamCandidate>(studentIds.size());
        for (var index = 0; index < studentIds.size(); index++) {
            var candidate = ExamCandidate.createFresh(examId, studentIds.get(index), createdBy, now);
            candidate.assignToSchedule(scheduleIds.get(index % scheduleIds.size()), now, createdBy);
            candidate.assignPaper(paperIds.get(index % paperIds.size()), now, createdBy);
            candidate.setStatus(ExamCandidateStatus.ASSIGNED);
            candidates.add(candidate);
        }
        exams.examCandidateRepository().saveAll(candidates);
    }

    // ---------------------------------------------------------------------------------
    // Tiện ích
    // ---------------------------------------------------------------------------------

    private UUID requireRoleId(String roleCode) {
        return identity.roleRepository().findByCode(roleCode)
            .orElseThrow(() -> new IllegalStateException("Thiếu vai trò bắt buộc: " + roleCode))
            .getId();
    }

    private UUID resolveSystemAdminId() {
        return identity.roleRepository().findByCode(SYSTEM_ADMIN_ROLE_CODE)
            .flatMap(role -> identity.userRoleRepository().findByRoleId(role.getId()).stream().findFirst())
            .map(userRole -> userRole.getUserId())
            .orElse(null);
    }

    private static FrameworkCriterionSignal sig(
            String code, String description, FrameworkCriterionSignalImportance importance, String evidenceHint) {
        return new FrameworkCriterionSignal(code, description, importance, evidenceHint);
    }

    // ---------------------------------------------------------------------------------
    // Bảng dữ liệu seed
    // ---------------------------------------------------------------------------------

    private record MemberSeed(String email, String phone, String fullName, Gender gender, LocalDate dateOfBirth) {
    }

    private record RoomSeed(String code, String name, String description) {
    }

    private record RoleIds(UUID schoolAdmin, UUID teacher, UUID student) {
    }

    private record ResultBandSeed(String code, String label, String description, int order) {
    }

    private record CriterionSeed(String code, String name, String description, int order, List<CriterionBandSeed> bands) {
    }

    private record CriterionBandSeed(
        String bandCode,
        String descriptor,
        List<FrameworkCriterionSignal> positiveSignals,
        List<FrameworkCriterionSignal> negativeSignals
    ) {
    }

    private record RubricCriterionSeed(
        String code,
        String name,
        String weight,
        int order,
        String exampleTranscript,
        String exampleExplanation,
        String exampleScore
    ) {
    }

    private record RubricBandSeed(String code, String name, String description, String scoreMin, String scoreMax, int order) {
    }

    private record ScoringRuleSeed(
        String code,
        String name,
        ScoringRuleConditionType conditionType,
        ScoringRuleConditionParams conditionParams,
        ScoringRuleActionType actionType,
        ScoringRuleActionParams actionParams,
        int priority,
        ScoringRuleSeverity severity,
        boolean stopProcessing
    ) {
    }

    private record QuestionSeed(
        QuestionType type,
        String questionText,
        String promptText,
        String expectedContent,
        String keyPoints
    ) {
    }

    private record GradeLevelSeed(
        String code,
        String name,
        int order,
        int birthYear,
        String targetBandCode,
        AssessmentPolicyStrictness strictness
    ) {

        private String classCodePrefix() {
            return code().substring(1);
        }

        /** Mỗi khối thi một ngày riêng để hai ca của các khối không tranh cùng một phòng. */
        private Instant examStart(Instant now) {
            return now.atZone(DateMapper.DEFAULT_INPUT_ZONE)
                .plusDays(6L + order())
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();
        }
    }

    private record BlueprintSectionSeed(int order, String title, String instruction, String weight) {
    }

    private record SeededFrameworkVersion(
        UUID frameworkId,
        UUID versionId,
        Map<String, UUID> bandIdsByCode,
        Map<String, UUID> criterionIdsByCode
    ) {
    }

    private record SeededBlueprint(UUID blueprintId, UUID versionId, List<SeededBlueprintSection> sections, int totalSlots) {
    }

    private record SeededBlueprintSection(
        UUID sectionId,
        int order,
        String title,
        String instruction,
        String weight,
        List<UUID> slotIds
    ) {
    }
}

/**
 * Các repository được gom thành sáu nhóm theo chủ đề thay vì nhồi hơn bốn mươi tham số
 * vào một constructor. Đây là bean của Spring nên vẫn được tiêm bình thường.
 */
@Component
record SampleIdentityRepositories(
    SchoolRepository schoolRepository,
    UserRepository userRepository,
    RoleRepository roleRepository,
    UserRoleRepository userRoleRepository,
    SchoolUserRepository schoolUserRepository,
    SupportedLanguageRepository supportedLanguageRepository,
    PasswordEncoderPort passwordEncoderPort
) {
}

@Component
record SampleStructureRepositories(
    SchoolGradeLevelRepository schoolGradeLevelRepository,
    SchoolGradeRepository schoolGradeRepository,
    SchoolClassRepository schoolClassRepository,
    SchoolClassUserRepository schoolClassUserRepository,
    SchoolRoomRepository schoolRoomRepository
) {
}

@Component
record SampleAssessmentRepositories(
    FrameworkRepository frameworkRepository,
    FrameworkVersionRepository frameworkVersionRepository,
    FrameworkCriterionRepository frameworkCriterionRepository,
    FrameworkResultBandRepository frameworkResultBandRepository,
    FrameworkCriterionBandRepository frameworkCriterionBandRepository,
    RubricRepository rubricRepository,
    RubricVersionRepository rubricVersionRepository,
    RubricCriterionRepository rubricCriterionRepository,
    RubricResultBandRepository rubricResultBandRepository,
    AssessmentPolicyRepository assessmentPolicyRepository,
    ScoringRuleRepository scoringRuleRepository
) {
}

@Component
record SampleQuestionRepositories(
    QuestionBankRepository questionBankRepository,
    QuestionTopicRepository questionTopicRepository,
    QuestionRepository questionRepository,
    QuestionEvaluationGuideRepository questionEvaluationGuideRepository
) {
}

@Component
record SampleExamRepositories(
    ExamBlueprintRepository examBlueprintRepository,
    ExamBlueprintVersionRepository examBlueprintVersionRepository,
    ExamBlueprintSectionRepository examBlueprintSectionRepository,
    ExamBlueprintSlotRepository examBlueprintSlotRepository,
    ExamRepository examRepository,
    ExamMemberRepository examMemberRepository,
    ExamPaperRepository examPaperRepository,
    ExamPaperSectionRepository examPaperSectionRepository,
    ExamPaperItemRepository examPaperItemRepository,
    ExamScheduleRepository examScheduleRepository,
    ExamScheduleProctorRepository examScheduleProctorRepository,
    ExamCandidateRepository examCandidateRepository
) {
}

@Component
record SampleSubscriptionRepositories(
    SubscriptionPlanRepository subscriptionPlanRepository,
    SchoolSubscriptionRepository schoolSubscriptionRepository,
    SubscriptionQuotaRepository subscriptionQuotaRepository
) {
}
