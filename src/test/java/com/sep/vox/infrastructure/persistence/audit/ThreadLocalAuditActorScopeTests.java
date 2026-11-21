package com.sep.vox.infrastructure.persistence.audit;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class ThreadLocalAuditActorScopeTests {

    private final ThreadLocalAuditActorScope scope = new ThreadLocalAuditActorScope();

    @Test
    void actorIsSetInsideTheActionAndClearedAfterwards() {
        List<String> seenInside = new ArrayList<>();

        String result = scope.callAs("importer", () -> {
            scope.findCurrentActor().ifPresent(seenInside::add);
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(seenInside).containsExactly("importer");
        assertThat(scope.findCurrentActor()).isEmpty();
    }

    @Test
    void actorIsClearedWhenTheActionThrows() {
        assertThatThrownBy(() -> scope.callAs("importer", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(scope.findCurrentActor()).isEmpty();
    }

    @Test
    void nestedScopeRestoresThePreviousActor() {
        List<String> seenAfterInner = new ArrayList<>();

        scope.callAs("outer", () -> {
            scope.callAs("inner", () -> "ignored");
            scope.findCurrentActor().ifPresent(seenAfterInner::add);
            return null;
        });

        assertThat(seenAfterInner).containsExactly("outer");
        assertThat(scope.findCurrentActor()).isEmpty();
    }
}
