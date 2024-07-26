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

import java.util.List;

public class R2DBC {

	private static List<String> classes = List.of("Batch", "Blob", "Clob", "Closeable", "ColumnMetadata", "Connection",
			"ConnectionFactories", "ConnectionFactory", "ConnectionFactoryMetadata", "ConnectionFactoryOptions",
			"ConnectionFactoryOptions.Builder", "ConnectionFactoryProvider", "ConnectionMetadata", "IsolationLevel",
			"Lifecycle", "NoSuchOptionException", "Nullability", "Option", "OutParameterMetadata", "OutParameters",
			"OutParametersMetadata", "Parameter", "Parameter.In", "Parameter.Out", "Parameters",
			"R2dbcBadGrammarException", "R2dbcDataIntegrityViolationException", "R2dbcException",
			"R2dbcNonTransientException", "R2dbcNonTransientResourceException", "R2dbcPermissionDeniedException",
			"R2dbcRollbackException", "R2dbcTimeoutException", "R2dbcTransientException",
			"R2dbcTransientResourceException", "R2dbcType", "Readable", "ReadableMetadata", "Result", "Result.Message",
			"Result.OutSegment", "Result.RowSegment", "Result.Segment", "Result.UpdateCount", "Row", "RowMetadata",
			"Statement", "TransactionDefinition", "Type", "Type.InferredType", "ValidationDepth", "Wrapped");

	static TypeSearchElement[] elements() {
		return classes.stream()
			.map((name) -> new TypeSearchElement("io.r2dbc.spi", name, null, null))
			.toArray(TypeSearchElement[]::new);
	}

}
