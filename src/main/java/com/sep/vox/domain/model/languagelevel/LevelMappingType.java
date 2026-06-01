package com.sep.vox.domain.model.languagelevel;

public enum LevelMappingType {
    EXACT, // chỉ sử dụng mappedStandardLevelVersionId
    RANGE // chỉ sử dụng mappedStandardLevelMinVersionId + mappedStandardLevelLMaxVersionId
}
