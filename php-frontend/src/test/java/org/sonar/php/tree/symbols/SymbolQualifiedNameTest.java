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
package org.sonar.php.tree.symbols;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolQualifiedNameTest {

  @Test
  void test() {
    SymbolQualifiedName qualifiedName1 = SymbolQualifiedName.create("A", "B", "C");
    SymbolQualifiedName qualifiedName2 = SymbolQualifiedName.create("a", "b", "c");
    assertThat(qualifiedName1).hasToString("a\\b\\c").isEqualTo(qualifiedName2);
  }

}
