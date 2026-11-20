package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.usecase.scoringrule.SearchSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.SearchSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewAllSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewAllSystemScoringRuleUseCase;

/**
 * A missing or non-positive page or size used to be replaced by page 1 and size 10; it is now rejected.
 */
@ExtendWith(MockitoExtension.class)
class ScoringRuleControllerTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();

    @Mock
    private SearchSystemScoringRuleUseCase searchSystemScoringRuleUseCase;
    @Mock
    private SearchSchoolScoringRuleUseCase searchSchoolScoringRuleUseCase;
    @Mock
    private ViewAllSystemScoringRuleUseCase viewAllSystemScoringRuleUseCase;
    @Mock
    private ViewAllSchoolScoringRuleUseCase viewAllSchoolScoringRuleUseCase;

    @InjectMocks
    private ScoringRuleController controller;

    @Test
    void search_system_scoring_rules_should_reject_page_zero_and_missing_page() {
        assertThatThrownBy(() -> controller.searchSystemScoringRules(policyId, null, null, 0, 10))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.searchSystemScoringRules(policyId, null, null, null, 10))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(searchSystemScoringRuleUseCase);
    }

    @Test
    void search_school_scoring_rules_should_reject_page_zero_and_missing_size() {
        assertThatThrownBy(() -> controller.searchSchoolScoringRules(schoolId, policyId, null, null, 0, 10))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.searchSchoolScoringRules(schoolId, policyId, null, null, 1, null))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(searchSchoolScoringRuleUseCase);
    }

    @Test
    void view_all_system_scoring_rules_should_reject_page_zero_and_missing_page() {
        assertThatThrownBy(() -> controller.viewAllSystemScoringRules(policyId, 0, 10))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.viewAllSystemScoringRules(policyId, null, 10))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewAllSystemScoringRuleUseCase);
    }

    @Test
    void view_all_school_scoring_rules_should_reject_page_zero_and_missing_page() {
        assertThatThrownBy(() -> controller.viewAllSchoolScoringRules(schoolId, policyId, 0, 10))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.viewAllSchoolScoringRules(schoolId, policyId, null, 10))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewAllSchoolScoringRuleUseCase);
    }
}
