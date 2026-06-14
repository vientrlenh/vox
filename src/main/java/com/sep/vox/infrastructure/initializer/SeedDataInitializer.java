package com.sep.vox.infrastructure.initializer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
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
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.domain.valueobject.Phone;

@Component
@Order(3)
public class SeedDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedDataInitializer.class);
    private static final String SEED_PASSWORD = "Abc@1234";
    private static final String ENGLISH_CODE = "ENG";
    private static final UUID DEFAULT_SCHOOL_CREATED_BY_ID = UUID.fromString("019ea126-32d2-74e9-bdfd-212d74abbd5c");
    private static final int SYSTEM_SEED_QUESTION_COUNT = 5;
    private static final int SCHOOL_QUESTION_COUNT_PER_CREATOR = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    public SeedDataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionBankRepository questionBankRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionRepository questionRepository,
            PasswordEncoderPort passwordEncoderPort) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("admin1@vox.local")) {
            LOGGER.info("Seed data already exists. Skip seeding");
            return;
        }

        var systemAdminRole = roleRepository.findByCode("SYSTEM_ADMIN").orElse(null);
        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN").orElse(null);
        var teacherRole = roleRepository.findByCode("TEACHER").orElse(null);
        var studentRole = roleRepository.findByCode("STUDENT").orElse(null);

        if (systemAdminRole == null || schoolAdminRole == null || teacherRole == null || studentRole == null) {
            LOGGER.info("Required roles not found. Skip seeding");
            return;
        }

        var now = OffsetDateTime.now();
        var hashedPassword = passwordEncoderPort.hash(SEED_PASSWORD);

        var school1 = schoolRepository.save(School.create(
            "LQD",
            "THPT Le Quy Don",
            "Le Quy Don High School - Ho Chi Minh City",
            "0904000001",
            "contact@lequydon.edu.vn",
            "lequydon.edu.vn",
            "123 Le Quy Don, District 3, Ho Chi Minh City",
            1200,
            DEFAULT_SCHOOL_CREATED_BY_ID,
            now
        ));
        var school2 = schoolRepository.save(School.create(
            "NHH",
            "THPT Nguyen Hue",
            "Nguyen Hue High School - Ho Chi Minh City",
            "0904000002",
            "contact@nguyenhue.edu.vn",
            "nguyenhue.edu.vn",
            "456 Nguyen Hue, District 1, Ho Chi Minh City",
            1000,
            DEFAULT_SCHOOL_CREATED_BY_ID,
            now
        ));
        var school3 = schoolRepository.save(School.create(
            "THD",
            "THPT Tran Hung Dao",
            "Tran Hung Dao High School - Ho Chi Minh City",
            "0904000003",
            "contact@tranhungdao.edu.vn",
            "tranhungdao.edu.vn",
            "789 Tran Hung Dao, District 5, Ho Chi Minh City",
            800,
            DEFAULT_SCHOOL_CREATED_BY_ID,
            now
        ));

        var admin1 = saveUser("admin1@vox.local", hashedPassword, "0901000001", "Nguyen Van Admin", Gender.MALE,
            LocalDate.of(1985, 1, 15), "Ho Chi Minh City", UserStatus.ACTIVE, null, now);
        saveSchoolUser(admin1.getId(), school1.getId(), now);
        assignRole(admin1, schoolAdminRole, now);
        admin1 = linkAdminOwnership(admin1, school1, now);

        var admin2 = saveUser("admin2@vox.local", hashedPassword, "0901000002", "Tran Thi Quan", Gender.FEMALE,
            LocalDate.of(1988, 3, 20), "Ho Chi Minh City", UserStatus.ACTIVE, null, now);
        saveSchoolUser(admin2.getId(), school2.getId(), now);
        assignRole(admin2, schoolAdminRole, now);
        admin2 = linkAdminOwnership(admin2, school2, now);

        var admin3 = saveUser("admin3@vox.local", hashedPassword, "0901000003", "Le Van Tri", Gender.MALE,
            LocalDate.of(1982, 7, 10), "Ho Chi Minh City", UserStatus.ACTIVE, null, now);
        saveSchoolUser(admin3.getId(), school3.getId(), now);
        assignRole(admin3, schoolAdminRole, now);
        admin3 = linkAdminOwnership(admin3, school3, now);

        var school1Creators = new ArrayList<User>();
        var school2Creators = new ArrayList<User>();
        var school3Creators = new ArrayList<User>();
        school1Creators.add(admin1);
        school2Creators.add(admin2);
        school3Creators.add(admin3);

        var teachers = List.of(
            teacherData("teacher1@vox.local", "0902000001", "Pham Minh Tuan", Gender.MALE, LocalDate.of(1990, 2, 14), school1.getId()),
            teacherData("teacher2@vox.local", "0902000002", "Hoang Thi Lan", Gender.FEMALE, LocalDate.of(1992, 5, 22), school1.getId()),
            teacherData("teacher3@vox.local", "0902000003", "Vu Duc Phong", Gender.MALE, LocalDate.of(1991, 8, 30), school1.getId()),
            teacherData("teacher4@vox.local", "0902000004", "Do Thi Hang", Gender.FEMALE, LocalDate.of(1993, 1, 8), school2.getId()),
            teacherData("teacher5@vox.local", "0902000005", "Bui Van Khang", Gender.MALE, LocalDate.of(1989, 11, 17), school2.getId()),
            teacherData("teacher6@vox.local", "0902000006", "Ngo Thanh Thao", Gender.FEMALE, LocalDate.of(1994, 4, 5), school2.getId()),
            teacherData("teacher7@vox.local", "0902000007", "Dinh Quang Hai", Gender.MALE, LocalDate.of(1990, 9, 12), school3.getId()),
            teacherData("teacher8@vox.local", "0902000008", "Ly Phuong Mai", Gender.FEMALE, LocalDate.of(1995, 6, 28), school3.getId()),
            teacherData("teacher9@vox.local", "0902000009", "Trinh Van Dung", Gender.MALE, LocalDate.of(1987, 12, 3), school3.getId()),
            teacherData("teacher10@vox.local", "0902000010", "Ho Thi Bich", Gender.FEMALE, LocalDate.of(1996, 3, 19), school3.getId())
        );

        for (var teacherData : teachers) {
            var creatorId = creatorIdForSchool(teacherData.schoolId(), school1, admin1, school2, admin2, school3, admin3);
            var teacher = saveUser(
                teacherData.email(),
                hashedPassword,
                teacherData.phone(),
                teacherData.fullName(),
                teacherData.gender(),
                teacherData.dob(),
                "Ho Chi Minh City",
                UserStatus.ACTIVE,
                creatorId,
                now
            );
            saveSchoolUser(teacher.getId(), teacherData.schoolId(), now);
            assignRole(teacher, teacherRole, now);
            addCreatorBySchool(teacher, teacherData.schoolId(), school1, school1Creators, school2, school2Creators, school3, school3Creators);
        }

        var students = List.of(
            studentData("student1@vox.local", "0903000001", "Nguyen Gia Bao", Gender.MALE, LocalDate.of(2008, 4, 10), school1.getId()),
            studentData("student2@vox.local", "0903000002", "Tran Thi Cam Tu", Gender.FEMALE, LocalDate.of(2008, 9, 25), school1.getId()),
            studentData("student3@vox.local", "0903000003", "Le Hoang Nam", Gender.MALE, LocalDate.of(2007, 1, 15), school2.getId()),
            studentData("student4@vox.local", "0903000004", "Pham Thi Thuy Linh", Gender.FEMALE, LocalDate.of(2008, 7, 7), school2.getId()),
            studentData("student5@vox.local", "0903000005", "Huynh Tan Phat", Gender.MALE, LocalDate.of(2007, 11, 30), school2.getId()),
            studentData("student6@vox.local", "0903000006", "Vo Thi Kim Ngan", Gender.FEMALE, LocalDate.of(2008, 2, 18), school3.getId()),
            studentData("student7@vox.local", "0903000007", "Dang Minh Khoa", Gender.MALE, LocalDate.of(2007, 6, 12), school3.getId())
        );

        for (var studentData : students) {
            var creatorId = creatorIdForSchool(studentData.schoolId(), school1, admin1, school2, admin2, school3, admin3);
            var student = saveUser(
                studentData.email(),
                hashedPassword,
                studentData.phone(),
                studentData.fullName(),
                studentData.gender(),
                studentData.dob(),
                "Ho Chi Minh City",
                UserStatus.ACTIVE,
                creatorId,
                now
            );
            saveSchoolUser(student.getId(), studentData.schoolId(), now);
            assignRole(student, studentRole, now);
        }

        var systemAdmin = findSystemAdminUser(systemAdminRole);
        var english = getOrCreateEnglishLanguage(now, systemAdmin.getId());

        seedSystemQuestionData(english.getId(), systemAdmin, now);
        seedSchoolQuestionData(english.getId(), school1, school1Creators, now);
        seedSchoolQuestionData(english.getId(), school2, school2Creators, now);
        seedSchoolQuestionData(english.getId(), school3, school3Creators, now);

        LOGGER.info("Seed data initialized successfully: 3 schools, 3 admins, 10 teachers, 7 students, 7 question banks, 29 topics, 200 questions");
    }

    private User saveUser(String email, String passwordHash, String phone, String fullName,
            Gender gender, LocalDate dob, String address, UserStatus status,
            UUID createdBy, OffsetDateTime now) {
        return userRepository.save(new User(
            new Email(email),
            passwordHash,
            new Phone(phone),
            new FullName(fullName),
            gender,
            new DateOfBirth(dob),
            address,
            null,
            status,
            now,
            now,
            createdBy,
            createdBy
        ));
    }

    private void saveSchoolUser(UUID userId, UUID schoolId, OffsetDateTime now) {
        schoolUserRepository.save(SchoolUser.create(userId, schoolId, now, null));
    }

    private void assignRole(User user, Role role, OffsetDateTime now) {
        userRoleRepository.save(new UserRole(user.getId(), role.getId(), now));
    }

    private User linkAdminOwnership(User admin, School school, OffsetDateTime now) {
        admin.setCreatedBy(admin.getId());
        admin.setUpdatedBy(admin.getId());
        admin.setUpdatedAt(now);
        var savedAdmin = userRepository.save(admin);

        school.setCreatedBy(savedAdmin.getId());
        school.setUpdatedBy(savedAdmin.getId());
        school.setUpdatedAt(now);
        schoolRepository.save(school);

        return savedAdmin;
    }

    private UUID creatorIdForSchool(UUID schoolId, School school1, User admin1, School school2, User admin2, School school3, User admin3) {
        if (school1.getId().equals(schoolId)) {
            return admin1.getId();
        }
        if (school2.getId().equals(schoolId)) {
            return admin2.getId();
        }
        if (school3.getId().equals(schoolId)) {
            return admin3.getId();
        }
        return DEFAULT_SCHOOL_CREATED_BY_ID;
    }

    private void addCreatorBySchool(
            User creator,
            UUID schoolId,
            School school1,
            List<User> school1Creators,
            School school2,
            List<User> school2Creators,
            School school3,
            List<User> school3Creators) {
        if (school1.getId().equals(schoolId)) {
            school1Creators.add(creator);
            return;
        }
        if (school2.getId().equals(schoolId)) {
            school2Creators.add(creator);
            return;
        }
        if (school3.getId().equals(schoolId)) {
            school3Creators.add(creator);
        }
    }

    private User findSystemAdminUser(Role systemAdminRole) {
        return userRoleRepository.findByRoleId(systemAdminRole.getId())
            .stream()
            .findFirst()
            .flatMap(userRole -> userRepository.findById(userRole.getUserId()))
            .orElseThrow(() -> new IllegalStateException("System admin user not found"));
    }

    private SupportedLanguage getOrCreateEnglishLanguage(OffsetDateTime now, UUID createdBy) {
        return supportedLanguageRepository.findByCode(ENGLISH_CODE)
            .orElseGet(() -> supportedLanguageRepository.save(new SupportedLanguage(
                new LanguageCode(ENGLISH_CODE),
                "English",
                "English language for seeded question banks",
                true,
                now,
                now,
                createdBy,
                createdBy
            )));
    }

    private void seedSystemQuestionData(UUID languageId, User systemAdmin, OffsetDateTime now) {
        var bank = createPublishedQuestionBank(
            languageId,
            null,
            "SYS_BANK_01",
            "System Seed Question Bank",
            "System-owned seeded question bank",
            QuestionBankOwnerType.SYSTEM,
            systemAdmin.getId(),
            now
        );

        var topicCounts = distributeEvenly(SYSTEM_SEED_QUESTION_COUNT, 2);
        var totalQuestionIndex = 0;
        for (int topicIndex = 0; topicIndex < 2; topicIndex++) {
            var topic = createPublishedQuestionTopic(
                bank.getId(),
                "SYS_TOPIC_" + String.format("%02d", topicIndex + 1),
                "System Topic " + (topicIndex + 1),
                "Seeded system topic " + (topicIndex + 1),
                systemAdmin.getId(),
                now
            );
            for (int questionIndex = 0; questionIndex < topicCounts.get(topicIndex); questionIndex++) {
                totalQuestionIndex++;
                createSeedQuestion(
                    topic,
                    systemAdmin,
                    "SYSQ" + String.format("%03d", totalQuestionIndex),
                    "System",
                    totalQuestionIndex,
                    now.plusSeconds(totalQuestionIndex)
                );
            }
        }
    }

    private void seedSchoolQuestionData(UUID languageId, School school, List<User> creators, OffsetDateTime now) {
        if (creators.isEmpty()) {
            return;
        }

        var bankAOwner = creators.get(0);
        var bankBOwner = creators.get(Math.min(1, creators.size() - 1));

        var bankA = createPublishedQuestionBank(
            languageId,
            school.getId(),
            school.getCode().value() + "_BANK_01",
            school.getName() + " Bank 1",
            "Seeded bank 1 for " + school.getName(),
            QuestionBankOwnerType.SCHOOL,
            bankAOwner.getId(),
            now
        );
        var bankB = createPublishedQuestionBank(
            languageId,
            school.getId(),
            school.getCode().value() + "_BANK_02",
            school.getName() + " Bank 2",
            "Seeded bank 2 for " + school.getName(),
            QuestionBankOwnerType.SCHOOL,
            bankBOwner.getId(),
            now
        );

        var topicsPerBank = List.of(
            new BankTopicPlan(bankA, 4),
            new BankTopicPlan(bankB, 5)
        );
        var topicQuestionCounts = distributeEvenly(creators.size() * SCHOOL_QUESTION_COUNT_PER_CREATOR, 9);
        var creatorCursor = 0;
        var topicCursor = 0;
        var globalQuestionCounter = 0;

        for (var plan : topicsPerBank) {
            for (int topicOffset = 0; topicOffset < plan.topicCount(); topicOffset++) {
                var topicOwner = creators.get(topicCursor % creators.size());
                var topicCode = plan.bank().getCode() + "_TOPIC_" + String.format("%02d", topicOffset + 1);
                var topic = createPublishedQuestionTopic(
                    plan.bank().getId(),
                    topicCode,
                    school.getName() + " Topic " + (topicCursor + 1),
                    "Seeded topic " + (topicCursor + 1) + " for " + school.getName(),
                    topicOwner.getId(),
                    now.plusMinutes(topicCursor)
                );

                var questionsInTopic = topicQuestionCounts.get(topicCursor);
                for (int localIndex = 0; localIndex < questionsInTopic; localIndex++) {
                    var creator = creators.get(creatorCursor % creators.size());
                    creatorCursor++;
                    globalQuestionCounter++;
                    createSeedQuestion(
                        topic,
                        creator,
                        topic.getCode() + "_Q" + String.format("%02d", localIndex + 1),
                        school.getCode().value(),
                        globalQuestionCounter,
                        now.plusSeconds(globalQuestionCounter)
                    );
                }
                topicCursor++;
            }
        }
    }

    private QuestionBank createPublishedQuestionBank(
            UUID languageId,
            UUID schoolId,
            String code,
            String name,
            String description,
            QuestionBankOwnerType ownerType,
            UUID createdBy,
            OffsetDateTime now) {
        var bank = QuestionBank.create(languageId, schoolId, code, name, description, ownerType, now, createdBy);
        bank.setStatus(QuestionBankStatus.PUBLISHED);
        bank.setUpdatedAt(now);
        bank.setUpdatedBy(createdBy);
        return questionBankRepository.save(bank);
    }

    private QuestionTopic createPublishedQuestionTopic(
            UUID bankId,
            String code,
            String name,
            String description,
            UUID createdBy,
            OffsetDateTime now) {
        var topic = new QuestionTopic(
            bankId,
            code,
            name,
            description,
            QuestionTopicStatus.PUBLISHED,
            now,
            now,
            createdBy,
            createdBy
        );
        return questionTopicRepository.save(topic);
    }

    private void createSeedQuestion(
            QuestionTopic topic,
            User creator,
            String code,
            String label,
            int ordinal,
            OffsetDateTime now) {
        var type = QuestionType.values()[ordinal % QuestionType.values().length];
        var scope = scopeForIndex(ordinal);
        var visibility = visibilityFor(scope, ordinal);
        var questionPrompt = "Seed question " + ordinal + " for " + label + ": " + questionTextFor(type, ordinal);
        var question = Question.create(
            topic.getId(),
            code,
            "Read the prompt carefully before responding.",
            questionPrompt,
            "Use clear pronunciation and organize your answer logically.",
            "Take a moment to plan your answer with key points.",
            type,
            20 + (ordinal % 4) * 10,
            30 + (ordinal % 3) * 15,
            90 + (ordinal % 4) * 30,
            scope,
            visibility,
            null,
            ordinal % 10 == 0,
            now,
            creator.getId()
        );
        question.setStatus(statusFor(scope, ordinal));
        question.setUpdatedAt(now);
        question.setUpdatedBy(creator.getId());
        var savedQuestion = questionRepository.save(question);
        seedEvaluationGuide(savedQuestion.getId(), type, label, ordinal);
        seedAssets(savedQuestion.getId(), type, scope, label, ordinal);
    }

    private void seedEvaluationGuide(UUID questionId, QuestionType type, String label, int ordinal) {
        var guide = new QuestionEvaluationGuide(
            questionId,
            "The response should address the prompt for " + label + " question " + ordinal + " with a complete and relevant answer.",
            keyPointsFor(type, ordinal),
            "Accept responses that stay on topic, are understandable, and provide enough detail for the question type.",
            "Reject responses that ignore the prompt, contain unrelated memorized content, or do not attempt the task.",
            "Reward structure, fluency, relevance, and development. Allow minor language errors if meaning remains clear.",
            commonMistakesFor(type)
        );
        questionEvaluationGuideRepository.save(guide);
    }

    private void seedAssets(UUID questionId, QuestionType type, QuestionScope scope, String label, int ordinal) {
        var assets = new ArrayList<QuestionAsset>();

        if (type == QuestionType.READ_ALOUD) {
            assets.add(createAsset(
                questionId,
                "Reading Passage " + ordinal,
                QuestionAssetType.TEXT_PASSAGE,
                "https://seed.vox.local/assets/text/" + ordinal,
                "A short reading passage for " + label + " question " + ordinal + ".",
                null,
                "Learners read the passage aloud with natural pacing and clear articulation.",
                1
            ));
        }

        if (type == QuestionType.DESCRIPTION || ordinal % 4 == 0) {
            assets.add(createAsset(
                questionId,
                "Visual Prompt " + ordinal,
                QuestionAssetType.IMAGE,
                "https://seed.vox.local/assets/image/" + ordinal + ".jpg",
                null,
                "Illustration for " + label + " question " + ordinal,
                "A contextual image that supports the speaking prompt.",
                assets.size() + 1
            ));
        }

        if (scope == QuestionScope.CLASSROOM_ASSESSMENT || type == QuestionType.SHORT_ANSWER) {
            assets.add(createAsset(
                questionId,
                "Audio Cue " + ordinal,
                QuestionAssetType.AUDIO,
                "https://seed.vox.local/assets/audio/" + ordinal + ".mp3",
                "Listen to the short prompt and respond clearly.",
                null,
                "Short classroom listening cue for follow-up speaking.",
                assets.size() + 1
            ));
        }

        if (scope == QuestionScope.CENTRAL_EXAM_PAPER && ordinal % 3 == 0) {
            assets.add(createAsset(
                questionId,
                "Exam Scenario Clip " + ordinal,
                QuestionAssetType.VIDEO,
                "https://seed.vox.local/assets/video/" + ordinal + ".mp4",
                "A short scenario clip introducing the exam speaking situation.",
                null,
                "Video prompt used for exam-style seeded content.",
                assets.size() + 1
            ));
        }

        if (!assets.isEmpty()) {
            questionAssetRepository.saveAll(assets);
        }
    }

    private QuestionAsset createAsset(
            UUID questionId,
            String title,
            QuestionAssetType type,
            String url,
            String transcript,
            String altText,
            String description,
            int order) {
        return new QuestionAsset(
            questionId,
            title,
            type == QuestionAssetType.AUDIO || type == QuestionAssetType.VIDEO ? 20 + order * 10 : null,
            altText,
            type,
            url,
            transcript,
            description,
            order
        );
    }

    private String keyPointsFor(QuestionType type, int ordinal) {
        return switch (type) {
            case READ_ALOUD -> "Pronunciation, stress, pacing, and clear delivery for passage " + ordinal + ".";
            case SHORT_ANSWER -> "Direct answer, one or two supporting details, and understandable expression.";
            case LONG_ANSWER -> "Clear structure, expanded ideas, supporting reasons, and coherent conclusion.";
            case OPINION -> "Opinion stated clearly, reasons explained, and examples used effectively.";
            case DESCRIPTION -> "Relevant details, logical order, and accurate description of visible information.";
        };
    }

    private String commonMistakesFor(QuestionType type) {
        return switch (type) {
            case READ_ALOUD -> "Skipping words, monotone reading, unclear endings, and broken rhythm.";
            case SHORT_ANSWER -> "Answering too briefly, missing the main point, or drifting off topic.";
            case LONG_ANSWER -> "Listing ideas without development, weak transitions, or incomplete conclusion.";
            case OPINION -> "Opinion without support, repetitive reasons, or unclear examples.";
            case DESCRIPTION -> "Naming isolated items only, missing relationships, or disorganized sequence.";
        };
    }

    private QuestionScope scopeForIndex(int ordinal) {
        return QuestionScope.values()[ordinal % QuestionScope.values().length];
    }

    private QuestionVisibility visibilityFor(QuestionScope scope, int ordinal) {
        return switch (scope) {
            case QUESTION_BANK -> switch (ordinal % 3) {
                case 0 -> QuestionVisibility.BANK_VISIBLE;
                case 1 -> QuestionVisibility.AUTHOR_ONLY;
                default -> QuestionVisibility.REVIEWER_ONLY;
            };
            case CLASSROOM_ASSESSMENT -> QuestionVisibility.ASSESSMENT_ONLY;
            case CENTRAL_EXAM_DRAFT -> switch (ordinal % 3) {
                case 0 -> QuestionVisibility.AUTHOR_ONLY;
                case 1 -> QuestionVisibility.REVIEWER_ONLY;
                default -> QuestionVisibility.BANK_VISIBLE;
            };
            case CENTRAL_EXAM_PAPER -> QuestionVisibility.EXAM_PAPER_ONLY;
        };
    }

    private QuestionStatus statusFor(QuestionScope scope, int ordinal) {
        return switch (scope) {
            case QUESTION_BANK -> switch (ordinal % 5) {
                case 0 -> QuestionStatus.PUBLISHED;
                case 1 -> QuestionStatus.DRAFT;
                case 2 -> QuestionStatus.SUBMITTED_FOR_REVIEW;
                case 3 -> QuestionStatus.APPROVED;
                default -> QuestionStatus.REVISION_REQUESTED;
            };
            case CLASSROOM_ASSESSMENT -> ordinal % 2 == 0 ? QuestionStatus.DRAFT : QuestionStatus.PUBLISHED;
            case CENTRAL_EXAM_DRAFT -> switch (ordinal % 4) {
                case 0 -> QuestionStatus.DRAFT;
                case 1 -> QuestionStatus.SUBMITTED_FOR_REVIEW;
                case 2 -> QuestionStatus.APPROVED;
                default -> QuestionStatus.PUBLISHED;
            };
            case CENTRAL_EXAM_PAPER -> ordinal % 2 == 0 ? QuestionStatus.DRAFT : QuestionStatus.PUBLISHED;
        };
    }

    private String questionTextFor(QuestionType type, int ordinal) {
        return switch (type) {
            case READ_ALOUD -> "Read the short passage aloud with natural pacing " + ordinal + ".";
            case SHORT_ANSWER -> "Answer the interview question in two or three sentences " + ordinal + ".";
            case LONG_ANSWER -> "Provide a detailed explanation with supporting reasons " + ordinal + ".";
            case OPINION -> "Give your opinion and justify it with examples " + ordinal + ".";
            case DESCRIPTION -> "Describe the given scene or situation clearly " + ordinal + ".";
        };
    }

    private List<Integer> distributeEvenly(int total, int bucketCount) {
        var distribution = new ArrayList<Integer>(bucketCount);
        var base = total / bucketCount;
        var remainder = total % bucketCount;
        for (int index = 0; index < bucketCount; index++) {
            distribution.add(base + (index < remainder ? 1 : 0));
        }
        return distribution;
    }

    private TeacherData teacherData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {
        return new TeacherData(email, phone, fullName, gender, dob, schoolId);
    }

    private StudentData studentData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {
        return new StudentData(email, phone, fullName, gender, dob, schoolId);
    }

    private record TeacherData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {}
    private record StudentData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {}
    private record BankTopicPlan(QuestionBank bank, int topicCount) {}
}
