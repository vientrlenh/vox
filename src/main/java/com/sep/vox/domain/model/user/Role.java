package com.sep.vox.domain.model.user;

import com.sep.vox.domain.model.BaseModel;
import com.sep.vox.domain.valueobject.RoleCode;

public class Role extends BaseModel {
    private RoleCode code;
    private String name;

    public Role() {
    }

    public RoleCode getCode() {
        return code;
    }

    public void setCode(RoleCode code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<Role, Builder> {

        private Builder() {
            super(new Role());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder code(RoleCode code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }
    }
}
