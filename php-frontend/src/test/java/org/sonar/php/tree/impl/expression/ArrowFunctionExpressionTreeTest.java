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

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.jupiter.api.Test;
import org.sonar.php.PHPTreeModelTest;
import org.sonar.php.parser.PHPParserBuilder;
import org.sonar.plugins.php.api.tree.CompilationUnitTree;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.declaration.BuiltInTypeTree;
import org.sonar.plugins.php.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.TypeTree;
import org.sonar.plugins.php.api.tree.expression.ArrayInitializerFunctionTree;
import org.sonar.plugins.php.api.tree.expression.ArrayPairTree;
import org.sonar.plugins.php.api.tree.expression.ArrowFunctionExpressionTree;
import org.sonar.plugins.php.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.php.api.tree.expression.YieldExpressionTree;
import org.sonar.plugins.php.api.tree.statement.ExpressionStatementTree;

import static org.assertj.core.api.Assertions.assertThat;

class ArrowFunctionExpressionTreeTest extends PHPTreeModelTest {

  @Test
  void basic() {
    ArrowFunctionExpressionTree tree = parse("fn () => NULL", Tree.Kind.ARROW_FUNCTION_EXPRESSION);

    assertThat(tree.is(Tree.Kind.ARROW_FUNCTION_EXPRESSION)).isTrue();
    assertThat(tree.staticToken()).isNull();
    assertThat(tree.functionToken().text()).isEqualTo("fn");
    assertThat(tree.referenceToken()).isNull();
    assertThat(tree.parameters().parameters()).isEmpty();
    assertThat(tree.returnTypeClause()).isNull();
    assertThat(tree.doubleArrowToken().text()).isEqualTo("=>");
    assertThat(expressionToString(tree.body())).isEqualTo("NULL");
  }

  @Test
  void withStaticReferenceParameterReturnType() {
    ArrowFunctionExpressionTree tree = parse("static fn&($x): string => $x . '_'", Tree.Kind.ARROW_FUNCTION_EXPRESSION);

    assertThat(tree.is(Tree.Kind.ARROW_FUNCTION_EXPRESSION)).isTrue();
    assertThat(tree.staticToken().text()).isEqualTo("static");
    assertThat(tree.functionToken().text()).isEqualTo("fn");
    assertThat(tree.referenceToken().text()).isEqualTo("&");
    assertThat(tree.parameters().parameters()).hasSize(1);
    assertThat(tree.parameters().parameters().get(0).variableIdentifier().text()).isEqualTo("$x");
    assertThat(((BuiltInTypeTree) ((TypeTree) tree.returnTypeClause().type()).typeName()).token().text()).isEqualTo("string");
    assertThat(tree.doubleArrowToken().text()).isEqualTo("=>");
    assertThat(expressionToString(tree.body())).isEqualTo("$x . '_'");
  }

  @Test
  void arrowFunctionAndYieldPrecedence() {
    ActionParser<Tree> parser = PHPParserBuilder.createParser();
    CompilationUnitTree root = (CompilationUnitTree) parser.parse("<?php\n" +
      "function f() {\n" +
      " yield foo($a) => $a;\n" +
      " yield fn($a)  => $a;\n" +
      "}\n");
    FunctionDeclarationTree function = (FunctionDeclarationTree) root.script().statements().get(0);

    YieldExpressionTree yield0 = (YieldExpressionTree) ((ExpressionStatementTree) function.body().statements().get(0)).expression();
    assertThat(yield0.key().getKind()).isEqualTo(Tree.Kind.FUNCTION_CALL);
    assertThat(yield0.doubleArrowToken()).isNotNull();
    assertThat(yield0.value().getKind()).isEqualTo(Tree.Kind.VARIABLE_IDENTIFIER);

    YieldExpressionTree yield1 = (YieldExpressionTree) ((ExpressionStatementTree) function.body().statements().get(1)).expression();
    assertThat(yield1.key()).isNull();
    assertThat(yield1.doubleArrowToken()).isNull();
    assertThat(yield1.value().getKind()).isEqualTo(Tree.Kind.ARROW_FUNCTION_EXPRESSION);
  }

  @Test
  void arrowFunctionAndArrayIndexValuePrecedence() {
    ActionParser<Tree> parser = PHPParserBuilder.createParser();
    CompilationUnitTree root = (CompilationUnitTree) parser.parse("<?php\n" +
      "$x = array(\n" +
      "    foo($a) => $a,\n" +
      "    fn($a)  => $a\n" +
      ");\n");
    ExpressionStatementTree statement = (ExpressionStatementTree) root.script().statements().get(0);
    ArrayInitializerFunctionTree arrayInitializer = (ArrayInitializerFunctionTree) ((AssignmentExpressionTree) statement.expression()).value();

    ArrayPairTree arrayPair0 = arrayInitializer.arrayPairs().get(0);
    assertThat(arrayPair0.key().getKind()).isEqualTo(Tree.Kind.FUNCTION_CALL);
    assertThat(arrayPair0.doubleArrowToken()).isNotNull();
    assertThat(arrayPair0.value().getKind()).isEqualTo(Tree.Kind.VARIABLE_IDENTIFIER);

    ArrayPairTree arrayPair1 = arrayInitializer.arrayPairs().get(1);
    assertThat(arrayPair1.key()).isNull();
    assertThat(arrayPair1.doubleArrowToken()).isNull();
    assertThat(arrayPair1.value().getKind()).isEqualTo(Tree.Kind.ARROW_FUNCTION_EXPRESSION);
  }

}
