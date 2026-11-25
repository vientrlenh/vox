package com.sep.vox.infrastructure.persistence.id;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/**
 * Keeps the id an entity already carries and generates a version 7 UUID when it carries none.
 * Used where another system (the AI service) may send the id of a row Vox stores.
 */
@IdGeneratorType(AssignedOrUuidVersion7Generator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AssignedOrUuidVersion7 {
}
