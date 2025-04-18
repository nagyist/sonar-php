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
package org.sonar.php.checks.formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.php.api.PHPPunctuator;
import org.sonar.php.checks.FormattingStandardCheck;
import org.sonar.php.tree.impl.PHPTree;
import org.sonar.plugins.php.api.tree.ScriptTree;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.declaration.CallArgumentTree;
import org.sonar.plugins.php.api.tree.declaration.ClassDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.FunctionExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

import static org.sonar.php.utils.collections.ListUtils.getLast;

public class IndentationCheck extends PHPVisitorCheck implements FormattingCheck {

  private static final String ARGUMENT_LINE_SPLIT_MESSAGE = "Either split this list into multiple lines, aligned at column \"%s\" or put all arguments on line \"%s\".";
  private static final String ARGUMENT_INDENTATION_MESSAGE = "Align all arguments in this list at column \"%s\".";
  private static final int PSR2_INDENTATION = 4;
  private static final String FUNCTION_CALL_PARENTHESIS_MESSAGE = "Move the closing parenthesis on the next line.";
  private static final String FUNCTION_DEC_PARENTHESIS_MESSAGE = "Move the closing parenthesis with the opening brace on the next line.";
  private static final String INTERFACE_SPLIT_MESSAGE = "Either split this list into multiple lines or move it on the same line \"%s\".";
  private static final String INTERFACE_INDENTATION = "Align all interfaces in this list at column \"%s\".";

  private FormattingStandardCheck check;
  private Map<Integer, Integer> startColumnByLine = new HashMap<>();

  @Override
  public void checkFormat(FormattingStandardCheck formattingCheck, ScriptTree scriptTree) {
    this.check = formattingCheck;
    this.startColumnByLine.clear();

    super.visitScript(scriptTree);
  }

  @Override
  public void visitToken(SyntaxToken token) {
    if (startColumnByLine.get(token.line()) == null) {
      startColumnByLine.put(token.line(), token.column());
    }

    super.visitToken(token);
  }

  @Override
  public void visitFunctionCall(FunctionCallTree tree) {
    super.visitFunctionCall(tree);

    if (check.isFunctionCallsArgumentsIndentation && !check.isInternalFunction(tree.callee())) {
      SyntaxToken calleeLastToken = ((PHPTree) tree.callee()).getLastToken();
      checkArgumentsIndentation(
        tree.callArguments().stream().map(CallArgumentTree::value).toList(),
        calleeLastToken,
        startColumnForLine(calleeLastToken.line()),
        tree.closeParenthesisToken(), true);
    }
  }

  @Override
  public void visitFunctionDeclaration(FunctionDeclarationTree tree) {
    super.visitFunctionDeclaration(tree);

    if (check.isMethodArgumentsIndentation) {
      checkArgumentsIndentation(
        tree.parameters().parameters(),
        tree.name().token(),
        startColumnForLine(tree.functionToken().line()),
        tree.parameters().closeParenthesisToken(), false);
    }
  }

  @Override
  public void visitMethodDeclaration(MethodDeclarationTree tree) {
    super.visitMethodDeclaration(tree);

    if (check.isMethodArgumentsIndentation) {
      checkArgumentsIndentation(
        tree.parameters().parameters(),
        tree.name().token(),
        startColumnForLine(tree.functionToken().line()),
        tree.parameters().closeParenthesisToken(),
        false);
    }
  }

  @Override
  public void visitFunctionExpression(FunctionExpressionTree tree) {
    super.visitFunctionExpression(tree);

    if (check.isMethodArgumentsIndentation) {
      checkArgumentsIndentation(
        tree.parameters().parameters(),
        tree.functionToken(),
        startColumnForLine(tree.functionToken().line()),
        tree.parameters().closeParenthesisToken(),
        false);
    }
  }

  @Override
  public void visitClassDeclaration(ClassDeclarationTree tree) {
    super.visitClassDeclaration(tree);
    checkImplementListIndentation(tree);
  }

  private void checkImplementListIndentation(ClassDeclarationTree classTree) {
    if (check.isInterfacesIndentation && classTree.is(Tree.Kind.CLASS_DECLARATION) && !classTree.superInterfaces().isEmpty()) {

      List<Tree> interfaceList = new ArrayList<>(classTree.superInterfaces());
      SyntaxToken classToken = classTree.classToken();
      SyntaxToken lastInterfaceToken = ((PHPTree) getLast(classTree.superInterfaces())).getFirstToken();
      int expectedColumn = classToken.column() + PSR2_INDENTATION;

      if (!TokenUtils.isOnSameLine(classToken, lastInterfaceToken)) {

        if (areIncorrectlySplitOnLines(classToken.line(), interfaceList)) {
          check.reportIssue(String.format(INTERFACE_SPLIT_MESSAGE, classToken.line()), classTree.superInterfaces().get(0));
        } else if (areIncorrectlyIndented(expectedColumn, interfaceList)) {
          check.reportIssue(String.format(INTERFACE_INDENTATION, expectedColumn), classTree.superInterfaces().get(0));
        }
      }
    }
  }

  private void checkArgumentsIndentation(
    List<? extends Tree> arguments,
    SyntaxToken functionName, int baseColumn,
    @Nullable SyntaxToken closeParenthesis,
    boolean isFunctionCall) {
    if (arguments.size() > 1 && !allArgumentsOnSameLine(functionName, arguments)) {
      int expectedIndentationColumn = baseColumn + PSR2_INDENTATION;
      int callingLine = functionName.line();
      Tree firstArg = arguments.get(0);
      if (areIncorrectlySplitOnLines(callingLine, arguments)) {
        check.reportIssue(String.format(ARGUMENT_LINE_SPLIT_MESSAGE, expectedIndentationColumn, callingLine), firstArg);
      } else if (areIncorrectlyIndented(expectedIndentationColumn, arguments)) {
        check.reportIssue(String.format(ARGUMENT_INDENTATION_MESSAGE, expectedIndentationColumn), firstArg);
      }

      // Checking parenthesis
      if (closeParenthesis != null) {
        checkClosingParenthesisLocation(getLast(arguments), closeParenthesis, isFunctionCall);
      }
    }
  }

  // http://www.php-fig.org/psr/psr-2/meta/#3-1-multi-line-arguments-09-08-2013
  private static boolean allArgumentsOnSameLine(SyntaxToken functionName, List<? extends Tree> arguments) {
    int expectedLine = getLastLine(functionName);
    for (Tree argument : arguments) {
      if (getStartLine(argument) != expectedLine) {
        return false;
      }

      if (argument.is(Kind.ARRAY_INITIALIZER_BRACKET, Kind.ARRAY_INITIALIZER_FUNCTION, Kind.FUNCTION_EXPRESSION)) {
        expectedLine = getLastLine(argument);
      }
    }

    return getLastLine(arguments.get(arguments.size() - 1)) == expectedLine;
  }

  private void checkClosingParenthesisLocation(Tree lastArgument, SyntaxToken closeParenthesis, boolean isFunctionCall) {
    if (!((PHPTree) lastArgument).getLastToken().text().equals(PHPPunctuator.RPARENTHESIS.getValue())
      && TokenUtils.isOnSameLine(((PHPTree) lastArgument).getLastToken(), closeParenthesis)) {
      check.reportIssue(
        isFunctionCall ? FUNCTION_CALL_PARENTHESIS_MESSAGE : FUNCTION_DEC_PARENTHESIS_MESSAGE,
        closeParenthesis);
    }
  }

  private static boolean areIncorrectlyIndented(int expectedColumn, List<? extends Tree> items) {
    for (Tree item : items) {
      if (isIncorrectlyIndented(expectedColumn, (PHPTree) item)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isIncorrectlyIndented(int expectedColumn, PHPTree item) {
    PHPTree parent = (PHPTree) item.getParent();
    if (isNamedArgument(parent)) {
      SyntaxToken firstToken = parent.getFirstToken();
      return firstToken.column() != expectedColumn;
    } else {
      return item.getFirstToken().column() != expectedColumn;
    }
  }

  private static boolean isNamedArgument(PHPTree tree) {
    return tree.is(Kind.CALL_ARGUMENT) && ((CallArgumentTree) tree).name() != null;
  }

  private static boolean areIncorrectlySplitOnLines(int referenceLine, List<? extends Tree> items) {
    int expectedLine = referenceLine + 1;

    for (Tree item : items) {
      if (getStartLine(item) < expectedLine) {
        return true;
      }

      if (item.is(Kind.ARRAY_INITIALIZER_BRACKET, Kind.FUNCTION_EXPRESSION)) {
        expectedLine = getLastLine(item) + 1;
      } else {
        expectedLine++;
      }
    }

    return false;
  }

  private static int getStartLine(Tree tree) {
    return ((PHPTree) tree).getLine();
  }

  private static int getLastLine(Tree tree) {
    return ((PHPTree) tree).getLastToken().line();
  }

  private int startColumnForLine(int line) {
    return startColumnByLine.get(line);
  }

}
