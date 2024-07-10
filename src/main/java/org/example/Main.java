package org.example;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    static final Pattern apiLink = Pattern.compile("\\{security-api-url\\}([^\\.]+)/(.*?)\\.html(#[^\\[]+)?\\[(.*?)\\]");
    public static void main(String[] args) throws Exception {
        String path = "/Users/rwinch/code/spring-projects/spring-security/main/docs/modules";
        PathMatcher adocMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.adoc");


        Files.find(Paths.get(path),
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> adocMatcher.matches(filePath))
                .forEach(Main::migrateToJavadocInlineMacro);
        System.out.println(count);
    }

    public static void migrateToJavadocInlineMacro(Path path) {
        try {
            String content = Files.readString(path);
            ReplacementResult replace = replace(content);
            if (replace.matched) {
//                count++;
                System.out.println(path);
                Files.writeString(path, replace.replacement);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static int count = 0;

    public static ReplacementResult replace(String content) {
        Matcher matcher = apiLink.matcher(content);
        Stream<MatchResult> results = matcher.results();

        if (results.count() == 0) {
            return new ReplacementResult(false, content, null);
        }
        matcher = matcher.reset();

        StringBuffer result = new StringBuffer();
        AtomicLong endIndex = new AtomicLong(0);
        matcher.results().forEach((mr) -> {
            count++;
            String packageName = mr.group(1);
            String className = mr.group(2);
            String anchor = mr.group(3) == null ? "" : mr.group(3);
            String xrefPath = packageName.replaceAll("/", ".") + "." + className + anchor;
            result.append(content.substring(endIndex.intValue(), mr.start()));
            result.append("javadoc:");
            result.append(xrefPath);
            result.append("[");
            String expectedText = "`" + className + "`";
            String expectedAnnotationText = "`@" + className + "`";
            String text = mr.group(4);
            if (expectedAnnotationText.equals(text)) {
                result.append("format=annotation");
            }
            else if (!expectedText.equals(text) && !className.equals(text)) {
                result.append(text);
            }

            result.append("]");
            endIndex.set(mr.end());
        });
        result.append(content.substring(endIndex.intValue(), content.length()));
        return new ReplacementResult(true, content, result.toString());
    }

    record ReplacementResult(boolean matched, String original, String replacement) {}
}
