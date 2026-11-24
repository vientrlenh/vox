package com.sep.vox.architecture;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

/**
 * Measures methods and constructors with the JDK compiler tree API (no dependency). A method is counted from its return
 * type (or constructor name) to its closing brace, so annotations and Javadoc are not counted.
 */
final class MethodLengthScanner {

    /** One method or constructor: {@code key} is {@code relative/path.java#name(ParamTypes)}. */
    record MeasuredMethod(String key, int lines) {
    }

    private MethodLengthScanner() {
    }

    static List<MeasuredMethod> measureDirectory(Path sourceRoot) throws IOException {
        List<InMemorySource> sources = new ArrayList<InMemorySource>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                String relativePath = sourceRoot.relativize(file).toString().replace(File.separatorChar, '/');
                sources.add(new InMemorySource(relativePath, Files.readString(file, StandardCharsets.UTF_8)));
            }
        }
        return measure(sources);
    }

    static List<MeasuredMethod> measureSource(String relativePath, String source) throws IOException {
        return measure(List.of(new InMemorySource(relativePath, source)));
    }

    private static List<MeasuredMethod> measure(List<InMemorySource> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavacTask task = (JavacTask) compiler.getTask(new StringWriter(), null, null, List.of("-proc:none"), null,
            sources);
        Map<String, InMemorySource> sourceByUri = new HashMap<String, InMemorySource>();
        sources.forEach(source -> sourceByUri.put(source.toUri().toString(), source));
        List<MeasuredMethod> measured = new ArrayList<MeasuredMethod>();
        SourcePositions positions = Trees.instance(task).getSourcePositions();
        for (CompilationUnitTree unit : task.parse()) {
            InMemorySource source = sourceByUri.get(unit.getSourceFile().toUri().toString());
            new MethodCollector(source.relativePath, source.content, unit, positions, measured).scan(unit, null);
        }
        return measured;
    }

    private static final class MethodCollector extends TreeScanner<Void, Void> {

        private final String relativePath;
        private final String source;
        private final CompilationUnitTree unit;
        private final SourcePositions positions;
        private final List<MeasuredMethod> measured;
        private final Deque<String> enclosingClassNames = new ArrayDeque<String>();

        MethodCollector(String relativePath, String source, CompilationUnitTree unit, SourcePositions positions,
            List<MeasuredMethod> measured) {
            this.relativePath = relativePath;
            this.source = source;
            this.unit = unit;
            this.positions = positions;
            this.measured = measured;
        }

        @Override
        public Void visitClass(ClassTree classTree, Void unused) {
            enclosingClassNames.push(classTree.getSimpleName().toString());
            super.visitClass(classTree, unused);
            enclosingClassNames.pop();
            return null;
        }

        @Override
        public Void visitMethod(MethodTree method, Void unused) {
            if (method.getBody() != null) {
                measured.add(new MeasuredMethod(keyOf(method), linesOf(method)));
            }
            return super.visitMethod(method, unused);
        }

        private int linesOf(MethodTree method) {
            LineMap lineMap = unit.getLineMap();
            long startLine = lineMap.getLineNumber(startOf(method));
            long endLine = lineMap.getLineNumber(positions.getEndPosition(unit, method));
            return (int) (endLine - startLine + 1);
        }

        private long startOf(MethodTree method) {
            if (method.getReturnType() != null) {
                return positions.getStartPosition(unit, method.getReturnType());
            }
            long afterModifiers = positions.getEndPosition(unit, method.getModifiers());
            long searchFrom = afterModifiers >= 0 ? afterModifiers : positions.getStartPosition(unit, method);
            int name = source.indexOf(enclosingClassNames.peek(), (int) searchFrom);
            return name >= 0 ? name : positions.getStartPosition(unit, method);
        }

        private String keyOf(MethodTree method) {
            String name = method.getReturnType() == null ? enclosingClassNames.peek() : method.getName().toString();
            String parameterTypes = method.getParameters().stream()
                .map(MethodCollector::parameterType)
                .collect(Collectors.joining(","));
            return relativePath + "#" + name + "(" + parameterTypes + ")";
        }

        private static String parameterType(VariableTree parameter) {
            return parameter.getType().toString().replaceAll("\\s+", "");
        }
    }

    private static final class InMemorySource extends SimpleJavaFileObject {

        private final String relativePath;
        private final String content;

        InMemorySource(String relativePath, String content) {
            super(URI.create("string:///" + relativePath), Kind.SOURCE);
            this.relativePath = relativePath;
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
