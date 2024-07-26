/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

class JavadocSite {

	static final String DIR = "/Users/pwebb/projects/spring-boot/code/3.3.x/";

	static final Path JAVADOC_SITE_PATH = Path.of(DIR + "spring-boot-project/spring-boot-docs/build/site/api/java");

	static final Path ANTORA_YAML_PATH = Path
		.of(DIR + "spring-boot-project/spring-boot-docs/build/generated/docs/antora-yml/antora.yml");

	static final Pattern ANTORA_JAVADOC_PATTERN = Pattern.compile("(url-[a-z-]+-javadoc):(.*)$");

	static final PathMatcher htmlMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.html");

	private Map<String, List<String>> lookup = new HashMap<>();

	JavadocSite() {
		try {
			addSite();
			addUrls();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void addSite() throws IOException {
		Files.find(JAVADOC_SITE_PATH, Integer.MAX_VALUE, (path, attr) -> htmlMatcher.matches(path)).forEach((page) -> {
			Path relative = JAVADOC_SITE_PATH.relativize(page);
			if (Character.isUpperCase(relative.getFileName().toString().charAt(0))
					&& !relative.toString().contains("-")) {
				String className = relative.getFileName().toString().replace(".html", "").replace('.', '$');
				String packageName = relative.getParent().toString().replace("/", ".");
				add(className, packageName + "." + className);
				add(packageName + "." + className, packageName + "." + className);
				if (className.contains("$")) {
					add(className.replace("$", "."), packageName + "." + className);
					add(packageName + "." + className.replace("$", "."), packageName + "." + className);
				}
			}
		});
	}

	private void addUrls() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader reader = objectMapper.reader();
		List<String> yaml = Files.readAllLines(ANTORA_YAML_PATH);
		for (String line : yaml) {
			Matcher matcher = ANTORA_JAVADOC_PATTERN.matcher(line);
			if (matcher.find()) {
				String name = matcher.group(1).trim();
				String url = expand(matcher.group(2).trim());
				if ("spring-boot".equals(name)) {
					continue;
				}
				String searchUrl = url + "/type-search-index.js";
				HttpClient httpClient = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
				HttpRequest request = HttpRequest.newBuilder(new URI(searchUrl)).build();
				HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
				String body = response.body().replace("typeSearchIndex = ", "");
				TypeSearchElement[] elements = reader.readValue(body, TypeSearchElement[].class);
				for (TypeSearchElement element : elements) {
					String packageName = element.p();
					String className = element.l().replace(".", "$");
					if (packageName != null && !packageName.isEmpty() && className != null && !className.isEmpty()) {
						add(className, packageName + "." + className);
						add(packageName + "." + className, packageName + "." + className);
						if (className.contains("$")) {
							add(className.replace("$", "."), packageName + "." + className);
							add(packageName + "." + className.replace("$", "."), packageName + "." + className);
						}
					}
				}
			}
		}
	}

	private String expand(String url) {
		return url.replace("{version-spring-data-commons}", "current")
			.replace("{version-spring-data-jpa}", "current")
			.replace("{version-spring-data-mongodb}", "current")
			.replace("{version-spring-data-rest}", "current")
			.replace("https://docs.spring.io/spring-hateoas/docs/2.3.1",
					"https://docs.spring.io/spring-hateoas/docs/current");
	}

	private void add(String key, String value) {
		this.lookup.computeIfAbsent(key, (k) -> new ArrayList<>()).add(value);
	}

	public List<String> lookup(String name) {
		return this.lookup.get(name);
	}

	public static void main(String[] args) {
		new JavadocSite();
	}

}
