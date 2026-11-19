package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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

    private static final Path QUERY_DIR =
        Path.of("src/main/java/com/sep/vox/infrastructure/persistence/query");

    /** Method trả PageResult và nhận vào một tham số trang. */
    private static final Pattern PAGED_METHOD = Pattern.compile(
        "public\\s+PageResult<[^>]+>\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{", Pattern.DOTALL);

    /**
     * Như {@link #PAGED_METHOD} nhưng nới cho query repository: ở đó phép phân trang hay nằm trong
     * một helper {@code private <T> PageResult<T> paginate(...)} dùng chung, chứ không phải lúc nào
     * cũng ở ngay method public.
     */
    private static final Pattern PAGED_QUERY_METHOD = Pattern.compile(
        "(?:public|private|protected)\\s+(?:static\\s+)?(?:<[^>]+>\\s+)?PageResult<[^>]+>\\s+(\\w+)"
            + "\\s*\\(([^)]*)\\)\\s*\\{",
        Pattern.DOTALL);

    private static final Pattern PAGE_PARAM = Pattern.compile("int\\s+(page|pageNumber)\\b");

    private static final Pattern SUBTRACTS_ONE =
        Pattern.compile("PageRequest\\.of\\(\\s*[\\w.()]+\\s*-\\s*1");

    /** Lối phân trang thủ công của query repository: {@code setFirstResult((page - 1) * size)}. */
    private static final Pattern OFFSET_SUBTRACTS_ONE =
        Pattern.compile("setFirstResult\\(\\s*\\(?\\s*[\\w.()]+\\s*-\\s*1");

    @Test
    @DisplayName("mọi adapter phân trang đều tự trừ 1 trước khi giao cho Spring Data")
    void adapters_must_convert_one_based_page_to_spring_data() throws IOException {
        List<String> offenders = new ArrayList<String>();
        int scanned = 0;

        try (Stream<Path> files = Files.list(ADAPTER_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = PAGED_METHOD.matcher(source);
                while (matcher.find()) {
                    if (!PAGE_PARAM.matcher(matcher.group(2)).find()) {
                        continue;
                    }
                    String body = bodyOf(source, matcher.end());
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

        // Guard for this check itself: if the regex is broken the list is empty, and the assertion
        // below would be true in a meaningless way.
        assertThat(scanned)
            .as("số method phân trang quét được -- 0 nghĩa là phép kiểm đang tự lừa mình")
            .isGreaterThan(30);

        assertThat(offenders)
            .as("adapter nhận trang 1-based mà không trừ 1: client xin trang đầu sẽ nhận trang thứ hai")
            .isEmpty();
    }

    /**
     * The same rule, but for the read side: query repositories do not go through
     * {@code PageRequest}; they compute the offset themselves with {@code setFirstResult}, so the
     * adapter check above does NOT see them.
     *
     * <p>That was a real blind spot: this code received a 1-based page from the controller (the
     * {@code validatePageSize} guard rejects page <= 0, the schema default is 1) but multiplied
     * {@code page * size} directly, so a client asking for the first page got the second one, and
     * the first page was unreachable. The grading dispatch board, the teacher queue and the appeal
     * list were all affected.
     */
    @Test
    @DisplayName("mọi query repository phân trang đều tự trừ 1 trước khi tính offset")
    void query_repositories_must_convert_one_based_page_to_offset() throws IOException {
        List<String> offenders = new ArrayList<String>();
        int scanned = 0;

        try (Stream<Path> files = Files.list(QUERY_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = PAGED_QUERY_METHOD.matcher(source);
                while (matcher.find()) {
                    if (!PAGE_PARAM.matcher(matcher.group(2)).find()) {
                        continue;
                    }
                    String body = bodyOf(source, matcher.end());
                    if (!body.contains("setFirstResult(")) {
                        continue;
                    }
                    scanned++;
                    if (!OFFSET_SUBTRACTS_ONE.matcher(body).find()) {
                        offenders.add(file.getFileName() + "." + matcher.group(1));
                    }
                }
            }
        }

        // Guard for this check itself, as above.
        assertThat(scanned)
            .as("số method phân trang quét được trong query repository")
            .isGreaterThan(4);

        assertThat(offenders)
            .as("query repository nhận trang 1-based mà nhân thẳng page * size: mất hẳn trang đầu")
            .isEmpty();
    }

    @Test
    @DisplayName("mọi tham số page trên schema GraphQL đều mặc định là 1")
    void graphql_page_arguments_must_default_to_one() throws IOException {
        List<String> offenders = new ArrayList<String>();
        int scanned = 0;

        try (Stream<Path> files = Files.list(GRAPHQL_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".graphqls")).toList()) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                Matcher field = Pattern.compile("([a-zA-Z_]\\w*)\\s*\\(([^)]*)\\)\\s*:").matcher(source);
                while (field.find()) {
                    Matcher page = Pattern.compile("\\bpage\\s*:\\s*Int!?\\s*(?:=\\s*(\\d+))?")
                        .matcher(field.group(2));
                    if (!page.find()) {
                        continue;
                    }
                    scanned++;
                    // Having no default is also accepted: the client is forced to pass the value,
                    // so there is no room for a misunderstanding.
                    // Only a default of 0 is forbidden -- that is what declares the wrong
                    // convention.
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

    /** Cuts out the method body that starts right after the opening brace. */
    private static String bodyOf(String source, int openBraceEnd) {
        int depth = 1;
        int i = openBraceEnd;
        while (i < source.length() && depth > 0) {
            char c = source.charAt(i);
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
