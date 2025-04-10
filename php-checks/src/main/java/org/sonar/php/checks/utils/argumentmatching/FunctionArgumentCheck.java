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
package org.sonar.php.checks.utils.argumentmatching;

import java.util.Optional;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.plugins.php.api.tree.declaration.CallArgumentTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

/**
 * This abstract class simplifies the checking of function calls for the specific arguments.
 * {@link ArgumentVerifierValueContainment} can be used to check whether an argument corresponds to a certain value or a set of values.
 * Based on a flag of the verifier an issue can be created on match or non-match.
 * If arguments should only be checked when other arguments must match certain values,
 * {@link ArgumentMatcherValueContainment} can be used. They are used as a condition for the verification.
 */
public abstract class FunctionArgumentCheck extends PHPVisitorCheck {

  /**
   * Implement this method to create an issue with specific message for the certain check.
   * As expression tree the verified argument is given.
   */
  protected abstract void createIssue(ExpressionTree argument);

  /**
   * Several {@link ArgumentVerifierValueContainment} and {@link ArgumentMatcherValueContainment} can be included in the check.
   * This means a single function can be checked for multiple arguments.
   * <p>
   * The order of indicators and verifiers must be observed.
   * On the one hand, an indicator can lead to the termination of the check of a certain function.
   * On the other hand, issues can be created by verifier before an indicator is triggered.
   */
  public void checkArgument(FunctionCallTree tree, String expectedFunctionName, ArgumentMatcher... expectedArgument) {
    String functionName = CheckUtils.getLowerCaseFunctionName(tree);
    if (expectedFunctionName.equals(functionName)) {
      for (ArgumentMatcher argumentMatcher : expectedArgument) {
        if ((argumentMatcher.getName() == null && argumentMatcher.getPosition() >= tree.callArguments().size()) || !verifyArgument(tree,
          argumentMatcher)) {
          return;
        }
      }
    }
  }

  public boolean checkArgumentAbsence(FunctionCallTree tree, String expectedFunctionName, int position) {
    String functionName = CheckUtils.getLowerCaseFunctionName(tree);
    if (expectedFunctionName.equals(functionName) && position >= tree.callArguments().size()) {
      createIssue(tree);
      return true;
    }
    return false;
  }

  private boolean verifyArgument(FunctionCallTree tree, ArgumentMatcher argumentMatcher) {
    Optional<CallArgumentTree> optionalArgument = CheckUtils.argument(tree, argumentMatcher.getName(), argumentMatcher.getPosition());

    if (optionalArgument.isPresent()) {

      ExpressionTree argument = optionalArgument.get().value();
      ExpressionTree argumentValue = CheckUtils.assignedValue(argument);

      boolean matchesValues = argumentMatcher.matches(argumentValue);
      if (argumentMatcher instanceof IssueRaiser issueRaiser && issueRaiser.shouldRaiseIssue(matchesValues, argumentValue)) {
        createIssue(argument);
      }
      return matchesValues;
    }
    return false;
  }

  public interface IssueRaiser {
    boolean shouldRaiseIssue(boolean matchingSuccessful, ExpressionTree argumentValue);
  }

}
