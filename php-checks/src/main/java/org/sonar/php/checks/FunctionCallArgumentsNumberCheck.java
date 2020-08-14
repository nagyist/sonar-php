/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.php.checks;

import com.google.common.collect.Iterables;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.php.symbols.FunctionSymbol;
import org.sonar.php.symbols.Parameter;
import org.sonar.php.symbols.Symbols;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.declaration.NamespaceNameTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

import static java.util.Objects.requireNonNull;

@Rule(key = "S930")
public class FunctionCallArgumentsNumberCheck extends PHPVisitorCheck {

  private static final String MESSAGE = "\"%s\" expects %d argument%s, but %d %s provided. %s";
  private static final String MESSAGE_FEWER = "Add more arguments or define default values.";
  private static final String MESSAGE_MORE = "Reduce provided arguments or add more parameters.";

  private int actualArguments;

  @Override
  public void visitFunctionCall(FunctionCallTree tree) {
    if (tree.callee().is(Tree.Kind.NAMESPACE_NAME) && !requireNonNull(tree.getParent()).is(Tree.Kind.NEW_EXPRESSION)) {
      NamespaceNameTree callee = (NamespaceNameTree) tree.callee();
      FunctionSymbol symbol = Symbols.getFunction(callee);

      if (!symbol.isUnknownSymbol() && !symbol.hasFuncGetArgs()) {
        actualArguments = tree.arguments().size();
        List<Parameter> parameters = symbol.parameters();

        if (!hasEllipsisOperator(parameters) && actualArguments > maxArguments(parameters)) {
          addIssue(callee, symbol, MESSAGE_MORE, maxArguments(parameters));
        } else if (actualArguments < minArguments(parameters)) {
          addIssue(callee, symbol, MESSAGE_FEWER, minArguments(parameters));
        }
      }
    }

    super.visitFunctionCall(tree);
  }

  private void addIssue(NamespaceNameTree callee, FunctionSymbol symbol, String messageAddition, int expectedArguments) {
    String expectedWord = expectedArguments == 1 ? "" : "s";
    String actualWord = actualArguments == 1 ? "is" : "were";
    newIssue(callee, String.format(MESSAGE, callee.fullName(), expectedArguments, expectedWord, actualArguments, actualWord, messageAddition))
      .secondary(symbol.location(), null);
  }

  private static boolean hasEllipsisOperator(List<Parameter> parameters) {
    return !parameters.isEmpty() && Iterables.getLast(parameters).hasEllipsisOperator();
  }

  private static int minArguments(List<Parameter> parameters) {
    return (int) parameters.stream()
      .filter(p -> !p.hasDefault())
      .count();
  }

  private static int maxArguments(List<Parameter> parameters) {
    return parameters.size();
  }
}