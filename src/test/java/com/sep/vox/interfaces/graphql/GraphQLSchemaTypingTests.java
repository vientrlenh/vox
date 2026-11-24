package com.sep.vox.interfaces.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.graphql.execution.GraphQlSource;

import com.sep.vox.support.IntegrationTest;

/**
 * Keeps the typed scalars and enums from silently regressing to {@code String}.
 *
 * <p>A field, argument or input field whose name says "time" or "status" ({@code *At}, {@code *Date},
 * {@code effectiveFrom}, {@code effectiveTo}, {@code status}) must use {@code DateTime}, {@code Date} or
 * an SDL enum. The only way to keep one as {@code String} is to add it to {@link #ALLOWED_STRING_TYPED_NAMES}
 * together with the reason, so the exception is reviewed in the diff instead of slipping in unnoticed.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
class GraphQLSchemaTypingTests {

    private static final Pattern TEMPORAL_OR_STATUS_NAME =
        Pattern.compile("\\w*At|\\w*Date|effectiveFrom|effectiveTo|status");

    /**
     * {@code Type.field} for fields and input fields, {@code Type.field(argument)} for arguments.
     * <ul>
     *   <li>{@code PracticeSession.status}: the domain model keeps the session status as a free
     *       {@code String} and no Java enum has the same value set.
     * </ul>
     */
    private static final Set<String> ALLOWED_STRING_TYPED_NAMES = Set.of("PracticeSession.status");

    @Autowired
    private GraphQlSource graphQlSource;

    @Test
    void temporal_and_status_names_are_not_string_unless_allow_listed() {
        Set<String> stringTyped = findStringTypedTemporalOrStatusNames(graphQlSource.schema());

        Set<String> unexpected = new TreeSet<String>(stringTyped);
        unexpected.removeAll(ALLOWED_STRING_TYPED_NAMES);
        assertThat(unexpected)
            .describedAs("use DateTime, Date or an SDL enum, or add the name to ALLOWED_STRING_TYPED_NAMES with a reason")
            .isEmpty();
    }

    @Test
    void every_allow_listed_name_is_still_a_string_in_the_schema() {
        Set<String> stringTyped = findStringTypedTemporalOrStatusNames(graphQlSource.schema());

        assertThat(stringTyped)
            .describedAs("remove names that were typed from ALLOWED_STRING_TYPED_NAMES")
            .containsAll(ALLOWED_STRING_TYPED_NAMES);
    }

    @Test
    void the_check_reports_a_mistyped_field_argument_and_input_field() {
        GraphQLSchema schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse("""
            scalar DateTime
            type Query { sessions(startDate: String, endDate: DateTime, from: String): [Session!]! }
            type Session { id: ID! createdAt: String! status: String endedAt: DateTime title: String }
            input SessionInput { effectiveTo: String effectiveFrom: DateTime }
            schema { query: Query }
            """));

        assertThat(findStringTypedTemporalOrStatusNames(schema))
            .containsExactlyInAnyOrder(
                "Query.sessions(startDate)", "Session.createdAt", "Session.status", "SessionInput.effectiveTo");
    }

    private static Set<String> findStringTypedTemporalOrStatusNames(GraphQLSchema schema) {
        Set<String> found = new TreeSet<String>();
        for (GraphQLNamedType type : schema.getAllTypesAsList()) {
            if (type.getName().startsWith("__")) {
                continue;
            }
            if (type instanceof GraphQLFieldsContainer container) {
                container.getFieldDefinitions().forEach(field -> collectOutputField(found, type, field));
            } else if (type instanceof GraphQLInputObjectType inputType) {
                inputType.getFieldDefinitions().forEach(field -> collectInputField(found, inputType, field));
            }
        }
        return found;
    }

    private static void collectOutputField(Set<String> found, GraphQLNamedType owner, GraphQLFieldDefinition field) {
        String qualifiedName = owner.getName() + "." + field.getName();
        collectIfStringTyped(found, qualifiedName, field.getName(), field.getType());
        for (GraphQLArgument argument : field.getArguments()) {
            collectIfStringTyped(found, qualifiedName + "(" + argument.getName() + ")", argument.getName(),
                argument.getType());
        }
    }

    private static void collectInputField(Set<String> found, GraphQLInputObjectType owner,
            GraphQLInputObjectField field) {
        collectIfStringTyped(found, owner.getName() + "." + field.getName(), field.getName(), field.getType());
    }

    private static void collectIfStringTyped(Set<String> found, String qualifiedName, String simpleName,
            GraphQLType type) {
        boolean isStringType = ((GraphQLNamedType) GraphQLTypeUtil.unwrapAll(type)).getName().equals("String");
        if (isStringType && TEMPORAL_OR_STATUS_NAME.matcher(simpleName).matches()) {
            found.add(qualifiedName);
        }
    }
}
