package com.sep.vox.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestcontainersConfiguration;
import com.sep.vox.infrastructure.config.JpaAuditingConfig;
import com.sep.vox.infrastructure.persistence.audit.AuditActorProvider;
import com.sep.vox.infrastructure.persistence.audit.ThreadLocalAuditActorScope;
import com.sep.vox.infrastructure.security.SecurityContextProvider;

/**
 * A JPA slice test against the real Postgres from {@link TestcontainersConfiguration}.
 * Tagged "integration", so it runs in {@code integrationTest} and not in {@code test}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, JpaAuditingConfig.class, AuditActorProvider.class,
        ThreadLocalAuditActorScope.class, SecurityContextProvider.class })
@ActiveProfiles("test")
public @interface RepositoryTest {
}
