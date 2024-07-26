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
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
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

	static final Pattern ANTORA_JAVADOC_PATTERN = Pattern.compile("(url-.+-javadoc):(.*)$");

	static final PathMatcher htmlMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.html");

	private final Map<String, List<String>> lookup = new HashMap<>();

	private final ObjectReader reader;

	JavadocSite() {
		try {
			this.reader = new ObjectMapper().reader();
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
		HttpClient httpClient = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
		List<String> yaml = Files.readAllLines(ANTORA_YAML_PATH);
		for (String line : yaml) {
			Matcher matcher = ANTORA_JAVADOC_PATTERN.matcher(line);
			if (matcher.find()) {
				String name = matcher.group(1).trim();
				String url = expand(matcher.group(2).trim());
				if (name.startsWith("url-spring-boot-")) {
					continue;
				}
				String location = "{" + name + "}/";
				try {
					addUrl(httpClient, url, location);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Unable to add " + url, ex);
				}
			}
		}
		addUrl(httpClient, "https://docs.oracle.com/en/java/javase/17/docs/api/", "{url-javase-javadoc}");
	}

	private void addUrl(HttpClient httpClient, String url, String location) throws Exception {
		String searchUrl = url + "/type-search-index.js";
		TypeSearchElement[] elements = getElements(httpClient, searchUrl);
		for (TypeSearchElement element : elements) {
			String packageName = element.p();
			String className = element.l().replace(".", "$");
			if (packageName != null && !packageName.isEmpty() && className != null && !className.isEmpty()) {
				add(className, location + packageName + "." + className);
				add(packageName + "." + className, location + packageName + "." + className);
				if (className.contains("$")) {
					add(className.replace("$", "."), location + packageName + "." + className);
					add(packageName + "." + className.replace("$", "."), location + packageName + "." + className);
				}
			}
		}

	}

	private TypeSearchElement[] getElements(HttpClient httpClient, String searchUrl)
			throws URISyntaxException, IOException, InterruptedException {
		if (searchUrl.startsWith("https://r2dbc.io/spec")) {
			// No search with r2dbc
			return R2DBC.elements();
		}
		String body = getElementsBody(httpClient, searchUrl).replace("typeSearchIndex = ", "");
		TypeSearchElement[] elements = this.reader.readValue(body, TypeSearchElement[].class);
		return elements;
	}

	private String getElementsBody(HttpClient httpClient, String searchUrl)
			throws URISyntaxException, IOException, InterruptedException {
		if (searchUrl.startsWith("https://javadoc.io/doc/org.flywaydb/flyway-core")) {
			return new String(getClass().getResourceAsStream("/flyway.site").readAllBytes(), StandardCharsets.UTF_8);
		}
		HttpRequest request = HttpRequest.newBuilder(new URI(searchUrl)).build();
		HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IllegalStateException("Bad status " + response.statusCode());
		}
		return response.body();
	}

	private String expand(String url) {
		return url.replace("{version-spring-data-commons}", "current")
			.replace("{version-spring-data-jpa}", "current")
			.replace("{version-spring-data-mongodb}", "current")
			.replace("{version-spring-data-rest}", "current")
			.replace("{version-spring-data-r2dbc}", "current")
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
