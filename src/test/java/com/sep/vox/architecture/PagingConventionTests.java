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
 * Keeps ONE pagination convention: everything that comes in from outside is 1-based, and
 * {@code OneBasedPaging} is the ONLY place allowed to convert to and from Spring Data's 0-based
 * pages. Adapters must go through it and must not subtract or add 1 by hand.
 *
 * <p>Why scan source text instead of writing a behavioural test: a mistake here does NOT break a
 * data type, does not throw, and none of the 1891 other tests touches it. The subscription refactor
 * flipped 35 pagination fields on the GraphQL schema from 0-based to 1-based but missed 16
 * adapters, so for a while the schema promised one thing and the data returned another -- a client
 * asking for the first page received the second. The whole suite was green before and after the
 * fix; only this scan sees it.
 *
 * <p>The scan reads text, not bytecode (unlike {@link LayeredArchitectureTests}): the subtraction
 * of 1 is swallowed by the compiler and leaves no trace in the bytecode for ArchUnit to inspect.
 */
class PagingConventionTests {

    private static final Path ADAPTER_DIR =
        Path.of("src/main/java/com/sep/vox/infrastructure/persistence/adapter");

    private static final Path GRAPHQL_DIR = Path.of("src/main/resources/graphql");

    private static final Path QUERY_DIR =
        Path.of("src/main/java/com/sep/vox/infrastructure/persistence/query");

    /** A method that returns a PageResult and takes a page parameter. */
    private static final Pattern PAGED_METHOD = Pattern.compile(
        "public\\s+PageResult<[^>]+>\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{", Pattern.DOTALL);

    /**
     * Like {@link #PAGED_METHOD} but relaxed for query repositories: there the paging is often done
     * in a shared helper {@code private <T> PageResult<T> paginate(...)}, not always in the public
     * method itself.
     */
    private static final Pattern PAGED_QUERY_METHOD = Pattern.compile(
        "(?:public|private|protected)\\s+(?:static\\s+)?(?:<[^>]+>\\s+)?PageResult<[^>]+>\\s+(\\w+)"
            + "\\s*\\(([^)]*)\\)\\s*\\{",
        Pattern.DOTALL);

    private static final Pattern PAGE_PARAM = Pattern.compile("int\\s+(page|pageNumber)\\b");

    private static final Path INTERFACES_DIR = Path.of("src/main/java/com/sep/vox/interfaces");

    private static final String PAGE_VALIDATOR_FILE_NAME = "PageArguments.java";

    /** A GraphQL {@code @Argument(name = "page")} or a REST request parameter named {@code page}. */
    private static final Pattern PAGE_ARGUMENT = Pattern.compile("name\\s*=\\s*\"page\"");

    /** A page range check written by hand instead of calling {@code PageArguments.validate}. */
    private static final Pattern HAND_WRITTEN_PAGE_CHECK = Pattern.compile("\\bpage\\s*(?:<=|<)\\s*[01]\\b");

    /**
     * Paging arithmetic written by hand in an adapter: a {@code PageRequest.of(...)} whose first
     * argument is not the literal 0 (the literal 0 is a "top N" query, not paging), a read of
     * Spring's 0-based {@code getNumber()}, or a manual {@code page - 1} (also on the
     * {@code normalizedPage} that two query repositories derive from the request page).
     */
    private static final Pattern HAND_WRITTEN_CONVERSION = Pattern.compile(
        "PageRequest\\.of\\(\\s*(?!0\\s*,)|\\.getNumber\\(\\)"
            + "|\\b(?:page|pageNumber|normalizedPage)\\s*-\\s*1\\b");

    @Test
    @DisplayName("every paged adapter method converts pages only through OneBasedPaging")
    void adapters_must_convert_pages_only_through_one_based_paging() throws IOException {
        List<String> offenders = new ArrayList<String>();
        int scanned = 0;

        for (Path file : javaFilesIn(ADAPTER_DIR)) {
            String source = withoutJavaComments(Files.readString(file, StandardCharsets.UTF_8));
            offenders.addAll(handWrittenConversionsIn(file, source));
            for (PagedMethod method : pagedMethodsIn(source, PAGED_METHOD)) {
                scanned++;
                if (!convertsThroughOneBasedPaging(method)) {
                    offenders.add(file.getFileName() + "." + method.name() + " does not call OneBasedPaging");
                }
            }
        }

        assertScannedEnoughAndNoOffenders(scanned, 30, "paged adapter methods", offenders,
            "adapters must convert pages only through OneBasedPaging: a hand-written conversion "
                + "can hand the client the second page when it asked for the first");
    }

    /**
     * The same rule, but for the read side: query repositories do not go through
     * {@code PageRequest}; they compute the offset themselves ({@code setFirstResult} for JPQL, an
     * {@code offset} parameter for native SQL), so the adapter check above does NOT see them. They
     * must take it from {@code OneBasedPaging.toOffset} (or {@code toFirstResult}) and never subtract 1 by hand.
     *
     * <p>That was a real blind spot: this code received a 1-based page from the controller (the
     * {@code PageArguments} guard rejects page <= 0, the schema default is 1) but multiplied
     * {@code page * size} directly, so a client asking for the first page got the second one, and
     * the first page was unreachable. The grading dispatch board, the teacher queue and the appeal
     * list were all affected.
     */
    @Test
    @DisplayName("every paged query repository method computes its offset only through OneBasedPaging")
    void query_repositories_must_compute_offsets_only_through_one_based_paging() throws IOException {
        List<String> offenders = new ArrayList<String>();
        int scanned = 0;

        for (Path file : javaFilesIn(QUERY_DIR)) {
            String source = withoutJavaComments(Files.readString(file, StandardCharsets.UTF_8));
            offenders.addAll(handWrittenConversionsIn(file, source));
            for (PagedMethod method : pagedMethodsIn(source, PAGED_QUERY_METHOD)) {
                if (!computesOffset(method)) {
                    continue;
                }
                scanned++;
                if (!takesOffsetFromOneBasedPaging(method)) {
                    offenders.add(file.getFileName() + "." + method.name()
                        + " computes an offset without OneBasedPaging.toOffset or toFirstResult");
                }
            }
        }

        assertScannedEnoughAndNoOffenders(scanned, 4, "paged query repository methods", offenders,
            "query repositories must compute offsets only through OneBasedPaging: a hand-written "
                + "offset can skip the first page or hand the client the second one");
    }

    /**
     * The request side of the convention: every entry point that takes a {@code page} argument must
     * validate it with {@code PageArguments.validate}, the only place that rejects a page below 1.
     * Without it a 0 reaches the repositories, which trust the boundary and would turn it into a
     * negative offset.
     */
    @Test
    @DisplayName("every controller method with a page argument calls PageArguments.validate")
    void controller_methods_with_a_page_argument_must_validate_it() throws IOException {
        List<String> offenders = new ArrayList<String>();
        int scanned = 0;

        for (Path file : javaFilesUnder(INTERFACES_DIR)) {
            String source = withoutJavaComments(Files.readString(file, StandardCharsets.UTF_8));
            for (PagedMethod method : methodsWithPageArgumentIn(source)) {
                scanned++;
                if (!method.body().contains("PageArguments.validate(")) {
                    offenders.add(file.getFileName() + "." + method.name() + " does not call PageArguments.validate");
                }
            }
        }

        assertScannedEnoughAndNoOffenders(scanned, 80, "page arguments", offenders,
            "every method with a page argument must validate it at the boundary");
    }

    @Test
    @DisplayName("nothing under interfaces/ checks the page range by hand except PageArguments")
    void interfaces_must_not_check_the_page_range_by_hand() throws IOException {
        List<String> offenders = new ArrayList<String>();

        for (Path file : javaFilesUnder(INTERFACES_DIR)) {
            if (file.getFileName().toString().equals(PAGE_VALIDATOR_FILE_NAME)) {
                continue;
            }
            String source = withoutJavaComments(Files.readString(file, StandardCharsets.UTF_8));
            Matcher matcher = HAND_WRITTEN_PAGE_CHECK.matcher(source);
            while (matcher.find()) {
                offenders.add(file.getFileName() + ": hand-written page check `" + matcher.group() + "`");
            }
        }

        assertThat(offenders)
            .as("page ranges are checked only by PageArguments, so that every entry point rejects the same input")
            .isEmpty();
    }

    @Test
    @DisplayName("every page argument in the GraphQL schema defaults to 1")
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

        assertThat(scanned).as("number of paged fields scanned").isGreaterThan(50);
        assertThat(offenders)
            .as("GraphQL field still declares a page default of 0 although the adapter already subtracts 1")
            .isEmpty();
    }

    /**
     * Guard for the scanning checks themselves: if a regex is broken, nothing is scanned and the
     * offender list is empty, so the assertion on it alone would be true in a meaningless way.
     */
    private static void assertScannedEnoughAndNoOffenders(
        int scanned, int minimumScanned, String scannedDescription, List<String> offenders, String rule) {
        assertThat(scanned)
            .as("number of " + scannedDescription + " scanned -- a low number means the check is fooling itself")
            .isGreaterThan(minimumScanned);
        assertThat(offenders).as(rule).isEmpty();
    }

    private record PagedMethod(String name, String body) {
    }

    private static List<Path> javaFilesIn(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static List<Path> javaFilesUnder(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    /** One entry per {@code page} argument: the method that declares it, with the body of that method. */
    private static List<PagedMethod> methodsWithPageArgumentIn(String source) {
        List<PagedMethod> methods = new ArrayList<PagedMethod>();
        Matcher matcher = PAGE_ARGUMENT.matcher(source);
        while (matcher.find()) {
            int methodOpenParen = enclosingOpenParen(source, enclosingOpenParen(source, matcher.start()));
            int methodCloseParen = matchingCloseParen(source, methodOpenParen);
            int bodyStart = source.indexOf('{', methodCloseParen) + 1;
            methods.add(new PagedMethod(identifierBefore(source, methodOpenParen), bodyOf(source, bodyStart)));
        }
        return methods;
    }

    /** The index of the nearest unmatched {@code (} before {@code position}. */
    private static int enclosingOpenParen(String source, int position) {
        int depth = 0;
        for (int i = position - 1; i >= 0; i--) {
            char c = source.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(' && depth-- == 0) {
                return i;
            }
        }
        throw new IllegalStateException("no enclosing parenthesis before index " + position);
    }

    private static int matchingCloseParen(String source, int openParen) {
        int depth = 0;
        for (int i = openParen; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        throw new IllegalStateException("unclosed parenthesis at index " + openParen);
    }

    private static String identifierBefore(String source, int position) {
        int end = position;
        while (Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (Character.isJavaIdentifierPart(source.charAt(start - 1))) {
            start--;
        }
        return source.substring(start, end);
    }

    private static List<PagedMethod> pagedMethodsIn(String source, Pattern methodPattern) {
        List<PagedMethod> methods = new ArrayList<PagedMethod>();
        Matcher matcher = methodPattern.matcher(source);
        while (matcher.find()) {
            if (PAGE_PARAM.matcher(matcher.group(2)).find()) {
                methods.add(new PagedMethod(matcher.group(1), bodyOf(source, matcher.end())));
            }
        }
        return methods;
    }

    /**
     * A paged method either calls {@code OneBasedPaging} itself or only delegates to an overload with
     * the same name, which is scanned on its own.
     */
    private static boolean convertsThroughOneBasedPaging(PagedMethod method) {
        return method.body().contains("OneBasedPaging.")
            || method.body().strip().startsWith("return " + method.name() + "(");
    }

    private static boolean takesOffsetFromOneBasedPaging(PagedMethod method) {
        return method.body().contains("OneBasedPaging.toOffset(")
            || method.body().contains("OneBasedPaging.toFirstResult(");
    }

    private static boolean computesOffset(PagedMethod method) {
        return method.body().contains("setFirstResult(") || method.body().contains("\"offset\"");
    }

    private static List<String> handWrittenConversionsIn(Path file, String source) {
        List<String> found = new ArrayList<String>();
        Matcher matcher = HAND_WRITTEN_CONVERSION.matcher(source);
        while (matcher.find()) {
            found.add(file.getFileName() + ": hand-written conversion `" + matcher.group().strip() + "`");
        }
        return found;
    }

    private static String withoutJavaComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//[^\n]*", "");
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
