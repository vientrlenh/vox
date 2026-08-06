package com.sep.vox.domain.valueobject.framework;

import java.util.Set;

public final class FrameworkCriterionCode {

    public static final Set<String> ALLOWED_CODES = Set.of(
            "PRONUNCIATION", "FLUENCY", "GRAMMAR", "VOCABULARY", "COHERENCE");

    private FrameworkCriterionCode() {}
}
