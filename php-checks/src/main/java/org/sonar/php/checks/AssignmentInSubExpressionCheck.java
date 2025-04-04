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
import org.sonar.php.tree.impl.expression.AssignmentByReferenceTreeImpl;
import org.sonar.php.tree.impl.expression.AssignmentExpressionTreeImpl;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.php.api.tree.statement.ForStatementTree;
import org.sonar.plugins.php.api.tree.statement.WhileStatementTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

@Rule(key = "S1121")
public class AssignmentInSubExpressionCheck extends PHPVisitorCheck {

  private static final Tree.Kind[] ALLOWED_PARENTS = new Tree.Kind[] {
    Tree.Kind.IF_STATEMENT,
    Tree.Kind.ELSEIF_CLAUSE,
    Tree.Kind.CALL_ARGUMENT,
    Tree.Kind.EXPRESSION_STATEMENT,
    Tree.Kind.ASSIGNMENT_BY_REFERENCE,
    Tree.Kind.ASSIGNMENT,
    Tree.Kind.ALTERNATIVE_CONDITIONAL_OR};

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    Tree parent = getParent(tree);
    if (!parent.is(ALLOWED_PARENTS)) {
      SyntaxToken toReport = getToken(tree);
      context().newIssue(this, toReport, String.format("Extract the assignment of \"%s\" from this expression.", tree.variable().toString()));
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    // ignore init and update
    scan(tree.condition());
    scan(tree.statements());
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    // ignore while condition
    scan(tree.statements());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    scan(tree.statement());
    // ignore condition
  }

  private static SyntaxToken getToken(AssignmentExpressionTree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT_BY_REFERENCE)) {
      return ((AssignmentByReferenceTreeImpl) tree).equalToken();
    }
    return ((AssignmentExpressionTreeImpl) tree).equalToken();
  }

  private static Tree getParent(AssignmentExpressionTree tree) {
    Tree result = tree.getParent();
    while (result.is(Tree.Kind.PARENTHESISED_EXPRESSION)) {
      result = result.getParent();
    }
    return result;
  }
}
