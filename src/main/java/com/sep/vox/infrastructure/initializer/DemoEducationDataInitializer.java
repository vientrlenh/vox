package com.sep.vox.infrastructure.initializer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
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
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.PlanQuota;
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
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
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
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

@Component
@Order(5)
public class DemoEducationDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoEducationDataInitializer.class);
    private static final String DEMO_SCHOOL_CODE = "DEMO-SCH-01";
    private static final String ENGLISH_CODE = "ENG";
    private static final int SCHOOL_COUNT = 2;
    private static final int TEACHERS_PER_SCHOOL = 3;
    private static final int STUDENTS_PER_SCHOOL = 18;
    private static final int HISTORICAL_STUDENTS_PER_SCHOOL = 6;
    private static final int CLASSES_PER_SCHOOL = 4;
    private static final int HISTORICAL_SCHOOL_YEARS = 2;
    private static final int HISTORICAL_CLASSES_PER_SCHOOL = HISTORICAL_SCHOOL_YEARS * 3;
    private static final int TOTAL_CLASSES_PER_SCHOOL = CLASSES_PER_SCHOOL + HISTORICAL_CLASSES_PER_SCHOOL;
    private static final int USERS_PER_SCHOOL =
        1 + TEACHERS_PER_SCHOOL + STUDENTS_PER_SCHOOL + HISTORICAL_STUDENTS_PER_SCHOOL;

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    @Value("${demo-data.enabled:false}")
    private boolean enabled;

    @Value("${demo-data.password:Password@123}")
    private String password;

    public DemoEducationDataInitializer(
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
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
            PasswordEncoderPort passwordEncoderPort) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        if (schoolRepository.existsByCode(DEMO_SCHOOL_CODE)) {
            var now = Instant.now();
            var hashedPassword = passwordEncoderPort.hash(password);
            replaceLegacyLinearResultBands();
            backfillDemoPlanTimeLimits();
            backfillHistoricalDemoClasses(now, hashedPassword);
            LOGGER.info("Demo education data already exists. Skip seeding");
            return;
        }

        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN").orElse(null);
        var teacherRole = roleRepository.findByCode("TEACHER").orElse(null);
        var studentRole = roleRepository.findByCode("STUDENT").orElse(null);
        var language = supportedLanguageRepository.findByCode(ENGLISH_CODE).orElse(null);
        if (schoolAdminRole == null || teacherRole == null || studentRole == null || language == null) {
            LOGGER.info("Required roles or supported language are missing. Skip demo education seeding");
            return;
        }

        var now = Instant.now();
        var hashedPassword = passwordEncoderPort.hash(password);
        var seededBy = resolveSeederUserId();

        if (seededBy != null) {
            seedSystemQuestionBanks(language, seededBy, now);
        } else {
            LOGGER.info("System admin user not found. Skip seeding system-owned question banks");
        }

        var subscriptionPlans = seedSubscriptionPlans(seededBy, now);
        var vstep = seedVstepFramework(seededBy, now);
        var schoolSeeds = List.of(
            new SchoolSeed("DEMO-SCH-01", "Trường THPT Hòa Bình", "Hà Nội",
                STUDENTS_PER_SCHOOL + HISTORICAL_STUDENTS_PER_SCHOOL),
            new SchoolSeed("DEMO-SCH-02", "Trường THPT Nguyễn Trãi", "Đà Nẵng",
                STUDENTS_PER_SCHOOL + HISTORICAL_STUDENTS_PER_SCHOOL)
        );

        var totalUsers = 0;
        for (var index = 0; index < schoolSeeds.size(); index++) {
            var schoolSeed = schoolSeeds.get(index);
            var school = createSchool(schoolSeed, seededBy, now);
            var members = createMembersForSchool(
                school,
                index + 1,
                schoolAdminRole.getId(),
                teacherRole.getId(),
                studentRole.getId(),
                hashedPassword,
                seededBy,
                now
            );
            var schoolStructure = seedSchoolStructure(school, language, members, now);
            var historicalStudentIds = createHistoricalStudentsForSchool(
                school,
                index + 1,
                studentRole.getId(),
                hashedPassword,
                members.schoolAdminId(),
                now
            );
            seedHistoricalSchoolYears(
                school,
                language,
                members.schoolAdminId(),
                members.teacherIds(),
                historicalStudentIds,
                now
            );
            seedSchoolSubscription(school, subscriptionPlans.get(index), now);
            seedSchoolRubricsAndPolicies(
                school,
                language,
                schoolStructure,
                members.schoolAdminId(),
                vstep,
                rubricSeedsForSchool(index),
                now
            );
            seedQuestionDataForSchool(
                school,
                language,
                members.schoolAdminId(),
                members.teacherIds().get(0),
                members.teacherIds().get(1),
                now
            );
            totalUsers += USERS_PER_SCHOOL;
        }

        LOGGER.info(
            "Demo education data seeded successfully. Created {} users, {} classes and active subscriptions across {} schools. Default password: {}",
            totalUsers,
            SCHOOL_COUNT * TOTAL_CLASSES_PER_SCHOOL,
            SCHOOL_COUNT,
            password
        );
    }

    private void replaceLegacyLinearResultBands() {
        var school = schoolRepository.findByCode(DEMO_SCHOOL_CODE).orElse(null);
        if (school == null) {
            return;
        }
        var now = Instant.now();
        var rubrics = rubricRepository.findAllByOwnerTypeAndSchoolId(
            RubricOwnerType.SCHOOL,
            school.getId(),
            0,
            100
        ).content();
        for (var rubric : rubrics) {
            for (var version : rubricVersionRepository.findByRubricId(rubric.getId())) {
                var currentBands = rubricResultBandRepository.findByRubricVersionId(version.getId());
                var hasLegacyLinearBand = currentBands.stream()
                    .anyMatch(band -> "SCALE_10_LINEAR".equals(band.getCode()));
                if (!hasLegacyLinearBand) {
                    continue;
                }
                rubricResultBandRepository.deleteByRubricVersionId(version.getId());
                for (var bandSeed : hoaBinhResultBandSeeds()) {
                    rubricResultBandRepository.save(new RubricResultBand(
                        version.getId(),
                        bandSeed.code(),
                        bandSeed.label(),
                        bandSeed.description(),
                        bandSeed.scoreMin(),
                        bandSeed.scoreMax(),
                        bandSeed.order(),
                        now,
                        now,
                        version.getCreatedBy(),
                        version.getUpdatedBy()
                    ));
                }
            }
        }
    }

    private void backfillDemoPlanTimeLimits() {
        subscriptionPlanRepository.findAllByStatus(PlanStatus.ACTIVE).forEach(plan -> {
            var demoPlanUpdate = demoPlanUpdateByPrice(plan.getPricePerYear());
            if (demoPlanUpdate == null) {
                return;
            }
            if (demoPlanUpdate.timeLimitMin() == plan.getMaxTimePerAttemptMin()
                    && demoPlanUpdate.pricePerYear().compareTo(plan.getPricePerYear()) == 0) {
                return;
            }
            plan.setMaxTimePerAttemptMin(demoPlanUpdate.timeLimitMin());
            plan.setPricePerYear(demoPlanUpdate.pricePerYear());
            subscriptionPlanRepository.save(plan);
        });
    }

    private DemoPlanUpdate demoPlanUpdateByPrice(BigDecimal pricePerYear) {
        if (pricePerYear == null) {
            return null;
        }
        if (pricePerYear.compareTo(new BigDecimal("1200000")) == 0
                || pricePerYear.compareTo(new BigDecimal("5000")) == 0) {
            return new DemoPlanUpdate(new BigDecimal("5000"), 5);
        }
        if (pricePerYear.compareTo(new BigDecimal("2400000")) == 0
                || pricePerYear.compareTo(new BigDecimal("10000")) == 0) {
            return new DemoPlanUpdate(new BigDecimal("10000"), 10);
        }
        if (pricePerYear.compareTo(new BigDecimal("4800000")) == 0
                || pricePerYear.compareTo(new BigDecimal("20000")) == 0) {
            return new DemoPlanUpdate(new BigDecimal("20000"), 15);
        }
        return null;
    }

    private School createSchool(SchoolSeed seed, UUID createdBy, Instant now) {
        return schoolRepository.save(School.create(
            seed.code(),
            seed.name(),
            "Dữ liệu demo đầy đủ cho trường học, lớp học và đánh giá VSTEP",
            phoneOf(seed.code(), 1),
            emailOf("contact", seed.code()),
            null,
            seed.address(),
            seed.studentCount(),
            createdBy,
            now
        ));
    }

    private MemberSeedResult createMembersForSchool(
            School school,
            int schoolIndex,
            UUID schoolAdminRoleId,
            UUID teacherRoleId,
            UUID studentRoleId,
            String hashedPassword,
            UUID createdBy,
            Instant now) {
        var schoolCode = school.getCode().value();
        var schoolAdmin = saveMember(
            emailOf("admin" + schoolIndex, schoolCode),
            phoneOf(schoolCode, 10),
            "Quản Trị Viên Demo",
            Gender.MALE,
            LocalDate.of(1985, 1 + schoolIndex, 10),
            hashedPassword,
            schoolAdminRoleId,
            school.getId(),
            createdBy,
            now,
            null
        );

        var teacherIds = new ArrayList<UUID>();
        var teacherNames = List.of("Nguyễn Minh Anh", "Trần Thu Hà", "Lê Hoàng Nam");
        for (var teacherIndex = 0; teacherIndex < TEACHERS_PER_SCHOOL; teacherIndex++) {
            var teacher = saveMember(
                emailOf("teacher" + schoolIndex + (char) ('a' + teacherIndex), schoolCode),
                phoneOf(schoolCode, 11 + teacherIndex),
                teacherNames.get(teacherIndex),
                teacherIndex == 1 ? Gender.FEMALE : Gender.MALE,
                LocalDate.of(1987 + teacherIndex * 2, 2 + teacherIndex, 11 + teacherIndex),
                hashedPassword,
                teacherRoleId,
                school.getId(),
                createdBy,
                now,
                null
            );
            teacherIds.add(teacher.getId());
        }

        // Instant.plus không nhận đơn vị YEARS/MONTHS (không phải khoảng thời gian chính xác nếu
        // không có lịch), nên phải quy về múi giờ trước khi cộng theo năm.
        var endDate = now.atZone(DateMapper.DEFAULT_INPUT_ZONE).plusYears(3).toInstant();
        var studentIds = new ArrayList<UUID>();
        var studentNames = currentStudentNames();
        for (var studentIndex = 0; studentIndex < STUDENTS_PER_SCHOOL; studentIndex++) {
            var student = saveMember(
                emailOf("student" + schoolIndex + (char) ('a' + studentIndex), schoolCode),
                phoneOf(schoolCode, 21 + studentIndex),
                studentNames.get(studentIndex),
                studentIndex % 2 == 0 ? Gender.FEMALE : Gender.MALE,
                LocalDate.of(2008 + studentIndex % 2, studentIndex % 12 + 1, studentIndex % 20 + 1),
                hashedPassword,
                studentRoleId,
                school.getId(),
                createdBy,
                now,
                endDate
            );
            studentIds.add(student.getId());
        }
        return new MemberSeedResult(schoolAdmin.getId(), List.copyOf(teacherIds), List.copyOf(studentIds));
    }

    private List<SubscriptionPlan> seedSubscriptionPlans(UUID createdBy, Instant now) {
        var planSeeds = List.of(
            new SubscriptionPlanSeed(
                "Khởi đầu",
                "Đủ cho trường bắt đầu tổ chức luyện nói và kiểm tra định kỳ",
                new BigDecimal("5000"),
                5,
                100,
                false,
                300,
                100,
                600
            ),
            new SubscriptionPlanSeed(
                "Chuyên nghiệp",
                "Hạn mức cân bằng cho trường phổ thông triển khai thường xuyên",
                new BigDecimal("10000"),
                10,
                500,
                true,
                1200,
                400,
                2500
            ),
            new SubscriptionPlanSeed(
                "Toàn diện",
                "Hạn mức lớn cho nhiều khối lớp và các kỳ thi tập trung",
                new BigDecimal("20000"),
                15,
                2000,
                false,
                5000,
                1500,
                10000
            )
        );

        var plans = new ArrayList<SubscriptionPlan>();
        for (var seed : planSeeds) {
            var plan = subscriptionPlanRepository.save(new SubscriptionPlan(
                seed.name(),
                seed.tagline(),
                seed.pricePerYear(),
                365,
                seed.maxTimePerAttemptMin(),
                seed.maxStudentCount(),
                seed.popular(),
                PlanStatus.ACTIVE,
                1,
                now,
                createdBy
            ));
            planQuotaRepository.save(new PlanQuota(
                plan.getId(),
                QuotaType.GRADING,
                seed.gradingQuota(),
                new BigDecimal("2500")
            ));
            planQuotaRepository.save(new PlanQuota(
                plan.getId(),
                QuotaType.CLASS_TEST,
                seed.classTestQuota(),
                new BigDecimal("1500")
            ));
            planQuotaRepository.save(new PlanQuota(
                plan.getId(),
                QuotaType.PRACTICE,
                seed.practiceQuota(),
                new BigDecimal("1000")
            ));
            plans.add(plan);
        }
        return List.copyOf(plans);
    }

    private void seedSchoolSubscription(School school, SubscriptionPlan plan, Instant now) {
        var startDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var subscription = schoolSubscriptionRepository.save(new SchoolSubscription(
            school.getId(),
            plan.getId(),
            startDate,
            startDate.plusDays(plan.getValidityDays()),
            SubscriptionStatus.ACTIVE,
            plan.getPricePerYear(),
            null,
            now
        ));
        planQuotaRepository.findAllByPlanId(plan.getId()).forEach(planQuota ->
            subscriptionQuotaRepository.save(new SubscriptionQuota(
                subscription.getId(),
                planQuota.getQuotaType(),
                planQuota.getIncludedQuantity(),
                0
            ))
        );
    }

    private SchoolStructureSeed seedSchoolStructure(
            School school,
            SupportedLanguage language,
            MemberSeedResult members,
            Instant now) {
        var schoolAdminId = members.schoolAdminId();
        var gradeLevels = new ArrayList<SchoolGradeLevel>();
        var schoolGrades = new ArrayList<SchoolGrade>();
        for (var gradeNumber = 10; gradeNumber <= 12; gradeNumber++) {
            var gradeLevel = schoolGradeLevelRepository.save(new SchoolGradeLevel(
                school.getId(),
                "KHOI_" + gradeNumber,
                "Khối " + gradeNumber,
                "Khối lớp " + gradeNumber + " trong dữ liệu demo VSTEP",
                gradeNumber,
                SchoolGradeLevelStatus.ACTIVE,
                now,
                now,
                schoolAdminId,
                schoolAdminId
            ));
            gradeLevels.add(gradeLevel);
            schoolGrades.add(schoolGradeRepository.save(new SchoolGrade(
                gradeLevel.getId(),
                "NH_2026_2027",
                "Năm học 2026 - 2027",
                "Năm học hiện hành của khối " + gradeNumber,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 5, 31),
                SchoolGradeStatus.ACTIVE,
                now,
                now,
                schoolAdminId,
                schoolAdminId
            )));
        }

        var classSizes = List.of(5, 5, 4, 4);
        var classGradeIndexes = List.of(0, 1, 2, 2);
        var classCodes = List.of("10A1", "11A1", "12A1", "12A2");
        var nextStudentIndex = 0;
        for (var classIndex = 0; classIndex < CLASSES_PER_SCHOOL; classIndex++) {
            var classCode = classCodes.get(classIndex);
            var schoolClass = schoolClassRepository.save(SchoolClass.create(
                school.getId(),
                language.getId(),
                schoolGrades.get(classGradeIndexes.get(classIndex)).getId(),
                classCode,
                "Lớp " + classCode,
                "Lớp tiếng Anh VSTEP năm học 2026 - 2027",
                schoolAdminId,
                now
            ));

            var teacherId = members.teacherIds().get(classIndex % TEACHERS_PER_SCHOOL);
            schoolClassUserRepository.save(new SchoolClassUser(
                teacherId,
                schoolClass.getId(),
                true,
                now,
                null,
                schoolAdminId
            ));
            for (var studentOffset = 0; studentOffset < classSizes.get(classIndex); studentOffset++) {
                schoolClassUserRepository.save(new SchoolClassUser(
                    members.studentIds().get(nextStudentIndex++),
                    schoolClass.getId(),
                    true,
                    now,
                    null,
                    schoolAdminId
                ));
            }
        }
        return new SchoolStructureSeed(List.copyOf(gradeLevels), List.copyOf(schoolGrades));
    }

    private void backfillHistoricalDemoClasses(Instant now, String hashedPassword) {
        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN").orElse(null);
        var teacherRole = roleRepository.findByCode("TEACHER").orElse(null);
        var studentRole = roleRepository.findByCode("STUDENT").orElse(null);
        var language = supportedLanguageRepository.findByCode(ENGLISH_CODE).orElse(null);
        if (schoolAdminRole == null || teacherRole == null || studentRole == null || language == null) {
            LOGGER.info("Required roles or supported language are missing. Skip historical demo class backfill");
            return;
        }

        var schoolSeeds = List.of(
            new SchoolSeed("DEMO-SCH-01", "Trường THPT Hòa Bình", "Hà Nội",
                STUDENTS_PER_SCHOOL + HISTORICAL_STUDENTS_PER_SCHOOL),
            new SchoolSeed("DEMO-SCH-02", "Trường THPT Nguyễn Trãi", "Đà Nẵng",
                STUDENTS_PER_SCHOOL + HISTORICAL_STUDENTS_PER_SCHOOL)
        );
        var seededBy = resolveSeederUserId();

        for (var index = 0; index < schoolSeeds.size(); index++) {
            var school = schoolRepository.findByCode(schoolSeeds.get(index).code()).orElse(null);
            if (school == null) {
                continue;
            }

            var schoolAdminId = findSchoolUserIdsByRole(school.getId(), schoolAdminRole.getId(), 1)
                .stream()
                .findFirst()
                .orElse(seededBy);
            var teacherIds = findSchoolUserIdsByRole(school.getId(), teacherRole.getId(), TEACHERS_PER_SCHOOL);
            if (schoolAdminId == null || teacherIds.size() < TEACHERS_PER_SCHOOL) {
                LOGGER.info("Demo school {} is missing admin or teacher users. Skip historical class backfill", school.getCode().value());
                continue;
            }

            var historicalStudentIds = createHistoricalStudentsForSchool(
                school,
                index + 1,
                studentRole.getId(),
                hashedPassword,
                schoolAdminId,
                now
            );
            seedHistoricalSchoolYears(
                school,
                language,
                schoolAdminId,
                teacherIds,
                historicalStudentIds,
                now
            );
        }
    }

    private List<UUID> findSchoolUserIdsByRole(UUID schoolId, UUID roleId, int limit) {
        return schoolUserRepository.findBySchoolId(schoolId, null, roleId, "ACTIVE", null, 1, 100)
            .content()
            .stream()
            .map(schoolUser -> schoolUser.getUserId())
            .limit(limit)
            .toList();
    }

    private List<UUID> createHistoricalStudentsForSchool(
            School school,
            int schoolIndex,
            UUID studentRoleId,
            String hashedPassword,
            UUID createdBy,
            Instant now) {
        var schoolCode = school.getCode().value();
        var joinedAt = now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusYears(2).toInstant();
        var leftAt = now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusMonths(1).toInstant();
        var studentIds = new ArrayList<UUID>();
        var studentNames = historicalStudentNames();
        for (var studentIndex = 0; studentIndex < HISTORICAL_STUDENTS_PER_SCHOOL; studentIndex++) {
            var email = emailOf("alumni" + schoolIndex + (char) ('a' + studentIndex), schoolCode);
            var existingStudent = userRepository.findByEmail(email).orElse(null);
            User student;
            if (existingStudent == null) {
                student = saveMember(
                    email,
                    phoneOf(schoolCode, 60 + studentIndex),
                    studentNames.get(studentIndex),
                    studentIndex % 2 == 0 ? Gender.FEMALE : Gender.MALE,
                    LocalDate.of(2006 + studentIndex % 2, studentIndex % 12 + 1, studentIndex % 20 + 1),
                    hashedPassword,
                    studentRoleId,
                    school.getId(),
                    createdBy,
                    joinedAt,
                    leftAt
                );
            } else {
                student = existingStudent;
                ensureUserRole(student.getId(), studentRoleId, joinedAt);
                ensureSchoolUser(student.getId(), school.getId(), joinedAt, leftAt);
            }
            studentIds.add(student.getId());
        }
        return List.copyOf(studentIds);
    }

    private void seedHistoricalSchoolYears(
            School school,
            SupportedLanguage language,
            UUID schoolAdminId,
            List<UUID> teacherIds,
            List<UUID> historicalStudentIds,
            Instant now) {
        var historicalStartYears = List.of(2024, 2025);
        for (var yearIndex = 0; yearIndex < historicalStartYears.size(); yearIndex++) {
            var startYear = historicalStartYears.get(yearIndex);
            var endYear = startYear + 1;
            var gradeStart = LocalDate.of(startYear, 9, 1);
            var gradeEnd = LocalDate.of(endYear, 5, 31);
            for (var gradeOffset = 0; gradeOffset < 3; gradeOffset++) {
                var gradeNumber = 10 + gradeOffset;
                var gradeLevel = ensureGradeLevel(school, gradeNumber, schoolAdminId, now);
                var schoolGrade = ensureHistoricalGrade(
                    gradeLevel,
                    startYear,
                    endYear,
                    gradeNumber,
                    gradeStart,
                    gradeEnd,
                    schoolAdminId,
                    now
                );
                var classCode = "Y" + startYear + "_" + gradeNumber + "A" + (yearIndex + 1);
                var schoolClass = ensureHistoricalClass(
                    school,
                    language,
                    schoolGrade,
                    classCode,
                    "Lớp " + gradeNumber + "A" + (yearIndex + 1) + " (" + startYear + " - " + endYear + ")",
                    schoolAdminId,
                    now
                );

                var teacherId = teacherIds.get((yearIndex * 3 + gradeOffset) % teacherIds.size());
                var joinedAt = gradeStart.atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
                var leftAt = gradeEnd.plusDays(1).atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
                ensureClassMembership(teacherId, schoolClass.getId(), false, joinedAt, leftAt, schoolAdminId);
                for (var studentSlot = 0; studentSlot < 2; studentSlot++) {
                    var studentIndex = (gradeOffset * 2 + studentSlot + yearIndex * 2) % historicalStudentIds.size();
                    ensureClassMembership(
                        historicalStudentIds.get(studentIndex),
                        schoolClass.getId(),
                        false,
                        joinedAt,
                        leftAt,
                        schoolAdminId
                    );
                }
            }
        }
    }

    private SchoolGradeLevel ensureGradeLevel(School school, int gradeNumber, UUID schoolAdminId, Instant now) {
        var code = "KHOI_" + gradeNumber;
        return schoolGradeLevelRepository.findBySchoolIdAndCode(school.getId(), code)
            .orElseGet(() -> schoolGradeLevelRepository.save(new SchoolGradeLevel(
                school.getId(),
                code,
                "Khối " + gradeNumber,
                "Khối lớp " + gradeNumber + " trong dữ liệu demo VSTEP",
                gradeNumber,
                SchoolGradeLevelStatus.ACTIVE,
                now,
                now,
                schoolAdminId,
                schoolAdminId
            )));
    }

    private SchoolGrade ensureHistoricalGrade(
            SchoolGradeLevel gradeLevel,
            int startYear,
            int endYear,
            int gradeNumber,
            LocalDate startDate,
            LocalDate endDate,
            UUID schoolAdminId,
            Instant now) {
        var code = "NH_" + startYear + "_" + endYear;
        return schoolGradeRepository.findBySchoolGradeLevelIdAndCode(gradeLevel.getId(), code)
            .orElseGet(() -> schoolGradeRepository.save(new SchoolGrade(
                gradeLevel.getId(),
                code,
                "Năm học " + startYear + " - " + endYear,
                "Năm học đã qua của khối " + gradeNumber,
                startDate,
                endDate,
                SchoolGradeStatus.INACTIVE,
                now,
                now,
                schoolAdminId,
                schoolAdminId
            )));
    }

    private SchoolClass ensureHistoricalClass(
            School school,
            SupportedLanguage language,
            SchoolGrade schoolGrade,
            String classCode,
            String className,
            UUID schoolAdminId,
            Instant now) {
        return schoolClassRepository.findBySchoolIdAndCode(school.getId(), classCode)
            .orElseGet(() -> {
                var schoolClass = SchoolClass.create(
                    school.getId(),
                    language.getId(),
                    schoolGrade.getId(),
                    classCode,
                    className,
                    "Lớp tiếng Anh VSTEP thuộc niên khóa đã qua",
                    schoolAdminId,
                    now
                );
                schoolClass.setStatus(SchoolClassStatus.INACTIVE);
                return schoolClassRepository.save(schoolClass);
            });
    }

    private void ensureClassMembership(
            UUID userId,
            UUID schoolClassId,
            boolean active,
            Instant joinedAt,
            Instant leftAt,
            UUID assignedBy) {
        if (schoolClassUserRepository.findByUserIdAndSchoolClassId(userId, schoolClassId).isPresent()) {
            return;
        }
        schoolClassUserRepository.save(new SchoolClassUser(
            userId,
            schoolClassId,
            active,
            joinedAt,
            leftAt,
            assignedBy
        ));
    }

    private void ensureUserRole(UUID userId, UUID roleId, Instant assignedAt) {
        if (userRoleRepository.findByUserIdAndRoleId(userId, roleId).isPresent()) {
            return;
        }
        userRoleRepository.save(new UserRole(userId, roleId, assignedAt));
    }

    private void ensureSchoolUser(UUID userId, UUID schoolId, Instant joinedAt, Instant leftAt) {
        if (schoolUserRepository.findBySchoolIdAndUserId(schoolId, userId).isPresent()) {
            return;
        }
        schoolUserRepository.save(SchoolUser.create(userId, schoolId, joinedAt, leftAt));
    }

    private VstepFrameworkSeed seedVstepFramework(UUID createdBy, Instant now) {
        var framework = frameworkRepository.save(new Framework(
            new FrameworkCode("VSTEP"),
            "Khung đánh giá nói VSTEP",
            "Khung đánh giá định hướng VSTEP cho học sinh Việt Nam, gồm 5 tiêu chí phân tích trên thang 0 - 10",
            true,
            now,
            now,
            createdBy,
            createdBy
        ));
        var version = frameworkVersionRepository.save(new FrameworkVersion(
            framework.getId(),
            "VSTEP_SPEAKING_1_0",
            "VSTEP Speaking 1.0",
            "Phiên bản dùng cho đánh giá nói theo định hướng VSTEP",
            1,
            now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusYears(1).toInstant(),
            null,
            FrameworkVersionStatus.PUBLISHED,
            now,
            now,
            createdBy,
            createdBy
        ));

        var bands = bandSeeds().stream()
            .map(seed -> frameworkResultBandRepository.save(new FrameworkResultBand(
                version.getId(),
                seed.code(),
                seed.label(),
                seed.description(),
                seed.order(),
                now,
                now,
                createdBy,
                createdBy
            )))
            .toList();

        var criteria = new ArrayList<FrameworkCriterion>();
        for (var criterionSeed : criterionSeeds()) {
            var criterion = frameworkCriterionRepository.save(new FrameworkCriterion(
                version.getId(),
                criterionSeed.frameworkCode(),
                criterionSeed.name(),
                criterionSeed.description(),
                criterionSeed.order(),
                now,
                now,
                createdBy,
                createdBy
            ));
            criteria.add(criterion);

            for (var bandIndex = 0; bandIndex < bands.size(); bandIndex++) {
                var resultBand = bands.get(bandIndex);
                var descriptor = criterionSeed.bandDescriptors().get(bandIndex);
                frameworkCriterionBandRepository.save(new FrameworkCriterionBand(
                    criterion.getId(),
                    resultBand.getId(),
                    descriptor,
                    criterionSignals(
                        criterionSeed.frameworkCode() + "_" + resultBand.getCode() + "_POS",
                        descriptor,
                        "Tìm bằng chứng trực tiếp trong audio hoặc transcript"
                    ),
                    criterionSignals(
                        criterionSeed.frameworkCode() + "_" + resultBand.getCode() + "_NEG",
                        "Biểu hiện không ổn định hoặc thấp hơn mô tả của band " + resultBand.getLabel(),
                        "Không suy diễn khi không đủ bằng chứng"
                    ),
                    now,
                    now,
                    createdBy,
                    createdBy
                ));
            }
        }
        return new VstepFrameworkSeed(
            framework.getId(),
            version.getId(),
            List.copyOf(bands),
            List.copyOf(criteria)
        );
    }

    private FrameworkCriterionSignals criterionSignals(String code, String description, String evidenceHint) {
        return new FrameworkCriterionSignals(List.of(new FrameworkCriterionSignal(
            code,
            description,
            FrameworkCriterionSignalImportance.HIGH,
            evidenceHint
        )));
    }

    private void seedSchoolRubricsAndPolicies(
            School school,
            SupportedLanguage language,
            SchoolStructureSeed schoolStructure,
            UUID schoolAdminId,
            VstepFrameworkSeed vstep,
            List<SchoolRubricSeed> rubricSeeds,
            Instant now) {
        for (var rubricSeed : rubricSeeds) {
            var rubric = rubricRepository.save(new Rubric(
                language.getId(),
                vstep.frameworkId(),
                "VSTEP-" + school.getCode().value() + "-" + rubricSeed.codeSuffix(),
                rubricSeed.name() + " - " + school.getName(),
                rubricSeed.description(),
                RubricOwnerType.SCHOOL,
                school.getId()
            ));

            RubricVersion currentVersion = null;
            for (var versionSeed : rubricSeed.versions()) {
                var effectiveFrom = versionSeed.status() == RubricStatus.ARCHIVED
                    ? now.atZone(DateMapper.DEFAULT_INPUT_ZONE).minusYears(2).toInstant()
                    : now.minus(1, ChronoUnit.DAYS);
                var effectiveTo = versionSeed.status() == RubricStatus.ARCHIVED
                    ? now.minus(2, ChronoUnit.DAYS)
                    : null;
                var version = new RubricVersion(
                    rubric.getId(),
                    versionSeed.version(),
                    rubric.getCode() + "_V" + versionSeed.version(),
                    rubricSeed.name() + " " + versionSeed.name(),
                    versionSeed.description(),
                    versionSeed.status(),
                    effectiveFrom,
                    effectiveTo,
                    BigDecimal.ZERO,
                    versionSeed.totalScaleMax(),
                    versionSeed.totalScoreMethod(),
                    now,
                    now,
                    schoolAdminId,
                    schoolAdminId
                );
                var rubricVersion = rubricVersionRepository.save(version);
                seedRubricVersionDetails(rubricVersion, versionSeed, vstep, schoolAdminId, now);
                if (versionSeed.status() == RubricStatus.PUBLISHED) {
                    currentVersion = rubricVersion;
                }
            }

            if (currentVersion == null) {
                throw new IllegalStateException("Rubric demo phải có một version đang PUBLISHED");
            }
            var targetBandId = vstep.bands().stream()
                .filter(band -> band.getCode().equals(rubricSeed.targetBandCode()))
                .findFirst()
                .orElseThrow()
                .getId();
            assessmentPolicyRepository.save(new AssessmentPolicy(
                school.getId(),
                schoolStructure.gradeLevels().get(rubricSeed.gradeLevelIndex()).getId(),
                null,
                null,
                language.getId(),
                vstep.versionId(),
                currentVersion.getId(),
                targetBandId,
                rubricSeed.passingScore(),
                rubricSeed.strictness(),
                1,
                AssessmentPolicyStatus.PUBLISHED,
                now.minus(1, ChronoUnit.DAYS),
                null,
                now,
                now,
                schoolAdminId,
                schoolAdminId
            ));
        }
    }

    private void seedRubricVersionDetails(
            com.sep.vox.domain.model.rubric.RubricVersion rubricVersion,
            SchoolRubricVersionSeed versionSeed,
            VstepFrameworkSeed vstep,
            UUID schoolAdminId,
            Instant now) {
        for (var criterionSeed : criterionSeeds()) {
            var frameworkCriterion = vstep.criteria().stream()
                .filter(item -> item.getCode().equals(criterionSeed.frameworkCode()))
                .findFirst()
                .orElseThrow();
            rubricCriterionRepository.save(new RubricCriterion(
                rubricVersion.getId(),
                frameworkCriterion.getId(),
                criterionSeed.rubricCode(),
                criterionSeed.name(),
                criterionSeed.description(),
                new RubricCriterionExamples(List.of(new RubricCriterionExample(
                    criterionSeed.exampleTranscript(),
                    criterionSeed.exampleExplanation(),
                    versionSeed.criterionScaleMax().multiply(new BigDecimal("0.60"))
                        .setScale(2, RoundingMode.HALF_UP)
                ))),
                versionSeed.criterionWeights().get(criterionSeed.order() - 1),
                BigDecimal.ZERO,
                versionSeed.criterionScaleMax(),
                criterionSeed.order(),
                true,
                now,
                now,
                schoolAdminId,
                schoolAdminId
            ));
        }

        for (var bandSeed : versionSeed.resultBands()) {
            rubricResultBandRepository.save(new RubricResultBand(
                rubricVersion.getId(),
                bandSeed.code(),
                bandSeed.label(),
                bandSeed.description(),
                bandSeed.scoreMin(),
                bandSeed.scoreMax(),
                bandSeed.order(),
                now,
                now,
                schoolAdminId,
                schoolAdminId
            ));
        }
    }

    private void seedQuestionDataForSchool(
            School school,
            SupportedLanguage language,
            UUID schoolAdminId,
            UUID teacherOneId,
            UUID teacherTwoId,
            Instant now) {
        var bank = questionBankRepository.save(new QuestionBank(
            language.getId(),
            school.getId(),
            "QB-" + school.getCode().value(),
            "Question Bank - " + school.getName(),
            "Demo question bank for " + school.getName(),
            QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED,
            now,
            now,
            schoolAdminId,
            schoolAdminId
        ));

        var speakingTopic = questionTopicRepository.save(new QuestionTopic(
            bank.getId(),
            "TOPIC-SPK-" + school.getCode().value(),
            "Speaking Foundation",
            "Basic speaking topics",
            QuestionTopicStatus.PUBLISHED,
            now,
            now,
            schoolAdminId,
            schoolAdminId
        ));
        var opinionTopic = questionTopicRepository.save(new QuestionTopic(
            bank.getId(),
            "TOPIC-OPN-" + school.getCode().value(),
            "Opinion Practice",
            "Opinion presentation practice",
            QuestionTopicStatus.PUBLISHED,
            now,
            now,
            schoolAdminId,
            schoolAdminId
        ));

        var questionOne = questionRepository.save(new Question(
            bank.getId(),
            speakingTopic.getId(),
            "Q-" + school.getCode().value() + "-001",
            "Speak clearly and naturally.",
            "Please introduce yourself and your school in English.",
            "Mention your name, grade, and one favorite subject.",
            "You are a student introducing yourself in a school event.",
            QuestionType.SHORT_ANSWER,
            15,
            30,
            60,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.PUBLISHED,
            now,
            now,
            teacherOneId,
            teacherOneId
        ));
        questionEvaluationGuideRepository.save(new QuestionEvaluationGuide(
            questionOne.getId(),
            "A short self-introduction with school context.",
            "Name; school; grade; favorite subject",
            "My name is... I study at... I am in grade...",
            "Talking only about unrelated hobbies.",
            "Focus on clarity, completeness, and relevance.",
            "Missing the school or grade information."
        ));

        questionRepository.save(new Question(
            bank.getId(),
            speakingTopic.getId(),
            "Q-" + school.getCode().value() + "-002",
            "Speak fluently for one minute.",
            "Describe a memorable class activity you joined recently.",
            "Talk about what happened and why you liked it.",
            "Think about a presentation, club meeting, or competition.",
            QuestionType.DESCRIPTION,
            20,
            45,
            90,
            QuestionSharing.PRIVATE,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.DRAFT,
            now,
            now,
            teacherOneId,
            teacherOneId
        ));

        var questionThree = questionRepository.save(new Question(
            bank.getId(),
            opinionTopic.getId(),
            "Q-" + school.getCode().value() + "-003",
            "State a clear opinion and explain it.",
            "Do you think students should use AI tools for homework? Why or why not?",
            "Give your opinion and support it with reasons.",
            "Consider both benefits and risks before answering.",
            QuestionType.OPINION,
            25,
            45,
            120,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.SUBMITTED_FOR_REVIEW,
            now,
            now,
            teacherTwoId,
            teacherTwoId
        ));
        questionEvaluationGuideRepository.save(new QuestionEvaluationGuide(
            questionThree.getId(),
            "A balanced opinion with supporting reasons.",
            "Clear stance; at least two reasons",
            "Students can use AI if they still think critically.",
            "Only repeating that AI is popular.",
            "Reward reasoning, examples, and relevance.",
            "No clear opinion or no supporting explanation."
        ));

        questionRepository.save(new Question(
            bank.getId(),
            opinionTopic.getId(),
            "Q-" + school.getCode().value() + "-004",
            "Answer briefly but completely.",
            "What is one effective way to improve your English speaking skill?",
            "State one method and explain how it helps.",
            "You may mention practice habits or learning tools.",
            QuestionType.LONG_ANSWER,
            15,
            30,
            75,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.PUBLISHED,
            now,
            now,
            teacherTwoId,
            teacherTwoId
        ));
    }

    private void seedSystemQuestionBanks(SupportedLanguage language, UUID systemAdminId, Instant now) {
        var speakingBank = questionBankRepository.save(new QuestionBank(
            language.getId(),
            null,
            "QB-SYS-SPEAK",
            "System Question Bank - Speaking",
            "System-wide standard question bank, speaking topics",
            QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.PUBLISHED,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        var speakingTopic = questionTopicRepository.save(new QuestionTopic(
            speakingBank.getId(),
            "TOPIC-SYS-CONV",
            "Daily Conversation",
            "System-wide daily conversation topic",
            QuestionTopicStatus.PUBLISHED,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        questionRepository.save(new Question(
            speakingBank.getId(),
            speakingTopic.getId(),
            "Q-SYS-SPEAK-001",
            "Speak clearly at a moderate pace.",
            "Talk about your daily routine on a typical school day.",
            "Mention what time you wake up, study, and relax.",
            "Think about morning, afternoon, and evening activities.",
            QuestionType.LONG_ANSWER,
            15,
            30,
            90,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.PUBLISHED,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        var speakingReview = questionRepository.save(new Question(
            speakingBank.getId(),
            speakingTopic.getId(),
            "Q-SYS-SPEAK-002",
            "Answer naturally, don't recite from memory.",
            "What do you usually do to relax after a stressful school day?",
            "Give at least one specific activity and explain why it helps.",
            "Consider hobbies, sports, music, or time with family/friends.",
            QuestionType.SHORT_ANSWER,
            10,
            30,
            60,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.SUBMITTED_FOR_REVIEW,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        questionEvaluationGuideRepository.save(new QuestionEvaluationGuide(
            speakingReview.getId(),
            "A specific relaxing activity with a brief reason.",
            "Activity named; reason given",
            "I usually listen to music because it helps me feel calm.",
            "Only complaining about school without answering the question.",
            "Reward specificity and a clear connecting reason.",
            "Vague answer with no concrete activity mentioned."
        ));

        var readingBank = questionBankRepository.save(new QuestionBank(
            language.getId(),
            null,
            "QB-SYS-READ",
            "System Question Bank - General",
            "System-wide standard question bank, general/social topics",
            QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.PUBLISHED,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        var readingTopic = questionTopicRepository.save(new QuestionTopic(
            readingBank.getId(),
            "TOPIC-SYS-READ",
            "General Topics",
            "System-wide general/social topics",
            QuestionTopicStatus.PUBLISHED,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        questionRepository.save(new Question(
            readingBank.getId(),
            readingTopic.getId(),
            "Q-SYS-READ-001",
            "Speak naturally and keep it brief.",
            "What is your favorite time of day, and why?",
            "Try to give a clear, specific answer.",
            "Think of one clear example before you start.",
            QuestionType.SHORT_ANSWER,
            15,
            20,
            45,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.DRAFT,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
        questionRepository.save(new Question(
            readingBank.getId(),
            readingTopic.getId(),
            "Q-SYS-READ-002",
            "Describe in as much detail as you can.",
            "Describe your ideal weekend from morning to night, including what you would do and who you would spend time with.",
            "Include some detail about your ideal day.",
            "Organize your answer roughly in time order: morning, afternoon, evening.",
            QuestionType.DESCRIPTION,
            25,
            60,
            150,
            QuestionSharing.SCHOOL_SHARED,
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.PUBLISHED,
            now,
            now,
            systemAdminId,
            systemAdminId
        ));
    }

    private List<SchoolRubricSeed> rubricSeedsForSchool(int schoolIndex) {
        var balancedWeights = decimalList("0.20", "0.20", "0.20", "0.20", "0.20");
        var foundationWeights = decimalList("0.25", "0.25", "0.15", "0.15", "0.20");
        var academicWeights = decimalList("0.15", "0.20", "0.20", "0.20", "0.25");
        var communicationWeights = decimalList("0.20", "0.25", "0.15", "0.15", "0.25");
        var hoaBinhGrades = hoaBinhResultBandSeeds();
        var nguyenTraiGrades = nguyenTraiLetterGradeBandSeeds();

        if (schoolIndex == 0) {
            return List.of(
                new SchoolRubricSeed(
                    "G10_FOUNDATION",
                    "Rubric nền tảng khối 10",
                    "Củng cố độ rõ và khả năng duy trì hội thoại cơ bản.",
                    0,
                    "BAC_2",
                    new BigDecimal("45.00"),
                    AssessmentPolicyStrictness.LENIENT,
                    List.of(
                        weightedVersion(1, "v1 lịch sử", RubricStatus.ARCHIVED, balancedWeights, hoaBinhGrades),
                        weightedVersion(2, "v2 hiện hành", RubricStatus.PUBLISHED, foundationWeights, hoaBinhGrades)
                    )
                ),
                new SchoolRubricSeed(
                    "G11_DEVELOPMENT",
                    "Rubric phát triển khối 11",
                    "Mở rộng ngữ pháp, từ vựng và khả năng phát triển ý ở Bậc 3.",
                    1,
                    "BAC_3",
                    new BigDecimal("60.00"),
                    AssessmentPolicyStrictness.STANDARD,
                    List.of(
                        weightedVersion(1, "v1 cân bằng", RubricStatus.ARCHIVED, balancedWeights, hoaBinhGrades),
                        weightedVersion(2, "v2 học thuật", RubricStatus.PUBLISHED, academicWeights, hoaBinhGrades)
                    )
                ),
                new SchoolRubricSeed(
                    "G12_MOCK",
                    "Rubric thi thử khối 12",
                    "Đánh giá tổng hợp theo định hướng kỳ thi VSTEP bậc 4.",
                    2,
                    "BAC_4",
                    new BigDecimal("70.00"),
                    AssessmentPolicyStrictness.STANDARD,
                    List.of(
                        weightedVersion(1, "v1 cân bằng", RubricStatus.ARCHIVED, balancedWeights, hoaBinhGrades),
                        weightedVersion(2, "v2 học thuật", RubricStatus.PUBLISHED, academicWeights, hoaBinhGrades)
                    )
                )
            );
        }

        return List.of(
            new SchoolRubricSeed(
                "G10_COMMUNICATION",
                "Rubric giao tiếp khối 10",
                "Chấm mức độ đáp ứng Bậc 3 trên thang 0 - 100, ưu tiên giao tiếp rõ và mạch lạc.",
                0,
                "BAC_3",
                new BigDecimal("50.00"),
                AssessmentPolicyStrictness.STANDARD,
                List.of(
                    weightedVersion(1, "v1 cân bằng", RubricStatus.ARCHIVED, balancedWeights, nguyenTraiGrades),
                    weightedVersion(2, "v2 giao tiếp", RubricStatus.PUBLISHED, communicationWeights, nguyenTraiGrades)
                )
            ),
            new SchoolRubricSeed(
                "G11_ACADEMIC",
                "Rubric học thuật khối 11",
                "Chấm mức độ đáp ứng Bậc 4 trên thang 0 - 100 với trọng số học thuật.",
                1,
                "BAC_4",
                new BigDecimal("65.00"),
                AssessmentPolicyStrictness.STRICT,
                List.of(
                    weightedVersion(1, "v1 cân bằng", RubricStatus.ARCHIVED, balancedWeights, nguyenTraiGrades),
                    weightedVersion(2, "v2 học thuật", RubricStatus.PUBLISHED, academicWeights, nguyenTraiGrades)
                )
            ),
            new SchoolRubricSeed(
                "G12_HIGH_STAKES",
                "Rubric thi chuẩn đầu ra khối 12",
                "Chấm mức độ đáp ứng Bậc 4 trên thang 0 - 100 với ngưỡng xếp loại nghiêm ngặt.",
                2,
                "BAC_4",
                new BigDecimal("75.00"),
                AssessmentPolicyStrictness.STRICT,
                List.of(
                    weightedVersion(1, "v1 cân bằng", RubricStatus.ARCHIVED, balancedWeights, nguyenTraiGrades),
                    weightedVersion(2, "v2 chuẩn đầu ra", RubricStatus.PUBLISHED, communicationWeights, nguyenTraiGrades)
                )
            )
        );
    }

    private SchoolRubricVersionSeed weightedVersion(
            int version,
            String name,
            RubricStatus status,
            List<BigDecimal> weights,
            List<BandSeed> resultBands) {
        return new SchoolRubricVersionSeed(
            version,
            name,
            "AI chấm mức độ đáp ứng bậc mục tiêu trên thang 0 - 100; trường quy đổi bằng dải xếp loại.",
            status,
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            RubricTotalScoreMethod.WEIGHTED_AVERAGE,
            weights,
            resultBands
        );
    }

    private List<BigDecimal> decimalList(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }

    private List<BandSeed> hoaBinhResultBandSeeds() {
        return List.of(
            gradeBand("BAND_1", "Band 1", 1, "0.00", "20.99"),
            gradeBand("BAND_2", "Band 2", 2, "21.00", "40.99"),
            gradeBand("BAND_3", "Band 3", 3, "41.00", "60.99"),
            gradeBand("BAND_4", "Band 4", 4, "61.00", "80.99"),
            gradeBand("BAND_5", "Band 5", 5, "81.00", "100.00")
        );
    }

    private List<BandSeed> nguyenTraiLetterGradeBandSeeds() {
        return List.of(
            gradeBand("F", "Chưa đạt", 1, "0.00", "49.99"),
            gradeBand("D", "D", 2, "50.00", "59.99"),
            gradeBand("C", "C", 3, "60.00", "69.99"),
            gradeBand("B", "B", 4, "70.00", "84.99"),
            gradeBand("A", "A", 5, "85.00", "100.00")
        );
    }

    private BandSeed gradeBand(String code, String label, int order, String scoreMin, String scoreMax) {
        return new BandSeed(
            code,
            label,
            "Xếp loại công bố theo cấu hình của nhà trường.",
            order,
            new BigDecimal(scoreMin),
            new BigDecimal(scoreMax)
        );
    }

    private List<BandSeed> bandSeeds() {
        return List.of(
            new BandSeed(
                "BAC_1",
                "Bậc 1",
                "Có thể sử dụng ngôn ngữ rất cơ bản trong các tình huống quen thuộc.",
                1,
                new BigDecimal("0.00"),
                new BigDecimal("1.99")
            ),
            new BandSeed(
                "BAC_2",
                "Bậc 2",
                "Có thể giao tiếp đơn giản về các chủ đề quen thuộc và nhu cầu trực tiếp.",
                2,
                new BigDecimal("2.00"),
                new BigDecimal("3.99")
            ),
            new BandSeed(
                "BAC_3",
                "Bậc 3",
                "Có thể giao tiếp độc lập ở mức cơ bản về các chủ đề quen thuộc.",
                3,
                new BigDecimal("4.00"),
                new BigDecimal("5.99")
            ),
            new BandSeed(
                "BAC_4",
                "Bậc 4",
                "Có thể giao tiếp độc lập, rõ ràng và tương đối linh hoạt trong nhiều tình huống.",
                4,
                new BigDecimal("6.00"),
                new BigDecimal("7.99")
            ),
            new BandSeed(
                "BAC_5",
                "Bậc 5",
                "Có thể sử dụng ngôn ngữ thành thạo, linh hoạt và hiệu quả.",
                5,
                new BigDecimal("8.00"),
                new BigDecimal("8.99")
            ),
            new BandSeed(
                "BAC_6",
                "Bậc 6",
                "Có thể sử dụng ngôn ngữ ở mức độ rất thành thạo, chính xác và tinh tế trong các tình huống phức tạp.",
                6,
                new BigDecimal("9.00"),
                new BigDecimal("10.00")
            )
        );
    }

    private List<CriterionSeed> criterionSeeds() {
        return List.of(
            new CriterionSeed(
                "PRONUNCIATION",
                "pronunciation",
                "Phát âm",
                "Độ rõ của âm, trọng âm, nhịp điệu và ngữ điệu; đánh giá mức độ người nghe hiểu được.",
                1,
                List.of(
                    "Phát âm được một số từ và cụm từ rất quen thuộc; người nghe thường phải cố gắng mới hiểu.",
                    "Phát âm các câu đơn giản tương đối hiểu được dù ảnh hưởng tiếng mẹ đẻ và lỗi âm còn rõ.",
                    "Phát âm nhìn chung rõ và dễ hiểu; lỗi âm hoặc trọng âm đôi lúc xuất hiện nhưng hiếm khi cản trở ý nghĩa.",
                    "Phát âm rõ, tự nhiên; trọng âm, nhịp điệu và ngữ điệu phần lớn phù hợp, chỉ còn lỗi nhỏ.",
                    "Phát âm rõ ràng và linh hoạt; điều khiển trọng âm, nhịp điệu, ngữ điệu hiệu quả để truyền đạt sắc thái.",
                    "Phát âm tự nhiên, chính xác và tinh tế; vận dụng linh hoạt các đặc điểm ngữ âm để biểu đạt hàm ý phức tạp."
                ),
                "I believe school clubs help students become more confident.",
                "Phát âm nhìn chung rõ, có trọng âm câu hợp lý và vẫn còn một vài âm chịu ảnh hưởng tiếng mẹ đẻ."
            ),
            new CriterionSeed(
                "FLUENCY",
                "fluency",
                "Lưu loát",
                "Tốc độ, tính liên tục, độ dài mạch nói và khả năng kiểm soát ngập ngừng hoặc tự sửa.",
                2,
                List.of(
                    "Chỉ tạo được các từ hoặc cụm rất ngắn, ngập ngừng dài và thường xuyên phải bắt đầu lại.",
                    "Nói được chuỗi câu đơn giản ngắn; tốc độ chậm và có nhiều khoảng dừng để tìm từ.",
                    "Duy trì được mạch nói về chủ đề quen thuộc; có ngập ngừng và tự sửa nhưng thông điệp vẫn liên tục.",
                    "Nói khá trôi chảy với tốc độ phù hợp; khoảng dừng chủ yếu phục vụ lập kế hoạch ý phức tạp.",
                    "Nói trôi chảy, tự nhiên và linh hoạt; rất ít ngập ngừng gây chú ý, tự sửa không làm đứt mạch.",
                    "Duy trì mạch nói hoàn toàn tự nhiên và linh hoạt ngay cả với nội dung trừu tượng, phức tạp hoặc bất ngờ."
                ),
                "In my opinion, online learning is useful because it saves time and gives students more resources.",
                "Mạch nói liên tục ở tốc độ phù hợp, có một vài khoảng dừng ngắn để tổ chức ý."
            ),
            new CriterionSeed(
                "GRAMMAR",
                "grammar",
                "Ngữ pháp",
                "Phạm vi và độ chính xác của cấu trúc ngữ pháp được sử dụng để diễn đạt ý.",
                3,
                List.of(
                    "Chủ yếu dùng mẫu câu ghi nhớ rất đơn giản; lỗi thường xuyên khiến ý khó hiểu.",
                    "Dùng được một số cấu trúc câu đơn giản; lỗi còn phổ biến nhưng ý cơ bản thường nhận ra được.",
                    "Kiểm soát khá tốt cấu trúc quen thuộc và thử dùng câu phức; lỗi không gây hiểu nhầm đáng kể.",
                    "Sử dụng đa dạng cấu trúc đơn và phức với độ chính xác cao; lỗi nhỏ không mang tính hệ thống.",
                    "Sử dụng linh hoạt nhiều cấu trúc phức tạp với độ chính xác nhất quán và phù hợp sắc thái.",
                    "Kiểm soát hoàn toàn cấu trúc ngữ pháp phức tạp, kể cả khi diễn đạt sắc thái tinh tế hoặc tái cấu trúc ý tức thời."
                ),
                "If schools offered more clubs, students would have more chances to develop social skills.",
                "Sử dụng đúng câu điều kiện và mệnh đề mục đích; cấu trúc có độ phức tạp phù hợp Bậc 4."
            ),
            new CriterionSeed(
                "VOCABULARY",
                "vocabulary",
                "Từ vựng",
                "Phạm vi, độ chính xác, tính phù hợp và khả năng diễn đạt lại khi thiếu từ.",
                4,
                List.of(
                    "Chỉ dùng được vốn từ rời rạc rất cơ bản về bản thân và tình huống quen thuộc.",
                    "Có đủ từ để xử lý nhu cầu giao tiếp đơn giản; lặp từ và chọn từ chưa chính xác còn thường xuyên.",
                    "Có đủ vốn từ cho chủ đề quen thuộc, biết diễn đạt lại; đôi lúc chọn từ chưa chính xác nhưng ý vẫn rõ.",
                    "Dùng vốn từ khá rộng và phù hợp chủ đề; có thể diễn đạt lại hiệu quả, chỉ đôi lúc chọn từ chưa tự nhiên.",
                    "Dùng vốn từ rộng, chính xác và linh hoạt, gồm thành ngữ hoặc sắc thái phù hợp ngữ cảnh.",
                    "Vận dụng vốn từ rất rộng và tinh tế, xử lý chính xác hàm ý, thành ngữ và khác biệt phong cách trong mọi ngữ cảnh."
                ),
                "Participating in volunteer projects can broaden students' perspectives and strengthen their sense of responsibility.",
                "Từ vựng đa dạng, đúng chủ đề giáo dục và có kết hợp từ tự nhiên."
            ),
            new CriterionSeed(
                "COHERENCE",
                "coherence",
                "Mạch lạc",
                "Mức độ trả lời đúng nhiệm vụ, phát triển ý, tổ chức thông tin và liên kết diễn ngôn.",
                5,
                List.of(
                    "Chỉ đưa ra được thông tin rời rạc, rất ít liên kết và thường chưa hoàn thành yêu cầu nhiệm vụ.",
                    "Trả lời được phần chính bằng các ý đơn giản; tổ chức còn tuyến tính và chủ yếu dùng từ nối cơ bản.",
                    "Trả lời đúng trọng tâm, phát triển được các ý chính với một số chi tiết và liên kết nhìn chung rõ.",
                    "Phát triển ý đầy đủ, có cấu trúc rõ và dùng phương tiện liên kết đa dạng, phù hợp.",
                    "Phát triển lập luận sâu, mạch lạc và linh hoạt; tổ chức, nhấn mạnh và liên kết ý rất hiệu quả.",
                    "Tổ chức diễn ngôn phức tạp hoàn toàn mạch lạc, linh hoạt điều chỉnh cấu trúc và sắc thái theo mục đích giao tiếp."
                ),
                "I support school clubs for two reasons. First, they build teamwork. Second, they help students discover their strengths.",
                "Câu trả lời có quan điểm, hai lý do tách biệt và phương tiện liên kết rõ ràng."
            )
        );
    }

    private User saveMember(
            String email,
            String phone,
            String fullName,
            Gender gender,
            LocalDate dateOfBirth,
            String hashedPassword,
            UUID roleId,
            UUID schoolId,
            UUID createdBy,
            Instant now,
            Instant endDate) {
        var user = new User(
            new Email(email),
            hashedPassword,
            new Phone(phone),
            new FullName(fullName),
            gender,
            new DateOfBirth(dateOfBirth),
            "Địa chỉ demo",
            null,
            UserStatus.ACTIVE,
            now,
            now,
            createdBy,
            createdBy
        );
        var savedUser = userRepository.save(user);
        userRoleRepository.save(new UserRole(savedUser.getId(), roleId, now));
        schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, now, endDate));
        return savedUser;
    }

    private UUID resolveSeederUserId() {
        var systemAdminRole = roleRepository.findByCode("SYSTEM_ADMIN").orElse(null);
        if (systemAdminRole == null) {
            return null;
        }
        return userRoleRepository.findByRoleId(systemAdminRole.getId()).stream()
            .map(role -> role.getUserId())
            .findFirst()
            .orElse(null);
    }

    private List<String> currentStudentNames() {
        return List.of(
            "Phạm Gia Hân",
            "Đỗ Minh Khang",
            "Võ Ngọc Anh",
            "Bùi Đức Anh",
            "Đặng Khánh Linh",
            "Hoàng Nam Phong",
            "Mai Thu Uyên",
            "Cao Nhật Minh",
            "Vũ Bảo Trâm",
            "Trịnh Quang Huy",
            "Lý Thanh Mai",
            "Nguyễn Tuấn Kiệt",
            "Hà An Nhiên",
            "Đinh Gia Bảo",
            "Phan Minh Châu",
            "Hồ Anh Thư",
            "Tạ Quốc Bảo",
            "Lâm Hải Đăng"
        );
    }

    private List<String> historicalStudentNames() {
        return List.of(
            "Ngô Hoài An",
            "Chu Bảo Ngọc",
            "Dương Minh Quân",
            "Lưu Khánh Vy",
            "Vương Đức Minh",
            "Trần Gia Huy"
        );
    }

    private String emailOf(String localPart, String schoolCode) {
        return localPart + "." + schoolCode.toLowerCase().replace("-", "") + "@vox.demo";
    }

    private String phoneOf(String schoolCode, int suffix) {
        var digits = schoolCode.replaceAll("\\D", "");
        return "09" + String.format("%03d%05d", Integer.parseInt(digits), suffix);
    }

    private record SchoolSeed(String code, String name, String address, int studentCount) {
    }

    private record MemberSeedResult(UUID schoolAdminId, List<UUID> teacherIds, List<UUID> studentIds) {
    }

    private record SchoolStructureSeed(
        List<SchoolGradeLevel> gradeLevels,
        List<SchoolGrade> schoolGrades
    ) {
    }

    private record SubscriptionPlanSeed(
        String name,
        String tagline,
        BigDecimal pricePerYear,
        int maxTimePerAttemptMin,
        int maxStudentCount,
        boolean popular,
        int gradingQuota,
        int classTestQuota,
        int practiceQuota
    ) {
    }

    private record DemoPlanUpdate(BigDecimal pricePerYear, int timeLimitMin) {
    }

    private record BandSeed(
        String code,
        String label,
        String description,
        int order,
        BigDecimal scoreMin,
        BigDecimal scoreMax
    ) {
    }

    private record CriterionSeed(
        String frameworkCode,
        String rubricCode,
        String name,
        String description,
        int order,
        List<String> bandDescriptors,
        String exampleTranscript,
        String exampleExplanation
    ) {
        private CriterionSeed {
            if (bandDescriptors == null || bandDescriptors.size() != 6) {
                throw new IllegalArgumentException("Mỗi tiêu chí VSTEP phải có đúng 6 mô tả từ Bậc 1 đến Bậc 6");
            }
            bandDescriptors = List.copyOf(bandDescriptors);
        }
    }

    private record SchoolRubricSeed(
        String codeSuffix,
        String name,
        String description,
        int gradeLevelIndex,
        String targetBandCode,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        List<SchoolRubricVersionSeed> versions
    ) {
        private SchoolRubricSeed {
            if (gradeLevelIndex < 0 || gradeLevelIndex > 2) {
                throw new IllegalArgumentException("Rubric demo phải thuộc một trong ba khối 10 - 12");
            }
            if (versions == null || versions.size() < 2
                    || versions.stream().filter(item -> item.status() == RubricStatus.PUBLISHED).count() != 1) {
                throw new IllegalArgumentException("Mỗi rubric demo cần ít nhất 2 version và đúng 1 version PUBLISHED");
            }
            versions = List.copyOf(versions);
        }
    }

    private record SchoolRubricVersionSeed(
        int version,
        String name,
        String description,
        RubricStatus status,
        BigDecimal totalScaleMax,
        BigDecimal criterionScaleMax,
        RubricTotalScoreMethod totalScoreMethod,
        List<BigDecimal> criterionWeights,
        List<BandSeed> resultBands
    ) {
        private SchoolRubricVersionSeed {
            if (criterionWeights == null || criterionWeights.size() != 5) {
                throw new IllegalArgumentException("Rubric version phải có đúng 5 trọng số tiêu chí");
            }
            var weightSum = criterionWeights.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (totalScoreMethod == RubricTotalScoreMethod.WEIGHTED_AVERAGE
                    && weightSum.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Rubric WEIGHTED_AVERAGE phải có tổng trọng số bằng 1");
            }
            if (totalScoreMethod == RubricTotalScoreMethod.SUM
                    && totalScaleMax.compareTo(criterionScaleMax.multiply(BigDecimal.valueOf(5))) != 0) {
                throw new IllegalArgumentException("Rubric SUM phải có thang tổng bằng 5 lần thang tiêu chí");
            }
            if (resultBands == null || resultBands.isEmpty()) {
                throw new IllegalArgumentException("Rubric version phải có dải xếp loại kết quả");
            }
            criterionWeights = List.copyOf(criterionWeights);
            resultBands = List.copyOf(resultBands);
        }
    }

    private record VstepFrameworkSeed(
        UUID frameworkId,
        UUID versionId,
        List<FrameworkResultBand> bands,
        List<FrameworkCriterion> criteria
    ) {
    }
}
