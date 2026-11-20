package com.sep.vox.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Keeps the {@code test} task free of Docker: a test that starts a Spring context or a JPA slice
 * must be tagged "integration" so that only {@code integrationTest} runs it.
 *
 * <p>The check follows meta-annotations, so the composed {@code @IntegrationTest} and
 * {@code @RepositoryTest} satisfy it, and a raw {@code @SpringBootTest} or {@code @DataJpaTest}
 * without the tag fails the build.
 */
@AnalyzeClasses(packages = "com.sep.vox", importOptions = ImportOption.OnlyIncludeTests.class)
class TestSuiteSeparationTests {

    private static final DescribedPredicate<JavaClass> STARTS_SPRING_CONTEXT =
        DescribedPredicate.describe(
            "start a Spring context or a JPA slice",
            javaClass -> javaClass.isMetaAnnotatedWith(SpringBootTest.class)
                || javaClass.isMetaAnnotatedWith(DataJpaTest.class));

    private static final DescribedPredicate<JavaAnnotation<?>> INTEGRATION_TAG =
        DescribedPredicate.describe(
            "@Tag(\"integration\")",
            annotation -> annotation.getRawType().isEquivalentTo(Tag.class)
                && "integration".equals(annotation.get("value").orElse(null)));

    @ArchTest
    static final ArchRule spring_context_tests_are_tagged_integration =
        classes().that(STARTS_SPRING_CONTEXT)
            .should().beMetaAnnotatedWith(INTEGRATION_TAG)
            .because("tests that need Docker or a Spring context must run only in integrationTest");
}
