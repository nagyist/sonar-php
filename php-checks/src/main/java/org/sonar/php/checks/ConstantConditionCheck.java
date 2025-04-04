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
package org.sonar.php.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ConditionalExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.php.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.php.api.tree.statement.BlockTree;
import org.sonar.plugins.php.api.tree.statement.ElseifClauseTree;
import org.sonar.plugins.php.api.tree.statement.IfStatementTree;
import org.sonar.plugins.php.api.tree.statement.StatementTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

@Rule(key = ConstantConditionCheck.KEY)
public class ConstantConditionCheck extends PHPVisitorCheck {

  public static final String KEY = "S5797";
  private static final String MESSAGE = "Replace this expression; used as a condition it will always be constant.";
  private static final Tree.Kind[] BOOLEAN_CONSTANT_KINDS = {
    Tree.Kind.BOOLEAN_LITERAL,
    Tree.Kind.NUMERIC_LITERAL,
    Tree.Kind.REGULAR_STRING_LITERAL,
    Tree.Kind.NULL_LITERAL,
    Tree.Kind.HEREDOC_LITERAL,
    Tree.Kind.NOWDOC_LITERAL,
    Tree.Kind.MAGIC_CONSTANT,
    Tree.Kind.ARRAY_INITIALIZER_FUNCTION,
    Tree.Kind.ARRAY_INITIALIZER_BRACKET,
    Tree.Kind.NEW_EXPRESSION,
    Tree.Kind.FUNCTION_EXPRESSION,
  };
  private static final Tree.Kind[] LITERAL_KINDS = {
    Tree.Kind.BOOLEAN_LITERAL,
    Tree.Kind.NUMERIC_LITERAL,
    Tree.Kind.REGULAR_STRING_LITERAL,
    Tree.Kind.NULL_LITERAL,
    Tree.Kind.HEREDOC_LITERAL,
    Tree.Kind.NOWDOC_LITERAL,
    Tree.Kind.MAGIC_CONSTANT,
  };
  private static final Tree.Kind[] CONDITIONAL_KINDS = {
    Tree.Kind.CONDITIONAL_AND,
    Tree.Kind.CONDITIONAL_OR,
    Tree.Kind.ALTERNATIVE_CONDITIONAL_AND,
    Tree.Kind.ALTERNATIVE_CONDITIONAL_OR,
    Tree.Kind.ALTERNATIVE_CONDITIONAL_XOR,
  };

  private static boolean isBooleanConstant(ExpressionTree conditionExpression) {
    if (conditionExpression.is(Tree.Kind.PARENTHESISED_EXPRESSION)) {
      ParenthesisedExpressionTree parenthesisedExpression = (ParenthesisedExpressionTree) conditionExpression;
      return isBooleanConstant(parenthesisedExpression.expression());
    }
    if (conditionExpression instanceof BinaryExpressionTree binaryExpression) {
      return isValueConstant(binaryExpression.leftOperand()) && isValueConstant(binaryExpression.rightOperand());
    }
    return conditionExpression.is(BOOLEAN_CONSTANT_KINDS);
  }

  private static boolean isValueConstant(ExpressionTree conditionExpression) {
    if (conditionExpression.is(Tree.Kind.PARENTHESISED_EXPRESSION)) {
      ParenthesisedExpressionTree parenthesisedExpression = (ParenthesisedExpressionTree) conditionExpression;
      return isValueConstant(parenthesisedExpression.expression());
    }
    if (conditionExpression instanceof BinaryExpressionTree binaryExpression) {
      return isValueConstant(binaryExpression.leftOperand()) && isValueConstant(binaryExpression.rightOperand());
    }
    return conditionExpression.is(LITERAL_KINDS);
  }

  private static boolean containsClassOrInterfaceDeclaration(IfStatementTree ifStatement) {
    return ifStatement.statements().stream()
      // Class declaration can't be written outside a block in an if statement
      .filter(s -> s.is(Tree.Kind.BLOCK))
      .map(BlockTree.class::cast)
      .flatMap(block -> block.statements().stream())
      .anyMatch(ConstantConditionCheck::isClassOrInterfaceDeclaration);
  }

  private static boolean isClassOrInterfaceDeclaration(StatementTree statement) {
    return statement.is(Tree.Kind.CLASS_DECLARATION, Tree.Kind.INTERFACE_DECLARATION);
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    if (!containsClassOrInterfaceDeclaration(tree)) {
      ExpressionTree conditionExpression = tree.condition().expression();
      checkBooleanConstant(conditionExpression);
    }
    super.visitIfStatement(tree);
  }

  @Override
  public void visitElseifClause(ElseifClauseTree tree) {
    ExpressionTree conditionExpression = tree.condition().expression();
    checkBooleanConstant(conditionExpression);
    super.visitElseifClause(tree);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    // Avoid redundant issues if the whole expression is constant
    if (tree.is(CONDITIONAL_KINDS) && !isBooleanConstant(tree)) {
      checkBooleanConstant(tree.leftOperand());
      checkBooleanConstant(tree.rightOperand());
    }
    super.visitBinaryExpression(tree);
  }

  @Override
  public void visitPrefixExpression(UnaryExpressionTree tree) {
    if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      checkBooleanConstant(tree.expression());
    }
    super.visitPrefixExpression(tree);
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    ExpressionTree conditionExpression = tree.condition();
    checkBooleanConstant(conditionExpression);
    super.visitConditionalExpression(tree);
  }

  private void checkBooleanConstant(ExpressionTree conditionExpression) {
    if (isBooleanConstant(conditionExpression)) {
      newIssue(conditionExpression, MESSAGE);
    }
  }
}
