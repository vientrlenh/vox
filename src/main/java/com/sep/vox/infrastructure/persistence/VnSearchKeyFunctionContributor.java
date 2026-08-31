package com.sep.vox.infrastructure.persistence;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Cho JPQL gọi được hàm SQL {@code vn_search_key(text)} (tạo ở migration V7).
 *
 * <p>Hibernate từ chối mọi hàm nó chưa biết kiểu trả về, nên không đăng ký ở đây thì mọi câu JPQL
 * có {@code vn_search_key(...)} sẽ chết ngay lúc dựng metamodel — tức là hỏng lúc khởi động chứ
 * không phải lúc chạy query.
 *
 * <p>Nạp qua ServiceLoader: {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
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
