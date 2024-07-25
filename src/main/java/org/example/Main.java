package org.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	static final Pattern xrefPattern = Pattern.compile("xref:api:java\\/([^\\.]+)/(.*?)\\.html(#[^\\[]+)?\\[(.*?)\\]");

	static final Pattern classNamePattern = Pattern.compile("`[A-Za-z\\.@][A-Za-z\\.]+`");

	static final PathMatcher adocMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.adoc");

	public static void main(String[] args) throws Exception {
		String path = "/Users/pwebb/projects/spring-boot/code/3.3.x/spring-boot-project/spring-boot-docs/src/docs/antora/modules";
		Files.find(Paths.get(path), Integer.MAX_VALUE, Main::shouldMigrate).forEach(Main::migrate);
	}

	private static boolean shouldMigrate(Path path, BasicFileAttributes attributes) {
		return adocMatcher.matches(path) && !path.getFileName().toString().startsWith("nav-");
	}

	public static void migrate(Path path) {
		try {
			System.out.println("Considering " + path);
			String content = Files.readString(path);
			String replacement = replace(content);
			if (replacement != null) {
				System.out.println(" - writing replacements");
				Files.writeString(path, replacement);
			}
			else {
				System.out.println(" - no replacements");
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String replace(String content) {
		Matcher matcher = xrefPattern.matcher(content);
		StringBuffer result = new StringBuffer();
		AtomicLong endIndex = new AtomicLong(0);
		matcher.results().forEach((matchResult) -> {
			String packageName = matchResult.group(1);
			String className = matchResult.group(2);
			String anchor = matchResult.group(3) == null ? "" : matchResult.group(3);
			String text = matchResult.group(4);
			String xrefPath = packageName.replace("/", ".") + "." + className.replace(".", "$") + anchor;
			String expectedText = "`" + className + "`";
			String expectedAnnotationText = "`@" + className + "`";
			result.append(content.substring(endIndex.intValue(), matchResult.start()));
			result.append("javadoc:");
			result.append(xrefPath);
			result.append("[");
			if (expectedAnnotationText.equals(text)) {
				result.append("format=annotation");
			}
			else if (!expectedText.equals(text) && !className.equals(text)) {
				result.append(text);
			}
			result.append("]");
			endIndex.set(matchResult.end());
		});
		result.append(content.substring(endIndex.intValue(), content.length()));
		return (!result.toString().equals(content)) ? result.toString() : null;
	}

}
