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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.php.PHPTreeModelTest;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.expression.ArrayAssignmentPatternElementTree;
import org.sonar.plugins.php.api.tree.expression.ListExpressionTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.php.utils.collections.ListUtils.getLast;

class ListExpressionTreeTest extends PHPTreeModelTest {

  @Test
  void empty() {
    ListExpressionTree tree = parse("list ()", Kind.LIST_EXPRESSION);

    assertListExpression(tree, 0, 0);
    assertThat(tree).hasToString("list ()");
  }

  @Test
  void simpleVariable() {
    ListExpressionTree tree = parse("list ($a, $b)", Kind.LIST_EXPRESSION);

    assertListExpression(tree, 2, 1);
    assertFirstElement(tree, Kind.VARIABLE_IDENTIFIER, "$a");
    assertThat(tree).hasToString("list ($a, $b)");
  }

  @Test
  void omittedElement() {
    ListExpressionTree tree = parse("list (, $a, , ,$b)", Kind.LIST_EXPRESSION);

    assertListExpression(tree, 2, 4);
    assertThat(tree).hasToString("list (, $a, , ,$b)");
    assertThat(tree.elements().get(0)).isNotPresent();
    Optional<ArrayAssignmentPatternElementTree> last = getLast(tree.elements());
    assertThat(last).isPresent();
    assertThat(expressionToString(last.get())).isEqualTo("$b");
  }

  @Test
  void nestedListExpression() {
    ListExpressionTree tree = parse("list (list ($a), $b)", Kind.LIST_EXPRESSION);

    assertListExpression(tree, 2, 1);
    assertFirstElement(tree, Kind.LIST_EXPRESSION, "list ($a)");
  }

  private static void assertFirstElement(ListExpressionTree tree, Kind kind, String string) {
    Optional<ArrayAssignmentPatternElementTree> first = tree.elements().get(0);
    assertThat(first).isPresent();
    ArrayAssignmentPatternElementTree element = first.get();
    assertThat(element.variable().is(kind)).isTrue();
    assertThat(expressionToString(element)).isEqualTo(string);
  }

  private static void assertListExpression(ListExpressionTree tree, int nbElement, int nbSeparators) {
    assertThat(tree.is(Kind.LIST_EXPRESSION)).isTrue();

    assertThat(tree.listToken().text()).isEqualTo("list");
    assertThat(tree.openParenthesisToken().text()).isEqualTo("(");
    assertThat(tree.elements().stream().filter(Optional::isPresent)).hasSize(nbElement);
    assertThat(tree.separators()).hasSize(nbSeparators);
    assertThat(tree.closeParenthesisToken().text()).isEqualTo(")");
  }

}
