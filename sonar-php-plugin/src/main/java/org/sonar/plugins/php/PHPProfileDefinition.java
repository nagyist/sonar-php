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
package org.sonar.plugins.php;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.php.checks.CheckList;
import org.sonar.plugins.php.api.Php;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/**
 * Sonar way profile.
 */
public final class PHPProfileDefinition implements BuiltInQualityProfilesDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(PHPProfileDefinition.class);

  public static final String SONAR_WAY_PROFILE = "Sonar way";
  public static final String SONAR_WAY_PATH = "org/sonar/l10n/php/rules/php/Sonar_way_profile.json";

  public static final String SECURITY_RULES_CLASS = "com.sonar.plugins.security.api.PhpRules";
  public static final String RULES_KEYS_METHOD_NAME = "getRuleKeys";
  public static final String REPOSITORY_KEYS_METHOD_NAME = "getRepositoryKey";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile(SONAR_WAY_PROFILE, Php.KEY);
    BuiltInQualityProfileJsonLoader.load(sonarWay, CheckList.REPOSITORY_KEY, SONAR_WAY_PATH);
    getSecurityRuleKeys(SECURITY_RULES_CLASS, RULES_KEYS_METHOD_NAME, REPOSITORY_KEYS_METHOD_NAME)
      .forEach(key -> sonarWay.activateRule(key.repository(), key.rule()));
    sonarWay.done();
  }

  static Set<RuleKey> getSecurityRuleKeys(String rulesClassName, String ruleKeyMethodName, String repositoryKeyMethodName) {
    try {

      Class<?> phpRulesClass = Class.forName(rulesClassName);
      Method getRuleKeysMethod = phpRulesClass.getMethod(ruleKeyMethodName);
      Set<String> ruleKeys = (Set<String>) getRuleKeysMethod.invoke(null);
      Method getRepositoryKeyMethod = phpRulesClass.getMethod(repositoryKeyMethodName);
      String repositoryKey = (String) getRepositoryKeyMethod.invoke(null);
      return ruleKeys.stream().map(k -> RuleKey.of(repositoryKey, k)).collect(Collectors.toSet());

    } catch (ClassNotFoundException e) {
      LOG.debug("com.sonar.plugins.security.api.PhpRules is not found, {}", securityRuleMessage(e));
    } catch (NoSuchMethodException e) {
      LOG.debug("Method not found on com.sonar.plugins.security.api.PhpRules, {}", securityRuleMessage(e));
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOG.debug("[{}] {}", e.getClass().getSimpleName(), securityRuleMessage(e));
    }

    return new HashSet<>();
  }

  private static String securityRuleMessage(Exception e) {
    return "no security rules added to Sonar way PHP profile: " + e.getMessage();
  }
}
