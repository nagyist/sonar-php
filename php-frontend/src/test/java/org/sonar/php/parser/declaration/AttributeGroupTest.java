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
package org.sonar.php.parser.declaration;

import org.junit.jupiter.api.Test;
import org.sonar.php.parser.PHPLexicalGrammar;

import static org.sonar.php.utils.Assertions.assertThat;

class AttributeGroupTest {

  @Test
  void test() {
    assertThat(PHPLexicalGrammar.ATTRIBUTE_GROUP)
      .matches("#[A]")
      .matches("#[A,B]")
      .matches("#[A,B,]")
      .matches("#[A()]")
      .matches("#[A($x)]")
      .matches("#[A(x:$x)]")
      .matches("#[A(x:$x), B()]")
      .notMatches("#[]");
  }

}
