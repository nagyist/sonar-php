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
import org.sonar.php.api.PHPPunctuator;
import org.sonar.php.parser.PHPGrammar;
import org.sonar.squidbridge.checks.SquidCheck;

@Rule(
  key = "S1125",
  priority = Priority.MINOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MINOR)
public class BooleanEqualityComparisonCheck extends SquidCheck<Grammar> {

  @Override
  public void init() {
    subscribeTo(
      PHPGrammar.UNARY_EXPR,
      PHPGrammar.EQUALITY_EXPR,
      PHPGrammar.LOGICAL_AND_EXPR,
      PHPGrammar.LOGICAL_OR_EXPR);
  }

  @Override
  public void visitNode(AstNode astNode) {
    AstNode boolLiteral = getBooleanLiteralFromExpresion(astNode);

    if (boolLiteral != null) {
      getContext().createLineViolation(this, "Remove the literal \"" + boolLiteral.getTokenOriginalValue() + "\" boolean value.", astNode);
    }
  }

  private static AstNode getBooleanLiteralFromExpresion(AstNode expression) {
    if (expression.is(PHPGrammar.UNARY_EXPR)) {
      return getBooleanLiteralFromUnaryExpression(expression);
    }

    AstNode leftExpr = expression.getFirstChild();
    AstNode rightExpr = expression.getLastChild();

    if (isBooleanLiteral(leftExpr)) {
      return leftExpr;
    } else if (isBooleanLiteral(rightExpr)) {
      return rightExpr;
    } else {
      return null;
    }
  }

  private static AstNode getBooleanLiteralFromUnaryExpression(AstNode unaryExpression) {
    AstNode boolLiteral = null;

    if (unaryExpression.getFirstChild().is(PHPPunctuator.BANG)) {
      AstNode expr = unaryExpression.getLastChild();

      if (isBooleanLiteral(expr)) {
        boolLiteral = expr;
      }
    }
    return boolLiteral;
  }

  private static boolean isBooleanLiteral(AstNode astNode) {
    return astNode.is(PHPGrammar.POSTFIX_EXPR)
      && astNode.getFirstChild().is(PHPGrammar.COMMON_SCALAR)
      && astNode.getFirstChild().getFirstChild().is(PHPGrammar.BOOLEAN_LITERAL);
  }
}
