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
package org.sonar.php.checks.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.php.checks.utils.FunctionUsageCheck;
import org.sonar.php.ini.BasePhpIniIssue;
import org.sonar.php.ini.PhpIniCheck;
import org.sonar.php.ini.PhpIniIssue;
import org.sonar.php.ini.tree.PhpIniFile;
import org.sonar.php.utils.collections.MapBuilder;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.declaration.CallArgumentTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.LiteralTree;

import static org.sonar.php.checks.utils.CheckUtils.getLowerCaseFunctionName;

@Rule(key = "S3331")
public class CookieDomainCheck extends FunctionUsageCheck implements PhpIniCheck {

  private static final String MESSAGE = "Specify at least a second-level cookie domain.";

  // The key is the function name, the value is the index of the 'domain' parameter
  private static final Map<String, Integer> FUNCTION_AND_PARAM_INDEX = MapBuilder.<String, Integer>builder()
    .put("setcookie", 4)
    .put("session_set_cookie_params", 2)
    .build();

  @Override
  protected Set<String> lookedUpFunctionNames() {
    return Collections.unmodifiableSet(FUNCTION_AND_PARAM_INDEX.keySet());
  }

  @Override
  protected void checkFunctionCall(FunctionCallTree tree) {
    int domainIndex = FUNCTION_AND_PARAM_INDEX.get(getLowerCaseFunctionName(tree));

    Optional<CallArgumentTree> domainArgument = CheckUtils.argument(tree, "domain", domainIndex);
    if (domainArgument.isPresent()) {
      ExpressionTree domainValue = CheckUtils.assignedValue(domainArgument.get().value());

      if (domainValue.is(Tree.Kind.REGULAR_STRING_LITERAL) && isFirstLevelDomain(((LiteralTree) domainValue).value())) {
        if (domainArgument.get().value() == domainValue) {
          context().newIssue(this, domainValue, MESSAGE);
        } else {
          context().newIssue(this, domainValue, MESSAGE).secondary(domainArgument.get(), MESSAGE);
        }
      }
    }
  }

  @Override
  public List<PhpIniIssue> analyze(PhpIniFile phpIniFile) {
    return phpIniFile.directivesForName("session.cookie_domain").stream()
      .filter(d -> isFirstLevelDomain(d.value().text()))
      .map(d -> BasePhpIniIssue.newIssue(MESSAGE).line(d.name().line()))
      .toList();
  }

  private static boolean isFirstLevelDomain(String domain) {
    String trimedFromQuotes = CheckUtils.trimQuotes(domain);
    return !trimedFromQuotes.isEmpty() && Arrays.stream(trimedFromQuotes.split("\\."))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .count() < 2;
  }

}
