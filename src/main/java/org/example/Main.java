package org.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {

	static final Pattern apiLink = Pattern
		.compile("\\{security-api-url\\}([^\\.]+)/(.*?)\\.html(#[^\\[]+)?\\[(.*?)\\]");

	static int count = 0;

	public static void main(String[] args) throws Exception {
		String path = "/Users/pwebb/projects/spring-boot/code/3.3.x/spring-boot-project/spring-boot-docs/src/docs/antora/modules";
		PathMatcher adocMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.adoc");
		Files.find(Paths.get(path), Integer.MAX_VALUE, (filePath, fileAttr) -> adocMatcher.matches(filePath))
			.forEach(Main::migrateToJavadocInlineMacro);
		System.out.println(count);
	}

	public static void migrateToJavadocInlineMacro(Path path) {
		try {
			String content = Files.readString(path);
			String replacement = replace(content);
			if (replacement != null) {
				Files.writeString(path, replacement);
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String replace(String content) {
		Matcher matcher = apiLink.matcher(content);
		Stream<MatchResult> results = matcher.results();
		if (results.count() == 0) {
			return null;
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
		return result.toString();
	}

}
