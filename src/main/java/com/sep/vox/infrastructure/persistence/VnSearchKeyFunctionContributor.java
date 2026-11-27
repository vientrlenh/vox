package com.sep.vox.infrastructure.persistence;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Lets JPQL call the SQL function {@code vn_search_key(text)} (created in migration V7).
 *
 * <p>Hibernate rejects every function whose return type it does not know, so without registering it here every JPQL
 * statement containing {@code vn_search_key(...)} fails while the metamodel is built -- that is, at startup
 * and not at query time.
 *
 * <p>Loaded through ServiceLoader: {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 */
public class VnSearchKeyFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
            "vn_search_key",
            "vn_search_key(?1)",
            functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.STRING));
    }
}
