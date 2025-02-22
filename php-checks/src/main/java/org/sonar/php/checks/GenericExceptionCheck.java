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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.php.api.tree.ScriptTree;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.declaration.NamespaceNameTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.NewExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ThrowExpressionTree;
import org.sonar.plugins.php.api.tree.statement.NamespaceStatementTree;
import org.sonar.plugins.php.api.tree.statement.UseClauseTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

@Rule(key = GenericExceptionCheck.KEY)
public class GenericExceptionCheck extends PHPVisitorCheck {

  public static final String KEY = "S112";
  public static final String MESSAGE = "Define and throw a dedicated exception instead of using a generic one.";

  private static final Set<String> RAW_EXCEPTIONS = Set.of("ErrorException", "RuntimeException", "Exception");
  private Set<String> importedGenericExceptions = new HashSet<>();
  private boolean inGlobalNamespace = true;

  @Override
  public void visitScript(ScriptTree tree) {
    importedGenericExceptions.clear();
    inGlobalNamespace = true;
    super.visitScript(tree);
  }

  @Override
  public void visitThrowExpression(ThrowExpressionTree tree) {
    NamespaceNameTree namespaceName = getThrownClassName(tree);

    if (namespaceName != null && isGenericException(namespaceName)) {
      context().newIssue(this, namespaceName, MESSAGE);
    }

    super.visitThrowExpression(tree);
  }

  @Override
  public void visitUseClause(UseClauseTree tree) {
    String qualifiedName = tree.namespaceName().qualifiedName();

    if (RAW_EXCEPTIONS.contains(qualifiedName)) {
      importedGenericExceptions.add(tree.alias() != null ? tree.alias().text() : qualifiedName);
    }

    super.visitUseClause(tree);
  }

  @Override
  public void visitNamespaceStatement(NamespaceStatementTree tree) {
    if (tree.namespaceName() != null) {
      inGlobalNamespace = false;
    }

    super.visitNamespaceStatement(tree);

    // Delimited namespace with curly braces
    if (tree.openCurlyBrace() != null) {
      inGlobalNamespace = true;
    }
  }

  private boolean isGenericException(NamespaceNameTree namespaceName) {
    return isImportedGenericException(namespaceName) || isGlobalNamespaceGenericException(namespaceName);

  }

  private boolean isImportedGenericException(NamespaceNameTree namespaceName) {
    return importedGenericExceptions.contains(namespaceName.fullName());
  }

  private boolean isGlobalNamespaceGenericException(NamespaceNameTree namespaceName) {
    return isFromGlobalNamespace(namespaceName) && RAW_EXCEPTIONS.contains(namespaceName.name().text());
  }

  private boolean isFromGlobalNamespace(NamespaceNameTree namespaceName) {
    return !namespaceName.hasQualifiers()
      && (namespaceName.isFullyQualified() || inGlobalNamespace);
  }

  @Nullable
  private static NamespaceNameTree getThrownClassName(ThrowExpressionTree tree) {
    if (tree.expression().is(Kind.NEW_EXPRESSION)) {
      NewExpressionTree newExpression = (NewExpressionTree) tree.expression();

      if (newExpression.expression().is(Kind.FUNCTION_CALL)) {
        FunctionCallTree functionCall = (FunctionCallTree) newExpression.expression();

        if (functionCall.callee().is(Kind.NAMESPACE_NAME)) {
          return (NamespaceNameTree) functionCall.callee();
        }
      }
    }
    return null;
  }

}
