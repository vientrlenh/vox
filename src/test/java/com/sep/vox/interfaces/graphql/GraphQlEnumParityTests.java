package com.sep.vox.interfaces.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import graphql.schema.GraphQLNamedType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.graphql.execution.GraphQlSource;

import graphql.schema.GraphQLEnumType;

import com.sep.vox.support.IntegrationTest;

/**
 * Mọi enum trong SDL phải liệt kê ĐỦ hằng số của enum Java cùng tên.
 *
 * <p>Sinh ra sau một sự cố THẬT: {@code SchoolBalanceEntryType.QUOTA_FUNDING} được thêm vào enum Java
 * cùng V12, nhưng {@code school-balance.graphqls} thì không. Không có gì nổ lúc khởi động, không có
 * test nào đỏ, và bản ghi QUOTA_FUNDING đầu tiên chỉ cần TỒN TẠI là cả trang "Sao kê ví" chết với
 * {@code Can't serialize value ... Unknown value 'QUOTA_FUNDING'} -- hỏng cả trang, không phải hỏng
 * một dòng, vì lỗi serialize giết nguyên response.
 *
 * <p>Đây đúng là loại lỗi không ai bắt được bằng mắt: hai file cách nhau, cùng sửa thì không sao,
 * quên một bên thì im lặng cho tới lúc có dữ liệu thật. Nên chốt bằng một test soi cả hai chiều.
 *
 * <p>Quét theo TÊN ĐƠN GIẢN. Enum SDL không có enum Java cùng tên thì bỏ qua -- nhiều enum của SDL
 * là hình chiếu riêng của tầng đọc (SchoolRiskBucket, AiCostGranularity...) chứ không mirror cái gì.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
class GraphQlEnumParityTests {

    private static final String DOMAIN_PACKAGE = "com.sep.vox.domain.model";

    @Autowired
    private GraphQlSource graphQlSource;

    @Test
    void every_graphql_enum_lists_exactly_the_values_of_the_java_enum_it_mirrors() {
        Map<String, Class<?>> javaEnums = scanDomainEnums();
        Map<String, String> drift = new LinkedHashMap<String, String>();

        for (GraphQLNamedType type : graphQlSource.schema().getAllTypesAsList()) {
            if (!(type instanceof GraphQLEnumType sdlEnum) || sdlEnum.getName().startsWith("__")) {
                continue;
            }
            Class<?> javaEnum = javaEnums.get(sdlEnum.getName());
            if (javaEnum == null) {
                continue;
            }

            TreeSet<String> inJava = Arrays.stream(javaEnum.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .collect(Collectors.toCollection(TreeSet::new));
            TreeSet<String> inSdl = sdlEnum.getValues().stream()
                .map(value -> value.getName())
                .collect(Collectors.toCollection(TreeSet::new));

            // Extra on the Java side -> serialization breaks (exactly the QUOTA_FUNDING case).
            // Extra in the SDL -> clients can send an input value that valueOf() will throw on. The
            // two directions fail differently but share the same cause.
            TreeSet<String> missingFromSdl = new TreeSet<>(inJava);
            missingFromSdl.removeAll(inSdl);
            TreeSet<String> missingFromJava = new TreeSet<>(inSdl);
            missingFromJava.removeAll(inJava);

            if (!missingFromSdl.isEmpty() || !missingFromJava.isEmpty()) {
                drift.put(sdlEnum.getName(),
                    "thiếu trong SDL " + missingFromSdl + ", thừa trong SDL " + missingFromJava);
            }
        }

        assertThat(drift)
            .describedAs("enum SDL lệch với enum Java cùng tên -- thêm giá trị vào file .graphqls tương ứng")
            .isEmpty();
    }

    /**
     * {@code isCandidateComponent} must be relaxed: by default it rejects every abstract class, yet
     * an enum with its own body per constant (e.g. {@code QuotaType}) IS abstract in bytecode.
     * Without relaxing it, exactly the most interesting enums would fall out of this check.
     */
    private static Map<String, Class<?>> scanDomainEnums() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter((TypeFilter) (metadataReader, factory) -> true);

        Map<String, Class<?>> found = new LinkedHashMap<String, Class<?>>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(DOMAIN_PACKAGE)) {
            String className = candidate.getBeanClassName();
            if (className == null) {
                continue;
            }
            try {
                Class<?> loaded = Class.forName(className);
                if (loaded.isEnum()) {
                    found.put(loaded.getSimpleName(), loaded);
                }
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                // Not the job of this test.
            }
        }
        return found;
    }

    @Test
    void the_scan_actually_finds_domain_enums() {
        // Chốt chính phép quét: một ngày nào đó package đổi tên và scanDomainEnums trả rỗng, thì test
        // trên sẽ XANH vì không so gì cả -- im lặng đúng kiểu lỗi nó sinh ra để bắt.
        assertThat(scanDomainEnums())
            .containsKeys("SchoolBalanceEntryType", "QuotaType", "SchoolSubscriptionStatus");
    }

    @Test
    void the_balance_ledger_enum_is_complete_in_the_sdl() {
        GraphQLEnumType sdlEnum = (GraphQLEnumType) graphQlSource.schema().getType("SchoolBalanceEntryType");
        Set<String> inSdl = sdlEnum.getValues().stream()
            .map(value -> value.getName())
            .collect(Collectors.toSet());

        assertThat(inSdl).isEqualTo(
            Set.of("TOP_UP", "OVERAGE_CHARGE", "QUOTA_FUNDING", "REFUND", "ADJUSTMENT"));
    }
}
