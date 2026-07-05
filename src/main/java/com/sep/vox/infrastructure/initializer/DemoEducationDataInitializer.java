package com.sep.vox.infrastructure.initializer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

import com.sep.vox.application.port.output.PasswordEncoderPort;
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
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
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
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

@Component
@Order(5)
public class DemoEducationDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoEducationDataInitializer.class);
    private static final String DEMO_SCHOOL_CODE = "DEMO-SCH-01";
    private static final String ENGLISH_CODE = "ENG";

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
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        if (schoolRepository.existsByCode(DEMO_SCHOOL_CODE)) {
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

        var now = OffsetDateTime.now();
        var hashedPassword = passwordEncoderPort.hash(password);
        var seededBy = resolveSeederUserId();

        if (seededBy != null) {
            seedSystemQuestionBanks(language, seededBy, now);
        } else {
            LOGGER.info("System admin user not found. Skip seeding system-owned question banks");
        }

        var schoolSeeds = List.of(
            new SchoolSeed("DEMO-SCH-01", "Truong THPT Hoa Binh", "Ha Noi", 1200),
            new SchoolSeed("DEMO-SCH-02", "Truong THPT Nguyen Trai", "Da Nang", 980),
            new SchoolSeed("DEMO-SCH-03", "Truong THPT Le Quy Don", "Hai Phong", 1100),
            new SchoolSeed("DEMO-SCH-04", "Truong THPT Tran Phu", "Can Tho", 1050)
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
            totalUsers += 5;
            seedQuestionDataForSchool(school, language, members.schoolAdminId(), members.teacherOneId(), members.teacherTwoId(), now);
        }

        LOGGER.info("Demo education data seeded successfully. Created {} users across 4 schools. Default password: {}",
            totalUsers, password);
    }

    private School createSchool(SchoolSeed seed, UUID createdBy, OffsetDateTime now) {
        return schoolRepository.save(School.create(
            seed.code(),
            seed.name(),
            "Du lieu demo cho school, user va question",
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
            OffsetDateTime now) {
        var schoolCode = school.getCode().value();
        var schoolAdmin = saveMember(
            emailOf("admin" + schoolIndex, schoolCode),
            phoneOf(schoolCode, 10),
            "School Admin " + ordinalWord(schoolIndex),
            Gender.MALE,
            LocalDate.of(1985, 1 + schoolIndex, 10),
            hashedPassword,
            schoolAdminRoleId,
            school.getId(),
            createdBy,
            now,
            null
        );
        var teacherOne = saveMember(
            emailOf("teacher" + schoolIndex + "a", schoolCode),
            phoneOf(schoolCode, 11),
            "Teacher A " + ordinalWord(schoolIndex),
            Gender.FEMALE,
            LocalDate.of(1989, 2 + schoolIndex, 11),
            hashedPassword,
            teacherRoleId,
            school.getId(),
            createdBy,
            now,
            null
        );
        var teacherTwo = saveMember(
            emailOf("teacher" + schoolIndex + "b", schoolCode),
            phoneOf(schoolCode, 12),
            "Teacher B " + ordinalWord(schoolIndex),
            Gender.MALE,
            LocalDate.of(1991, 3 + schoolIndex, 12),
            hashedPassword,
            teacherRoleId,
            school.getId(),
            createdBy,
            now,
            null
        );
        var endDate = now.plusYears(3);
        saveMember(
            emailOf("student" + schoolIndex + "a", schoolCode),
            phoneOf(schoolCode, 21),
            "Student A " + ordinalWord(schoolIndex),
            Gender.FEMALE,
            LocalDate.of(2008, 4 + schoolIndex, 13),
            hashedPassword,
            studentRoleId,
            school.getId(),
            createdBy,
            now,
            endDate
        );
        saveMember(
            emailOf("student" + schoolIndex + "b", schoolCode),
            phoneOf(schoolCode, 22),
            "Student B " + ordinalWord(schoolIndex),
            Gender.MALE,
            LocalDate.of(2009, 5 + schoolIndex, 14),
            hashedPassword,
            studentRoleId,
            school.getId(),
            createdBy,
            now,
            endDate
        );
        return new MemberSeedResult(schoolAdmin.getId(), teacherOne.getId(), teacherTwo.getId());
    }

    private void seedQuestionDataForSchool(
            School school,
            SupportedLanguage language,
            UUID schoolAdminId,
            UUID teacherOneId,
            UUID teacherTwoId,
            OffsetDateTime now) {
        var bank = questionBankRepository.save(new QuestionBank(
            language.getId(),
            school.getId(),
            "QB-" + school.getCode().value(),
            "Ngan hang cau hoi " + school.getName(),
            "Ngan hang cau hoi demo cua " + school.getName(),
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
            "Chu de noi co ban",
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
            "Chu de luyen trinh bay quan diem",
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
            "Doc ro rang va tu nhien.",
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
            "Noi lien mach trong mot phut.",
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
            "Dua ra quan diem ro rang va giai thich.",
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
            "Tra loi gon nhung day du.",
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

    private void seedSystemQuestionBanks(SupportedLanguage language, UUID systemAdminId, OffsetDateTime now) {
        var speakingBank = questionBankRepository.save(new QuestionBank(
            language.getId(),
            null,
            "QB-SYS-SPEAK",
            "Ngan hang cau hoi he thong - Speaking",
            "Ngan hang cau hoi chuan dung chung cho toan he thong, chu de noi",
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
            "Chu de hoi thoai hang ngay dung chung he thong",
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
            "Noi ro rang, toc do vua phai.",
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
            "Tra loi tu nhien, khong doc thuoc long.",
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
            "Ngan hang cau hoi he thong - Doc hieu",
            "Ngan hang cau hoi chuan dung chung cho toan he thong, chu de doc hieu",
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
            "Reading Comprehension",
            "Chu de doc hieu dung chung he thong",
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
            "Doc doan van truoc khi tra loi.",
            "Summarize the main idea of a short passage about climate change in your own words.",
            "Keep your summary under three sentences.",
            "Read the passage carefully and identify the central argument first.",
            QuestionType.DESCRIPTION,
            30,
            45,
            120,
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
            "Tra loi dung trong tam cau hoi.",
            "Explain the author's purpose in writing the passage.",
            "Support your answer with one detail from the text.",
            "Distinguish between informing, persuading, and entertaining.",
            QuestionType.OPINION,
            20,
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
            OffsetDateTime now,
            OffsetDateTime endDate) {
        var user = new User(
            new Email(email),
            hashedPassword,
            new Phone(phone),
            new FullName(fullName),
            gender,
            new DateOfBirth(dateOfBirth),
            "Demo address",
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
            .map(UserRole::getUserId)
            .findFirst()
            .orElse(null);
    }

    private String emailOf(String localPart, String schoolCode) {
        return localPart + "." + schoolCode.toLowerCase().replace("-", "") + "@vox.demo";
    }

    private static final String[] ORDINAL_WORDS = {"One", "Two", "Three", "Four"};

    private String ordinalWord(int index) {
        if (index < 1 || index > ORDINAL_WORDS.length) {
            throw new IllegalArgumentException("Khong co ordinal word cho index " + index);
        }
        return ORDINAL_WORDS[index - 1];
    }

    private String phoneOf(String schoolCode, int suffix) {
        var digits = schoolCode.replaceAll("\\D", "");
        return "09" + String.format("%03d%05d", Integer.parseInt(digits), suffix);
    }

    private record SchoolSeed(String code, String name, String address, int studentCount) {
    }

    private record MemberSeedResult(UUID schoolAdminId, UUID teacherOneId, UUID teacherTwoId) {
    }
}
