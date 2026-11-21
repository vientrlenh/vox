package com.sep.vox.domain.model;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.sep.vox.domain.common.ActorConstant;

class BaseModelTests {

    private static final UUID USER_ID = UUID.fromString("0198a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b");

    @Test
    void systemActorIsTheTextSystem() {
        assertThat(ActorConstant.SYSTEM).isEqualTo("System");
    }

    @Test
    void builderFillsTheSharedFieldsAndKeepsChainingType() {
        Instant now = Instant.parse("2026-11-21T01:00:00Z");

        SampleModel model = SampleModel.builder()
                .id(USER_ID).createdAt(now).updatedAt(now)
                .createdBy("creator").updatedBy("updater")
                .label("label")
                .build();

        assertThat(model.getId()).isEqualTo(USER_ID);
        assertThat(model.getCreatedAt()).isEqualTo(now);
        assertThat(model.getUpdatedAt()).isEqualTo(now);
        assertThat(model.getCreatedBy()).isEqualTo("creator");
        assertThat(model.getUpdatedBy()).isEqualTo("updater");
        assertThat(model.label).isEqualTo("label");
    }

    @Test
    void actorIdsAreReadAsUserIdsOnlyWhenTheyAreUuidText() {
        SampleModel model = SampleModel.builder().createdBy(USER_ID.toString()).updatedBy("not-a-user").build();

        assertThat(model.createdByUserId()).contains(USER_ID);
        assertThat(model.updatedByUserId()).isEmpty();
    }

    @Test
    void systemAndNullAndMalformedActorsGiveNoUserId() {
        assertThat(SampleModel.builder().createdBy(ActorConstant.SYSTEM).build().createdByUserId()).isEmpty();
        assertThat(SampleModel.builder().build().createdByUserId()).isEmpty();
        assertThat(SampleModel.builder().createdBy("x".repeat(36)).build().createdByUserId()).isEmpty();
    }

    @Test
    void isCreatedByComparesTheUserIdText() {
        SampleModel model = SampleModel.builder().createdBy(USER_ID.toString()).build();

        assertThat(model.isCreatedBy(USER_ID)).isTrue();
        assertThat(model.isCreatedBy(UUID.randomUUID())).isFalse();
        assertThat(model.isCreatedBy(null)).isFalse();
        assertThat(SampleModel.builder().createdBy(ActorConstant.SYSTEM).build().isCreatedBy(USER_ID)).isFalse();
    }

    private static final class SampleModel extends BaseModel {
        private String label;

        static Builder builder() {
            return new Builder();
        }

        static final class Builder extends BaseModel.Builder<SampleModel, Builder> {

            private Builder() {
                super(new SampleModel());
            }

            @Override
            protected Builder self() {
                return this;
            }

            Builder label(String label) {
                target.label = label;
                return this;
            }
        }
    }
}
