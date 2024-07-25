package org.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
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
		while (matcher.find()) {
			String packageName = matcher.group(1);
			String className = matcher.group(2);
			String anchor = matcher.group(3);
			String text = matcher.group(4);
			String path = packageName.replace("/", ".") + "." + className.replace(".", "$")
					+ ((anchor != null) ? anchor : "");
			if (("`@" + className + "`").equals(text)) {
				text = "format=annotation";
			}
			else if (("`" + className + "`").equals(text) || className.equals(text)) {
				text = "";
			}
			String replacement = "javadoc:%s[%s]".formatted(path, text);
			matcher.appendReplacement(result, replacement.replace("$", "\\$"));
		}
		matcher.appendTail(result);
		return (!result.toString().equals(content)) ? result.toString() : null;
	}

}
