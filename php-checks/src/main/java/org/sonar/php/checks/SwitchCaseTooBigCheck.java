/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 SonarSource and Akram Ben Aissi
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.php.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.php.parser.PHPGrammar;
import org.sonar.squidbridge.checks.SquidCheck;

@Rule(
  key = "S1151",
  priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MAJOR)
public class SwitchCaseTooBigCheck extends SquidCheck<Grammar> {

  public static final int DEFAULT = 5;

  @RuleProperty(
    key = "max",
    defaultValue = "" + DEFAULT)
  int max = DEFAULT;

  @Override
  public void init() {
    subscribeTo(
      PHPGrammar.CASE_CLAUSE,
      PHPGrammar.DEFAULT_CLAUSE);
  }

  @Override
  public void visitNode(AstNode astNode) {
    int lines = Math.max(astNode.getNextAstNode().getTokenLine() - astNode.getTokenLine(), 1);
    if (lines > max) {
      getContext().createLineViolation(this, "Reduce this \"switch/case\" number of lines from {0} to at most {1}, for example by extracting code into function.",
        astNode, lines, max);
    }
  }
}
