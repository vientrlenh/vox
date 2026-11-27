package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

/**
 * The validator runs exactly once at startup, so if its logic is wrong-way-round (filters the wrong
 * set, compares the wrong table) the consequence is that the app still comes up normally and checks nothing -- silent,
 * exactly as if there were no validator. This test calls the bean directly with the three real mapping tables.
 *
 * <p>There is no test for the throwing branch: all three tables are {@code static final}, making them
 * disagree needs reflection, and the real trap -- someone adds a new event but forgets one table
 * -- is already caught by {@code NotificationPushedEventConsumerTests} with a direct comparison of the two sets of
 * eventTypes.
 */
class NotificationCategoryMappingValidatorTests {

    @Test
    void should_pass_with_the_real_mapping_tables() {
        InitializingBean checker = new NotificationCategoryMappingValidator().notificationCategoryMappingChecker();

        assertThatCode(checker::afterPropertiesSet).doesNotThrowAnyException();
    }
}
