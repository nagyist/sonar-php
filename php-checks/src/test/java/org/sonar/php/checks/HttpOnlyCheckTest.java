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

import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.php.CheckVerifier;

import static org.sonar.php.checks.phpini.PhpIniCheckTestUtils.check;
import static org.sonar.php.checks.phpini.PhpIniCheckTestUtils.issue;

class HttpOnlyCheckTest {

  private HttpOnlyCheck check = new HttpOnlyCheck();
  private File dir = new File("src/test/resources/checks/phpini");

  @Test
  void testPhpFile() {
    CheckVerifier.verify(new HttpOnlyCheck(), "HttpOnlyCheck.php");
  }

  @Test
  void testPhpIni() {
    check(check, new File(dir, "http_only.ini"));
    check(check, new File(dir, "empty.ini"), Collections.singletonList(issue("Set the \"session.cookie_httponly\" property to \"true\" if needed.")));
  }

}
