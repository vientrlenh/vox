package com.sep.vox.domain.model.user;

import java.util.Set;


public final class SchoolRoleCodes {

    public static final String STUDENT = "STUDENT";
    public static final String TEACHER = "TEACHER";
    public static final Set<String> ALL = Set.of(STUDENT, TEACHER);

    private SchoolRoleCodes() {}
}
