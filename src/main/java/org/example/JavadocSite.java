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

	static final Pattern ANTORA_VERSION_PATTERN = Pattern.compile("(version-.+):(.*)$");

	static final Pattern ALL_CLASSES_LI_PATTERN = Pattern.compile("<li>(.+?)<\\/li>");

	static final Pattern ALL_CLASSES_A_PATTERN = Pattern.compile("<a href=[\"'](.+?)[\"'].*?>(.*)<\\/a>");

	static final Pattern INNER_TAG_PATTERN = Pattern.compile("<.*?>(.+)<\\/");

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
		Map<String, String> versions = new HashMap<>();
		for (String line : yaml) {
			Matcher versionMatcher = ANTORA_VERSION_PATTERN.matcher(line);
			if (versionMatcher.find()) {
				String name = versionMatcher.group(1).trim();
				String version = versionMatcher.group(2).trim();
				versions.put(name, version);
			}
		}
		for (String line : yaml) {
			Matcher javaDocMatcher = ANTORA_JAVADOC_PATTERN.matcher(line);
			if (javaDocMatcher.find()) {
				String name = javaDocMatcher.group(1).trim();
				String url = expand(javaDocMatcher.group(2).trim(), versions);
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
	}

	private void addUrl(HttpClient httpClient, String url, String location) throws Exception {
		try {
			addUrlViaSearchElements(httpClient, url, location);
		}
		catch (BadStatusCodeException ex) {
			addUrlViaAllClassesFrame(httpClient, url, location);
		}
	}

	private void addUrlViaSearchElements(HttpClient httpClient, String url, String location) throws Exception {
		String searchUrl = url.replace("https://javadoc.io/doc/", "https://javadoc.io/static/")
				+ "/type-search-index.js";
		System.out.println(searchUrl);
		String body = getBody(httpClient, searchUrl).replace("typeSearchIndex = ", "");
		try {
			TypeSearchElement[] elements = this.reader.readValue(body, TypeSearchElement[].class);
			for (TypeSearchElement element : elements) {
				String packageName = element.p();
				String className = element.l().replace(".", "$");
				if (packageName != null && !packageName.isEmpty() && className != null && !className.isEmpty()) {
					add(location, packageName, className);
				}
			}
		}
		catch (Exception ex) {
			System.out.println(body);
			throw ex;
		}
	}

	private void addUrlViaAllClassesFrame(HttpClient httpClient, String url, String location) throws Exception {
		url = url.replace("https://javadoc.io/doc/", "https://javadoc.io/static/");
		String allClassesUrl = url + "/allclasses-frame.html";
		String body = getBody(httpClient, allClassesUrl);
		URI uri = new URI(allClassesUrl);
		String schemeAndHost = uri.getScheme() + "://" + uri.getHost();
		String prefix = url.substring(schemeAndHost.length()) + "/";
		Matcher listItemMatcher = ALL_CLASSES_LI_PATTERN.matcher(body);
		while (listItemMatcher.find()) {
			String listItem = listItemMatcher.group(1);
			Matcher anchorMatcher = ALL_CLASSES_A_PATTERN.matcher(listItem);
			if (anchorMatcher.find()) {
				String href = anchorMatcher.group(1);
				String text = anchorMatcher.group(2);
				Matcher innerTagMatcher = INNER_TAG_PATTERN.matcher(text);
				if (innerTagMatcher.find()) {
					text = innerTagMatcher.group(1);
				}
				if (href.endsWith(".html")) {
					href = href.substring(0, href.length() - 5);
				}
				if (href.startsWith(prefix)) {
					href = href.substring(prefix.length());
				}
				int lastSlash = href.lastIndexOf('/');
				String packageName = href.substring(0, lastSlash).replace('/', '.');
				String className = text.replace(".", "$");
				add(location, packageName, className);
			}
		}
	}

	private void add(String location, String packageName, String className) {
		add(className, location + packageName + "." + className);
		add(packageName + "." + className, location + packageName + "." + className);
		if (className.contains("$")) {
			add(className.replace("$", "."), location + packageName + "." + className);
			add(packageName + "." + className.replace("$", "."), location + packageName + "." + className);
		}
	}

	private String getBody(HttpClient httpClient, String searchUrl)
			throws URISyntaxException, IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(new URI(searchUrl)).build();
		HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new BadStatusCodeException(response);
		}
		return response.body();
	}

	private String expand(String url, Map<String, String> versions) {
		for (Map.Entry<String, String> entry : versions.entrySet()) {
			url = url.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		url = url.replace("https://javadoc.io/doc/io.micrometer/micrometer-core/1.13.7-SNAPSHOT",
				"https://javadoc.io/doc/io.micrometer/micrometer-core/1.13.6");
		url = url.replace("https://javadoc.io/doc/io.micrometer/micrometer-tracing/1.3.6-SNAPSHOT",
				"https://javadoc.io/doc/io.micrometer/micrometer-tracing/1.3.5");
		url = url.replace("https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-annotations/2.17.3",
				"https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-annotations/2.17.2");
		return url;
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
