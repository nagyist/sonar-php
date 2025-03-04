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

import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.plugins.php.api.tree.ScriptTree;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.php.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionExpressionTree;
import org.sonar.plugins.php.api.tree.expression.VariableIdentifierTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

@Rule(key = FunctionDefineOutsideClassCheck.KEY)
public class FunctionDefineOutsideClassCheck extends PHPVisitorCheck {

  public static final String KEY = "S2007";
  private static final String MESSAGE = "Move this %s into a class.";

  private final Set<String> globalVariableNames = new HashSet<>();

  @Override
  public void visitScript(ScriptTree tree) {
    globalVariableNames.clear();
    super.visitScript(tree);
  }

  @Override
  public void visitFunctionExpression(FunctionExpressionTree tree) {
    // don't visit nested nodes
  }

  @Override
  public void visitMethodDeclaration(MethodDeclarationTree tree) {
    // don't visit nested nodes
  }

  @Override
  public void visitFunctionDeclaration(FunctionDeclarationTree tree) {
    context().newIssue(this, tree.name(), String.format(MESSAGE, "function"));
    // don't visit nested nodes
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);

    if (tree.is(Kind.ASSIGNMENT) && tree.variable().is(Kind.VARIABLE_IDENTIFIER)) {
      String varName = ((VariableIdentifierTree) tree.variable()).variableExpression().text();

      if (!isSuperGlobal(varName) && !globalVariableNames.contains(varName)) {
        context().newIssue(this, tree.variable(), String.format(MESSAGE, "variable"));
        globalVariableNames.add(varName);
      }
    }
  }

  private static boolean isSuperGlobal(String varName) {
    return "$GLOBALS".equals(varName) || CheckUtils.getSuperGlobalsByOldName().containsValue(varName);
  }

}
