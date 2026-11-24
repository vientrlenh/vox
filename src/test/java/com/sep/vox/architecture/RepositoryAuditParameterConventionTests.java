package com.sep.vox.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A domain repository method does not receive {@code updatedAt}, {@code updatedBy}, {@code modifiedAt} or
 * {@code modifiedBy} from its caller: the adapter stamps the audit columns from the same source as the auditing hook.
 *
 * <p>Parameters named {@code now} that only feed {@code updated_at} are not visible to a name scan; the signature
 * changes of the stamp-removal phase cover them.
 */
class RepositoryAuditParameterConventionTests {

    private static final Path REPOSITORY_DIR = Path.of("src/main/java/com/sep/vox/domain/repository");

    private static final Pattern METHOD_DECLARATION = Pattern.compile("(\\w+)\\s*\\(([^()]*)\\)\\s*;");

    private static final Pattern AUDIT_PARAMETER_NAME =
        Pattern.compile("\\b(?:updatedAt|updatedBy|modifiedAt|modifiedBy)\\b");

    /** Known debt, not a permission: the list may only shrink. Entries are {@code Interface#method}. */
    private static final Set<String> EXEMPTIONS = Set.of();

    @Test
    @DisplayName("repository methods do not take audit stamps from the caller")
    void repository_methods_do_not_take_audit_stamps() throws IOException {
        Set<String> offenders = methodsWithAuditParameter();

        offenders.removeAll(EXEMPTIONS);

        assertThat(offenders)
            .as("remove the updatedAt/updatedBy parameter: the adapter stamps the row itself")
            .isEmpty();
    }

    @Test
    @DisplayName("every exempted repository method still takes an audit stamp")
    void exemptions_still_apply() throws IOException {
        Set<String> stale = new TreeSet<String>(EXEMPTIONS);

        stale.removeAll(methodsWithAuditParameter());

        assertThat(stale)
            .as("exempted methods without an audit parameter: remove them from EXEMPTIONS")
            .isEmpty();
    }

    @Test
    @DisplayName("the rule can fail: a method with an updatedBy parameter is detected")
    void rule_detects_a_violating_snippet() {
        String snippet = "interface SampleRepository { int rename(UUID id, String name, Instant updatedAt); "
            + "int clean(UUID id, String name); }";

        assertThat(methodsWithAuditParameter("SampleRepository", snippet)).containsExactly("SampleRepository#rename");
    }

    private static Set<String> methodsWithAuditParameter() throws IOException {
        Set<String> methods = new TreeSet<String>();
        try (Stream<Path> files = Files.list(REPOSITORY_DIR)) {
            List<Path> sources = files.filter(path -> path.toString().endsWith(".java")).toList();
            assertThat(sources.size())
                .as("number of repository interfaces scanned -- a low number means the check is fooling itself")
                .isGreaterThan(20);
            for (Path file : sources) {
                String interfaceName = file.getFileName().toString().replace(".java", "");
                methods.addAll(methodsWithAuditParameter(interfaceName, Files.readString(file, StandardCharsets.UTF_8)));
            }
        }
        return methods;
    }

    private static Set<String> methodsWithAuditParameter(String interfaceName, String source) {
        String withoutComments = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//[^\n]*", "");
        Set<String> methods = new TreeSet<String>();
        Matcher declaration = METHOD_DECLARATION.matcher(withoutComments);
        while (declaration.find()) {
            if (AUDIT_PARAMETER_NAME.matcher(declaration.group(2)).find()) {
                methods.add(interfaceName + "#" + declaration.group(1));
            }
        }
        return methods;
    }
}
