package com.sep.vox.interfaces.graphql.dataloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import java.util.UUID;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraphQLDataLoadersTests {

    @Mock
    private DataFetchingEnvironment environment;

    @Mock
    private DataLoader<UUID, String> loader;

    @Test
    void require_returnsTheRegisteredLoader() {
        when(environment.<UUID, String>getDataLoader("userByUserId")).thenReturn(loader);

        DataLoader<UUID, String> result = GraphQLDataLoaders.require(environment, "userByUserId");

        assertThat(result).isSameAs(loader);
    }

    @Test
    void require_throwsIllegalStateExceptionNamingTheLoaderWhenNotRegistered() {
        when(environment.getDataLoader("missingLoader")).thenReturn(null);

        assertThatThrownBy(() -> GraphQLDataLoaders.require(environment, "missingLoader"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missingLoader");
    }
}
