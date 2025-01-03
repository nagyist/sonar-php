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
package org.sonar.plugins.php.api.tree.declaration;

import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;

/**
 * <a href="http://php.net/manual/en/functions.arguments.php#functions.arguments.type-declaration.types">Not custom types</a><p>
 * Valid built-in types:
 * <ul>
 *   <li>mixed</li>
 *   <li>self</li>
 *   <li>parent</li>
 *   <li>static</li>
 *   <li>array</li>
 *   <li>callable</li>
 *   <li>bool</li>
 *   <li>float</li>
 *   <li>int</li>
 *   <li>string</li>
 *   <li>iterable</li>
 *   <li>object</li>
 * </ul>
 * <pre/>
 */
public interface BuiltInTypeTree extends TypeNameTree {
  SyntaxToken token();
}
