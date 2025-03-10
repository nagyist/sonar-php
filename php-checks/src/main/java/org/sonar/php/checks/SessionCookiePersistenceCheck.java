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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.php.checks.utils.FunctionUsageCheck;
import org.sonar.php.ini.BasePhpIniIssue;
import org.sonar.php.ini.PhpIniCheck;
import org.sonar.php.ini.PhpIniIssue;
import org.sonar.php.ini.tree.Directive;
import org.sonar.php.ini.tree.PhpIniFile;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.declaration.CallArgumentTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.LiteralTree;

@Rule(key = "S3332")
public class SessionCookiePersistenceCheck extends FunctionUsageCheck implements PhpIniCheck {

  private static final String PHP_INI_MESSAGE = "Configure \"session.cookie_lifetime\" to 0.";
  private static final String PHP_CODE_MESSAGE = "Set \"lifetime\" parameter to \"0\".";

  @Override
  public List<PhpIniIssue> analyze(PhpIniFile phpIniFile) {
    List<PhpIniIssue> issues = new ArrayList<>();
    for (Directive directive : phpIniFile.directivesForName("session.cookie_lifetime")) {
      String value = directive.value().text();
      if (!"0".equals(value) && !"\"0\"".equals(value)) {
        issues.add(BasePhpIniIssue.newIssue(PHP_INI_MESSAGE).line(directive.name().line()));
      }
    }
    return issues;
  }

  @Override
  protected Set<String> lookedUpFunctionNames() {
    return Set.of("session_set_cookie_params");
  }

  @Override
  protected void checkFunctionCall(FunctionCallTree functionCall) {
    Optional<CallArgumentTree> lifetimeArgument = CheckUtils.argument(functionCall, "lifetime", 0);
    if (lifetimeArgument.isPresent()) {
      ExpressionTree lifetimeArgumentValue = lifetimeArgument.get().value();
      if (lifetimeArgumentValue.is(Kind.NUMERIC_LITERAL)) {
        LiteralTree literal = (LiteralTree) lifetimeArgumentValue;
        if (!"0".equals(literal.value())) {
          context().newIssue(this, lifetimeArgumentValue, PHP_CODE_MESSAGE);
        }
      }
    }
  }

}
