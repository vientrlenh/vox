package com.sep.vox.infrastructure.persistence.id;

import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.uuid.UuidVersion7Strategy;

public class AssignedOrUuidVersion7Generator implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue,
                           EventType eventType) {
        if (currentValue != null) {
            return currentValue;
        }
        return UuidVersion7Strategy.INSTANCE.generateUuid(session);
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }
}
