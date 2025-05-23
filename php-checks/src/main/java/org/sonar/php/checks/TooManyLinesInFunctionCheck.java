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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.php.metrics.LineVisitor;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.declaration.FunctionTree;
import org.sonar.plugins.php.api.visitors.PHPSubscriptionCheck;

@Rule(key = TooManyLinesInFunctionCheck.KEY)
public class TooManyLinesInFunctionCheck extends PHPSubscriptionCheck {

  public static final String KEY = "S138";

  private static final String MESSAGE = "This function %s has %s lines, which is greater than the %s lines authorized. Split it into smaller functions.";

  private static final int DEFAULT = 150;

  @RuleProperty(
    key = "max",
    description = "Maximum authorized lines of code in a function",
    defaultValue = "" + DEFAULT)
  public int max = DEFAULT;

  @Override
  public List<Kind> nodesToVisit() {
    return CheckUtils.getFunctionKinds();
  }

  @Override
  public void visitNode(Tree tree) {
    FunctionTree declaration = (FunctionTree) tree;
    int nbLines = getNumberOfLines(declaration);

    if (nbLines > max) {
      context().newIssue(this, declaration.functionToken(), declaration.parameters(), String.format(MESSAGE, CheckUtils.getFunctionName(declaration), nbLines, max));
    }
  }

  public static int getNumberOfLines(FunctionTree declaration) {
    if (!declaration.body().is(Kind.BLOCK)) {
      return 0;
    }
    return LineVisitor.linesOfCode(declaration);
  }

}
