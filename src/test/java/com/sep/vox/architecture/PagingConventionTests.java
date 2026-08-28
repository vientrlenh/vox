package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Giữ MỘT quy ước phân trang duy nhất: mọi thứ đi vào từ ngoài đều 1-based, và adapter là chỗ DUY
 * NHẤT được phép quy đổi sang lối đếm từ 0 của Spring Data.
 *
 * <p>Vì sao phải quét mã nguồn thay vì viết test hành vi: sai lệch ở đây KHÔNG làm hỏng kiểu dữ
 * liệu, không ném lỗi, và không có test nào trong 1891 test còn lại chạm tới. Đợt refactor
 * subscription đã lật 35 trường phân trang trên schema GraphQL từ 0-based sang 1-based nhưng bỏ sót
 * 16 adapter, nên suốt quãng đó schema hứa một đằng còn dữ liệu trả về một nẻo -- client xin trang
 * đầu thì nhận trang thứ hai. Toàn bộ bộ test vẫn xanh trước lẫn sau khi sửa; chỉ có phép quét này
 * mới nhìn thấy.
 *
 * <p>Quét văn bản chứ không quét bytecode (khác {@link LayeredArchitectureTests}): phép trừ 1 bị
 * trình biên dịch nuốt mất, nó không để lại dấu vết nào ở tầng bytecode để ArchUnit soi vào.
 */
class PagingConventionTests {

    private static final Path ADAPTER_DIR =
        Path.of("src/main/java/com/sep/vox/infrastructure/persistence/adapter");

    private static final Path GRAPHQL_DIR = Path.of("src/main/resources/graphql");

    /** Method trả PageResult và nhận vào một tham số trang. */
    private static final Pattern PAGED_METHOD = Pattern.compile(
        "public\\s+PageResult<[^>]+>\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{", Pattern.DOTALL);

    private static final Pattern PAGE_PARAM = Pattern.compile("int\\s+(page|pageNumber)\\b");

    private static final Pattern SUBTRACTS_ONE =
        Pattern.compile("PageRequest\\.of\\(\\s*[\\w.()]+\\s*-\\s*1");

    @Test
    @DisplayName("mọi adapter phân trang đều tự trừ 1 trước khi giao cho Spring Data")
    void adapters_must_convert_one_based_page_to_spring_data() throws IOException {
        var offenders = new ArrayList<String>();
        var scanned = 0;

        try (Stream<Path> files = Files.list(ADAPTER_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var source = Files.readString(file, StandardCharsets.UTF_8);
                var matcher = PAGED_METHOD.matcher(source);
                while (matcher.find()) {
                    if (!PAGE_PARAM.matcher(matcher.group(2)).find()) {
                        continue;
                    }
                    var body = bodyOf(source, matcher.end());
                    if (!body.contains("PageRequest.of(")) {
                        continue;
                    }
                    scanned++;
                    if (!SUBTRACTS_ONE.matcher(body).find()) {
                        offenders.add(file.getFileName() + "." + matcher.group(1));
                    }
                }
            }
        }

        // Chốt chặn cho chính phép kiểm này: regex hỏng thì danh sách rỗng và khẳng định dưới đây
        // đúng một cách vô nghĩa.
        assertThat(scanned)
            .as("số method phân trang quét được -- 0 nghĩa là phép kiểm đang tự lừa mình")
            .isGreaterThan(30);

        assertThat(offenders)
            .as("adapter nhận trang 1-based mà không trừ 1: client xin trang đầu sẽ nhận trang thứ hai")
            .isEmpty();
    }

    @Test
    @DisplayName("mọi tham số page trên schema GraphQL đều mặc định là 1")
    void graphql_page_arguments_must_default_to_one() throws IOException {
        var offenders = new ArrayList<String>();
        var scanned = 0;

        try (Stream<Path> files = Files.list(GRAPHQL_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".graphqls")).toList()) {
                var source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                var field = Pattern.compile("([a-zA-Z_]\\w*)\\s*\\(([^)]*)\\)\\s*:").matcher(source);
                while (field.find()) {
                    var page = Pattern.compile("\\bpage\\s*:\\s*Int!?\\s*(?:=\\s*(\\d+))?")
                        .matcher(field.group(2));
                    if (!page.find()) {
                        continue;
                    }
                    scanned++;
                    // Không mặc định cũng chấp nhận: client buộc phải truyền, không có lối hiểu nhầm.
                    // Chỉ cấm mặc định 0 -- đó mới là thứ tuyên bố sai quy ước.
                    if ("0".equals(page.group(1))) {
                        offenders.add(file.getFileName() + ":" + field.group(1));
                    }
                }
            }
        }

        assertThat(scanned).as("số trường phân trang quét được").isGreaterThan(50);
        assertThat(offenders)
            .as("trường GraphQL còn khai page mặc định 0 trong khi adapter đã trừ 1")
            .isEmpty();
    }

    /** Cắt lấy thân method bắt đầu ngay sau dấu { mở. */
    private static String bodyOf(String source, int openBraceEnd) {
        var depth = 1;
        var i = openBraceEnd;
        while (i < source.length() && depth > 0) {
            var c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        return source.substring(openBraceEnd, i);
    }

    private static String stripComments(String schema) {
        return schema.replaceAll("(?s)\"\"\".*?\"\"\"", "").replaceAll("(?m)#[^\n]*", "");
    }
}
