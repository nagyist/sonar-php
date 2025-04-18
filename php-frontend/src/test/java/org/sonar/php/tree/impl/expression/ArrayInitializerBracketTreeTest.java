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
package org.sonar.php.tree.impl.expression;

import org.junit.jupiter.api.Test;
import org.sonar.php.PHPTreeModelTest;
import org.sonar.php.parser.PHPLexicalGrammar;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.expression.ArrayInitializerBracketTree;
import org.sonar.plugins.php.api.tree.expression.ArrayPairTree;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayInitializerBracketTreeTest extends PHPTreeModelTest {

  @Test
  void oneElement() {
    ArrayInitializerBracketTree tree = parse("[0]", PHPLexicalGrammar.ARRAY_INITIALIZER);

    assertThat(tree.is(Kind.ARRAY_INITIALIZER_BRACKET)).isTrue();

    assertThat(tree.openBracketToken().text()).isEqualTo("[");
    assertThat(tree.arrayPairs()).hasSize(1);
    assertThat(expressionToString(tree.arrayPairs().get(0).value())).isEqualTo("0");
    assertThat(tree.closeBracketToken().text()).isEqualTo("]");
  }

  @Test
  void multipleElements() {
    ArrayInitializerBracketTree tree = parse("[0, 1, 2]", PHPLexicalGrammar.ARRAY_INITIALIZER);

    assertThat(tree.is(Kind.ARRAY_INITIALIZER_BRACKET)).isTrue();

    assertThat(tree.openBracketToken().text()).isEqualTo("[");

    assertThat(tree.arrayPairs()).hasSize(3);
    assertThat(tree.arrayPairs().getSeparators()).hasSize(2);
    assertThat(expressionToString(tree.arrayPairs().get(0))).isEqualTo("0");

    assertThat(tree.closeBracketToken().text()).isEqualTo("]");
  }

  @Test
  void withTrailingComma() {
    ArrayInitializerBracketTree tree = parse("[0, 1, 2,]", PHPLexicalGrammar.ARRAY_INITIALIZER);

    assertThat(tree.is(Kind.ARRAY_INITIALIZER_BRACKET)).isTrue();

    assertThat(tree.openBracketToken().text()).isEqualTo("[");

    assertThat(tree.arrayPairs()).hasSize(3);
    assertThat(tree.arrayPairs().getSeparators()).hasSize(3);
    assertThat(expressionToString(tree.arrayPairs().get(0))).isEqualTo("0");

    assertThat(tree.closeBracketToken().text()).isEqualTo("]");
  }

  @Test
  void spreadOperator() {
    ArrayInitializerBracketTree tree = parse("[1, 2, ...$arr1, 5]", PHPLexicalGrammar.ARRAY_INITIALIZER);
    assertThat(tree.is(Kind.ARRAY_INITIALIZER_BRACKET)).isTrue();
    assertThat(tree.arrayPairs()).hasSize(4);
    ArrayPairTree spread = tree.arrayPairs().get(2);
    assertThat(spread.ellipsisToken()).isNotNull();
    assertThat(expressionToString(spread.value())).isEqualTo("$arr1");
  }

}
