package com.sep.vox.interfaces.graphql.dataloader;

import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

/**
 * The only place a controller looks a {@link DataLoader} up.
 *
 * <p>{@link #require} throws {@link IllegalStateException} when no loader is registered under the name. That is a
 * wiring fault (a typo or a missing registration) that the client cannot cause or fix, so it is an internal error (500)
 * and not a bad request.
 */
public final class GraphQLDataLoaders {

    private GraphQLDataLoaders() {
    }

    public static <K, V> DataLoader<K, V> require(DataFetchingEnvironment environment, String loaderName) {
        DataLoader<K, V> loader = environment.getDataLoader(loaderName);
        if (loader == null) {
            throw new IllegalStateException("DataLoader not registered: " + loaderName);
        }
        return loader;
    }
}
