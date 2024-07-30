package org.example;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	static final Pattern xrefPattern = Pattern.compile("xref:api:java\\/([^\\.]+)/(.*?)\\.html(#[^\\[]+)?\\[(.*?)\\]");

	static final Pattern classNamePattern = Pattern.compile("([\\s\\n])`([A-Za-z\\.@][A-Za-z\\.]+)`");

	static final PathMatcher adocMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.adoc");

	private static final Map<String, String> COMMON_ANNOTATION_NAMES;
	static {
		Map<String, String> names = new HashMap<>();
		names.put("Controller", "org.springframework.stereotype.Controller");
		names.put("Component", "org.springframework.stereotype.Component");
		names.put("Service", "org.springframework.stereotype.Service");
		names.put("Repository", "org.springframework.stereotype.Repository");
		names.put("RequestMapping", "org.springframework.web.bind.annotation.RequestMapping");
		names.put("Configuration", "org.springframework.context.annotation.Configuration");
		names.put("Value", "org.springframework.beans.factory.annotation.Value");
		names.put("Entity", "jakarta.persistence.Entity");
		names.put("PropertySource", "org.springframework.context.annotation.PropertySource");
		names.put("DurationUnit", "org.springframework.boot.convert.DurationUnit");
		names.put("Endpoint", "org.springframework.boot.actuate.endpoint.annotation.Endpoint");
		names.put("Order", "org.springframework.core.annotation.Order");
		COMMON_ANNOTATION_NAMES = Collections.unmodifiableMap(names);
	}

	private static final Map<String, String> COMMON_CLASS_NAMES;
	static {
		Map<String, String> names = new HashMap<>();
		names.put("Environment", "org.springframework.core.env.Environment");
		names.put("Filter", "jakarta.servlet.Filter");
		names.put("Binder", "org.springframework.boot.context.properties.bind.Binder");
		names.put("ReactorResourceFactory", "org.springframework.http.client.ReactorResourceFactory");
		names.put("PropertySource", "org.springframework.core.env.PropertySource");
		names.put("DataSource", "javax.sql.DataSource");
		names.put("RestClient", "org.springframework.web.client.RestClient");
		COMMON_CLASS_NAMES = Collections.unmodifiableMap(names);
	}

	static JavadocSite javadocSite = new JavadocSite();

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
			if (replacement != null && false) {
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
		String result = content;
		result = replaceXrefs(result);
		result = replaceClassNames(result);
		return (!result.toString().equals(content)) ? result.toString() : null;
	}

	private static String replaceXrefs(String content) {
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
		return result.toString();
	}

	private static String replaceClassNames(String content) {
		Matcher matcher = classNamePattern.matcher(content);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String name = matcher.group(2);
			String replacement = matcher.group();
			if (isLikelyClassName(name)) {
				if (name.endsWith("!!")) {
					return name.substring(0, name.length() - 2);
				}
				boolean annotation = name.startsWith("@");
				if (annotation) {
					name = name.substring(1);
					name = COMMON_ANNOTATION_NAMES.getOrDefault(name, name);
				}
				else {
					name = COMMON_CLASS_NAMES.getOrDefault(name, name);
				}
				List<String> lookup = javadocSite.lookup(name);
				if (lookup != null) {
					if (lookup.size() > 1) {
						throw new RuntimeException("Fix the ambigious " + lookup);
					}
					replacement = matcher.group(1)
							+ "javadoc:%s[%s]".formatted(lookup.get(0), (!annotation) ? "" : "format=annotation");
				}
				else {
					// System.err.println("No idea about " + name);
				}
			}
			matcher.appendReplacement(result, replacement.replace("$", "\\$"));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private static boolean isLikelyClassName(String name) {
		return name.startsWith("org.springframework.boot.") || name.startsWith("@")
				|| Character.isUpperCase(name.charAt(0));
	}

}
