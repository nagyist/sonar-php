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

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.api.utils.Version;
import org.sonar.php.checks.CheckList;

import static org.assertj.core.api.Assertions.assertThat;

class PHPRulesDefinitionTest {

  @Test
  void testActivationSonarLint() {
    RulesDefinition.Repository repository = buildRepository(8, 9);
    assertThat(repository).isNotNull();
    assertThat(repository.name()).isEqualTo("Sonar");
    assertThat(repository.language()).isEqualTo("php");
    assertThat(repository.rules()).hasSize(CheckList.getAllChecks().size());

    List<Rule> activated = repository.rules().stream().filter(Rule::activatedByDefault).collect(Collectors.toList());
    assertThat(activated).isNotEmpty();
    assertThat(activated.size()).isLessThan(CheckList.getAllChecks().size());
  }

  @Test
  void owaspSecurityStandard() {
    RulesDefinition.Repository repository_9_3 = buildRepository();
    RulesDefinition.Rule S5328_9_3 = repository_9_3.rule("S5328");
    assertThat(S5328_9_3).isNotNull();
    assertThat(S5328_9_3.securityStandards()).contains("owaspTop10-2021:a4");

    RulesDefinition.Repository repository = buildRepository(9, 2);
    RulesDefinition.Rule S5328 = repository.rule("S5328");
    assertThat(S5328).isNotNull();
    assertThat(S5328.securityStandards()).doesNotContain("owaspTop10-2021:a4");
  }

  private static RulesDefinition.Repository buildRepository() {
    return buildRepository(9, 3);
  }

  private static RulesDefinition.Repository buildRepository(int majorVersion, int minorVersion) {
    PHPRulesDefinition rulesDefinition = new PHPRulesDefinition(
      SonarRuntimeImpl.forSonarQube(Version.create(majorVersion, minorVersion), SonarQubeSide.SERVER, SonarEdition.DEVELOPER));
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    return context.repository("php");
  }
}
