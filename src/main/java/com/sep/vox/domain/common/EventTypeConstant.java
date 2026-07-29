package com.sep.vox.domain.common;

import java.util.List;

public final class EventTypeConstant {

    private EventTypeConstant() {}
    
    public static final String REGISTER_FORM_REJECTED = "RegisterFormRejected";
    public static final String USER_CREATED = "UserCreated";

    public static List<String> all() {
        return List.of(REGISTER_FORM_REJECTED, USER_CREATED);
    }
}
