/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.php.api.tree.expression;

import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;

/**
 * Common interface to represent all kinds of identifiers.
 * <pre>
 *   {@link Kind#NAME_IDENTIFIER name_identifier}
 *   {@link Kind#VARIABLE_IDENTIFIER $variable_identifier}
 * </pre>
 */
public interface IdentifierTree extends ExpressionTree {

  SyntaxToken token();

  String text();

}
