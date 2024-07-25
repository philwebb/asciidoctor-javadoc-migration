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
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JavadocSite {

	static final Path PATH = Path
		.of("/Users/pwebb/projects/spring-boot/code/3.3.x/spring-boot-project/spring-boot-docs/build/site/api/java");

	static final PathMatcher htmlMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.html");

	private Map<String, List<String>> lookup = new HashMap<>();

	JavadocSite() {
		try {
			Files.find(PATH, Integer.MAX_VALUE, (path, attr) -> htmlMatcher.matches(path)).forEach((page) -> {
				Path relative = PATH.relativize(page);
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
			this.lookup.remove("Environment");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void add(String key, String value) {
		this.lookup.computeIfAbsent(key, (k) -> new ArrayList<>()).add(value);
	}

	public List<String> lookup(String name) {
		return this.lookup.get(name);
	}

}
