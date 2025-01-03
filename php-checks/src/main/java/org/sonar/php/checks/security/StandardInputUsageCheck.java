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
package org.sonar.php.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.declaration.NamespaceNameTree;
import org.sonar.plugins.php.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.LiteralTree;
import org.sonar.plugins.php.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

@Rule(key = "S4829")
public class StandardInputUsageCheck extends PHPVisitorCheck {

  private static final String MESSAGE = "Make sure that reading the standard input is safe here.";

  private static final String STDIN = "STDIN";
  private static final String PHP_STDIN = "php://stdin";

  private static final List<String> SAFE_FUNCTIONS = Arrays.asList(
    "fclose",
    "feof",
    "fseek",
    "fstat",
    "ftell",
    "ftruncate",
    "posix_isatty",
    "stream_set_blocking");

  @Override
  public void visitNamespaceName(NamespaceNameTree tree) {
    if (STDIN.equalsIgnoreCase(tree.qualifiedName())) {
      checkUsage(tree);
    }
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (tree.is(Tree.Kind.REGULAR_STRING_LITERAL) && PHP_STDIN.equals(CheckUtils.trimQuotes(tree))) {
      checkUsage(tree);
    }
  }

  private void checkUsage(Tree tree) {
    Tree parent = tree.getParent();
    if (!(parent instanceof BinaryExpressionTree) && !(parent instanceof UnaryExpressionTree) && !isArgumentOfSafeFunctionCall(tree)) {
      context().newIssue(this, tree, MESSAGE);
    }
  }

  private static boolean isArgumentOfSafeFunctionCall(Tree tree) {
    if (!tree.getParent().is(Tree.Kind.CALL_ARGUMENT) || !tree.getParent().getParent().is(Tree.Kind.FUNCTION_CALL)) {
      return false;
    }

    FunctionCallTree functionCall = (FunctionCallTree) tree.getParent().getParent();
    ExpressionTree callee = functionCall.callee();
    if (callee.is(Tree.Kind.NAMESPACE_NAME)) {
      String qualifiedName = ((NamespaceNameTree) callee).qualifiedName();
      return SAFE_FUNCTIONS.stream().anyMatch(qualifiedName::equalsIgnoreCase);
    }

    return false;
  }

}
