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
import org.sonar.php.checks.utils.SyntacticEquivalence;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

@Rule(key = SelfAssignmentCheck.KEY)
public class SelfAssignmentCheck extends PHPVisitorCheck {

  public static final String KEY = "S1656";
  private static final String MESSAGE = "Remove or correct this useless self-assignment";

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);

    if (tree.is(Kind.ASSIGNMENT, Kind.ASSIGNMENT_BY_REFERENCE)) {
      check(tree.variable(), tree.value());
    }
  }

  private void check(ExpressionTree lhs, ExpressionTree rhs) {
    if (SyntacticEquivalence.areSyntacticallyEquivalent(lhs, rhs)) {
      context().newIssue(this, lhs, rhs, MESSAGE);
    }
  }
}
