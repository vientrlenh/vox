package com.sep.vox.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Blocks dependencies that point the wrong way between layers. The CORRECT direction is from the outside in:
 *
 * <pre>
 *   interfaces (presentation) ──┐
 *                               ├──> application ──> domain
 *   infrastructure ─────────────┘
 * </pre>
 *
 * <p>That means domain must know nothing about the other three layers, and application must not know about
 * infrastructure/interfaces. To let application call out to the world (HTTP client, .env config, queues, ...),
 * declare an interface in application/port/output and let infrastructure implement it -- see
 * QuotaPricingPort/QuotaPricingService as the model.
 *
 * <p>What each layer may use from the outside world (infrastructure and interfaces have no restriction):
 *
 * <ul>
 *   <li><b>domain</b>: only plain Java ({@code java..}) and domain itself. No Spring, Lombok, Jackson or any other
 *       library.
 *   <li><b>application</b>: plain Java, domain, application, slf4j, {@code @Service} and the
 *       {@code org.springframework.transaction.annotation} package ({@code @Transactional} and its enums). Lombok
 *       is guarded by {@link LombokUsageConventionTests}, because its annotations leave no trace in the bytecode.
 * </ul>
 *
 * <p>This test runs on bytecode, so it also catches references that do not go through an import (fully qualified
 * names, types in a method signature, annotations, generics, ...), not only import lines.
 */
@AnalyzeClasses(packages = "com.sep.vox", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTests {

    private static final String DOMAIN = "com.sep.vox.domain..";
    private static final String APPLICATION = "com.sep.vox.application..";
    private static final String INFRASTRUCTURE = "com.sep.vox.infrastructure..";
    private static final String INTERFACES = "com.sep.vox.interfaces..";

    private static final DescribedPredicate<JavaClass> APPLICATION_ALLOWED_DEPENDENCY = JavaClass.Predicates
        .resideInAnyPackage("java..", DOMAIN, APPLICATION, "org.slf4j..", "org.springframework.transaction.annotation..")
        .or(JavaClass.Predicates.belongToAnyOf(Service.class))
        .as("are plain Java, domain, application, slf4j, @Service or @Transactional");

    /**
     * Application classes that still depend on something outside the allowed set, by top-level class name. This is
     * known debt, not a permission: the list may only shrink. A new violation fails the build, and a class that no
     * longer violates must be removed from here (see {@link #applicationExemptionsStillApply}).
     */
    private static final Set<String> APPLICATION_EXEMPTIONS = Set.of(
        // Catches org.springframework.dao.DataIntegrityViolationException to map it to DuplicatedException.
        "BulkCreateSchoolClassUsersUseCase", "CreateFrameworkCriteriaUseCase", "CreateFrameworkCriterionBandsUseCase",
        "CreateFrameworkResultBandsUseCase", "CreateFrameworkUseCase", "CreateFrameworkVersionUseCase",
        "CreateGradeLevelUseCase", "CreateSchoolClassUserUseCase", "CreateSchoolGradeUseCase",
        "CreateSchoolRubricCriterionUseCase", "CreateSchoolRubricResultBandsUseCase",
        "CreateSchoolUserUseCase", "CreateSupportedLanguageUseCase", "CreateSystemRubricCriteriaUseCase", "CreateSystemRubricResultBandsUseCase",
        "UpdateFrameworkCriterionUseCase", "UpdateFrameworkResultBandUseCase", "UpdateGradeLevelUseCase",
        "UpdateSchoolRubricResultBandUseCase", "UpdateSchoolRubricVersionUseCase", "UpdateSupportedLanguageUseCase",
        "UpdateSystemRubricResultBandUseCase", "UpdateSystemRubricVersionUseCase", "ViewSchoolRubricDetailsUseCase",
        "ViewSubscriptionPlansUseCase",
        // DataIntegrityViolationException plus PlatformTransactionManager/TransactionTemplate (commit handlers).
        "QuestionImportCommitHandler", "RubricCriterionImportCommitHandler", "RubricResultBandImportCommitHandler",
        "SchoolClassImportCommitHandler", "SchoolClassUserImportCommitHandler", "SchoolDirectoryImportCommitHandler",
        "SchoolGradeImportCommitHandler", "SchoolRoomImportCommitHandler", "SchoolUserImportCommitHandler",
        // DataIntegrityViolationException plus @Value or Jackson.
        "CreateSchoolUseCase", "UpdateProfileUseCase", "UpdateSchoolUserUseCase",
        "UpdateSchoolRubricCriterionUseCase", "UpdateSystemRubricCriterionUseCase",
        // PlatformTransactionManager/TransactionTemplate for manual transaction control.
        "RecordAssessmentAttemptEvaluationUseCase", "SubmitPracticeTurnUseCase", "CreatePaymentCheckoutUrlUseCase",
        // @Value, @Qualifier, AsyncTaskExecutor, ApplicationEventPublisher.
        "PracticeTopicOfferEnrichmentService", "RecordRecordingPartChangedUseCase", "PracticePaperDraftService",
        "PracticeSessionClosedHandler",
        // @Component (a mapper or guard registered as a bean).
        "ImportRowResponseMapper", "ImportSessionResponseMapper", "MyClassAccessGuard",
        // Jackson (tools.jackson) used directly.
        "PracticeQuestionGenerationPort", "PracticeQuestionSelectionService", "ResolveNextPracticeQuestionUseCase",
        // Apache POI used directly for Excel import/export.
        "ExportAssessmentScoresExcelUseCase", "QuestionSpreadsheetService",
        // @Async.
        "InterestQuizBankTopUpService", "TopicOfferBackfillService",
        // Servlet, validation or security API on an application type, and Lombok-generated code.
        "CookieManagerPort", "SendRegisterFormDocumentCommand", "UpdateSchoolRoomCommand");

    @ArchTest
    static final ArchRule domainKhongDuocPhuThuocTangNgoai = noClasses()
        .that().resideInAPackage(DOMAIN)
        .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE, INTERFACES)
        .because("domain is the innermost layer -- it holds only business models and rules and must not know about"
            + " use cases, frameworks or any other technical detail");

    @ArchTest
    static final ArchRule domainOnlyDependsOnJavaAndDomain = classes()
        .that().resideInAPackage(DOMAIN)
        .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN)
        .because("domain is plain Java: no Spring, Jackson, JPA or any other framework or library type");

    @ArchTest
    static final ArchRule applicationKhongDuocPhuThuocTangNgoai = noClasses()
        .that().resideInAPackage(APPLICATION)
        .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, INTERFACES)
        .because("application may only depend on domain and on the ports it declares itself;"
            + " everything that reaches outside must go through an interface in application/port/output");

    @ArchTest
    static final ArchRule applicationOnlyDependsOnAllowedPackages = classes()
        .that().resideInAPackage(APPLICATION)
        .and(DescribedPredicate.describe("are not listed in the exemptions", javaClass ->
            !APPLICATION_EXEMPTIONS.contains(topLevelSimpleName(javaClass))))
        .should().onlyDependOnClassesThat(APPLICATION_ALLOWED_DEPENDENCY)
        .because("application may only use @Service, @Transactional and logging from the outside world;"
            + " anything else (Jackson, POI, Spring DAO or scheduling types, ...) goes behind a port in"
            + " application/port/output");

    @ArchTest
    static final ArchRule interfacesKhongDuocPhuThuocInfrastructure = noClasses()
        .that().resideInAPackage(INTERFACES)
        .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE)
        .because("interfaces and infrastructure are both adapters on the outer ring -- if one adapter calls the other"
            + " directly the two entry/exit paths become glued together; a controller must go through a use case/port");

    @ArchTest
    static void applicationExemptionsStillApply(JavaClasses importedClasses) {
        Set<String> stillViolating = importedClasses.stream()
            .filter(javaClass -> javaClass.getPackageName().startsWith("com.sep.vox.application"))
            .filter(LayeredArchitectureTests::dependsOnDisallowedType)
            .map(LayeredArchitectureTests::topLevelSimpleName)
            .collect(Collectors.toSet());
        Set<String> stale = new TreeSet<String>(APPLICATION_EXEMPTIONS);
        stale.removeAll(stillViolating);

        assertThat(stale)
            .as("exempted application classes that no longer break the allowed-dependency rule:"
                + " remove them from APPLICATION_EXEMPTIONS")
            .isEmpty();
    }

    private static boolean dependsOnDisallowedType(JavaClass javaClass) {
        return javaClass.getDirectDependenciesFromSelf().stream()
            .anyMatch(dependency -> !APPLICATION_ALLOWED_DEPENDENCY.test(dependency.getTargetClass()));
    }

    private static String topLevelSimpleName(JavaClass javaClass) {
        String fullName = javaClass.getName();
        String withoutPackage = fullName.substring(fullName.lastIndexOf('.') + 1);
        int nestedSeparator = withoutPackage.indexOf('$');
        return nestedSeparator < 0 ? withoutPackage : withoutPackage.substring(0, nestedSeparator);
    }
}
