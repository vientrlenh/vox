package com.sep.vox.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Chặn phụ thuộc ngược hướng giữa các tầng. Hướng ĐÚNG là từ ngoài vào trong:
 *
 * <pre>
 *   interfaces (presentation) ──┐
 *                               ├──> application ──> domain
 *   infrastructure ─────────────┘
 * </pre>
 *
 * <p>Nghĩa là domain không được biết gì về ba tầng còn lại, và application không được biết về
 * infrastructure/interfaces. Muốn application gọi ra ngoài (HTTP client, config .env, hàng đợi, ...)
 * thì khai báo interface ở application/port/output rồi để infrastructure implement -- xem
 * QuotaPricingPort/QuotaPricingService làm mẫu.
 *
 * <p>Test này chạy trên bytecode nên bắt được cả tham chiếu không qua import (tên đầy đủ, kiểu
 * trong chữ ký method, annotation, generic, ...) chứ không chỉ dòng import.
 */
@AnalyzeClasses(packages = "com.sep.vox", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTests {

    private static final String DOMAIN = "com.sep.vox.domain..";
    private static final String APPLICATION = "com.sep.vox.application..";
    private static final String INFRASTRUCTURE = "com.sep.vox.infrastructure..";
    private static final String INTERFACES = "com.sep.vox.interfaces..";

    @ArchTest
    static final ArchRule domainKhongDuocPhuThuocTangNgoai = noClasses()
        .that().resideInAPackage(DOMAIN)
        .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE, INTERFACES)
        .because("domain là tầng trong cùng -- chỉ chứa model/rule nghiệp vụ, không được biết"
            + " use case, framework hay bất kỳ chi tiết kỹ thuật nào");

    @ArchTest
    static final ArchRule applicationKhongDuocPhuThuocTangNgoai = noClasses()
        .that().resideInAPackage(APPLICATION)
        .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, INTERFACES)
        .because("application chỉ được phụ thuộc vào domain và vào port do chính nó khai báo;"
            + " mọi thứ chạm ra ngoài phải đi qua interface ở application/port/output");

    @ArchTest
    static final ArchRule interfacesKhongDuocPhuThuocInfrastructure = noClasses()
        .that().resideInAPackage(INTERFACES)
        .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE)
        .because("interfaces và infrastructure đều là adapter ở vòng ngoài -- adapter này gọi thẳng"
            + " adapter kia thì hai đường vào/ra dính vào nhau, controller phải qua use case/port");
}
