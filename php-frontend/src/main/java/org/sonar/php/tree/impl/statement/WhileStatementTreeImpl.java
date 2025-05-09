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
package org.sonar.php.tree.impl.statement;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.php.tree.impl.PHPTree;
import org.sonar.php.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.php.utils.collections.IteratorUtils;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.tree.statement.StatementTree;
import org.sonar.plugins.php.api.tree.statement.WhileStatementTree;
import org.sonar.plugins.php.api.visitors.VisitorCheck;

public class WhileStatementTreeImpl extends PHPTree implements WhileStatementTree {

  private final Kind kind;

  private final InternalSyntaxToken whileToken;
  private final ParenthesisedExpressionTree condition;
  private final InternalSyntaxToken colonToken;
  private final List<StatementTree> statements;
  private final InternalSyntaxToken endwhileToken;
  private final InternalSyntaxToken eosToken;

  public WhileStatementTreeImpl(InternalSyntaxToken whileToken, ParenthesisedExpressionTree condition, StatementTree statement) {
    this.kind = Kind.WHILE_STATEMENT;

    this.whileToken = whileToken;
    this.condition = condition;
    this.statements = Collections.singletonList(statement);

    this.colonToken = null;
    this.endwhileToken = null;
    this.eosToken = null;
  }

  public WhileStatementTreeImpl(
    InternalSyntaxToken whileToken, ParenthesisedExpressionTree condition, InternalSyntaxToken colonToken,
    List<StatementTree> statements, InternalSyntaxToken endwhileToken, InternalSyntaxToken eosToken) {
    this.kind = Kind.ALTERNATIVE_WHILE_STATEMENT;

    this.whileToken = whileToken;
    this.condition = condition;
    this.statements = statements;

    this.colonToken = colonToken;
    this.endwhileToken = endwhileToken;
    this.eosToken = eosToken;
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return IteratorUtils.concat(
      IteratorUtils.iteratorOf(whileToken, condition, colonToken),
      statements.iterator(),
      IteratorUtils.iteratorOf(endwhileToken, eosToken));
  }

  @Override
  public SyntaxToken whileToken() {
    return whileToken;
  }

  @Override
  public ParenthesisedExpressionTree condition() {
    return condition;
  }

  @Nullable
  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public List<StatementTree> statements() {
    return statements;
  }

  @Nullable
  @Override
  public SyntaxToken endWhileToken() {
    return endwhileToken;
  }

  @Nullable
  @Override
  public SyntaxToken eosToken() {
    return eosToken;
  }

  @Override
  public void accept(VisitorCheck visitor) {
    visitor.visitWhileStatement(this);
  }
}
