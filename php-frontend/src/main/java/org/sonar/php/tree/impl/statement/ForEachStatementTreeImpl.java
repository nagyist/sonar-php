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
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.tree.statement.ForEachStatementTree;
import org.sonar.plugins.php.api.tree.statement.StatementTree;
import org.sonar.plugins.php.api.visitors.VisitorCheck;

public class ForEachStatementTreeImpl extends PHPTree implements ForEachStatementTree {

  private final Kind kind;

  private final ForEachStatementHeader header;
  private final InternalSyntaxToken colonToken;
  private final List<StatementTree> statements;
  private final InternalSyntaxToken endforeachToken;
  private final InternalSyntaxToken eosToken;

  public ForEachStatementTreeImpl(ForEachStatementHeader header, StatementTree statement) {
    this(Kind.FOREACH_STATEMENT, header, null, Collections.singletonList(statement), null, null);
  }

  public ForEachStatementTreeImpl(
    ForEachStatementHeader header, InternalSyntaxToken colonToken,
    List<StatementTree> statements, InternalSyntaxToken endForEachToken, InternalSyntaxToken eosToken) {
    this(Kind.ALTERNATIVE_FOREACH_STATEMENT, header, colonToken, statements, endForEachToken, eosToken);
  }

  private ForEachStatementTreeImpl(
    Kind kind,
    ForEachStatementHeader header, @Nullable InternalSyntaxToken colonToken,
    List<StatementTree> statements, @Nullable InternalSyntaxToken endForEachToken, @Nullable InternalSyntaxToken eosToken) {
    this.header = header;
    this.colonToken = colonToken;
    this.statements = statements;
    this.endforeachToken = endForEachToken;
    this.eosToken = eosToken;

    this.kind = kind;
  }

  @Override
  public SyntaxToken foreachToken() {
    return header.foreachToken();
  }

  @Override
  public SyntaxToken openParenthesisToken() {
    return header.openParenthesisToken();
  }

  @Override
  public ExpressionTree expression() {
    return header.expression();
  }

  @Override
  public SyntaxToken asToken() {
    return header.asToken();
  }

  @Nullable
  @Override
  public ExpressionTree key() {
    return header.key();
  }

  @Nullable
  @Override
  public SyntaxToken doubleArrowToken() {
    return header.doubleArrowToken();
  }

  @Override
  public ExpressionTree value() {
    return header.value();
  }

  @Override
  public SyntaxToken closeParenthesisToken() {
    return header.closeParenthesisToken();
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
  public SyntaxToken endforeachToken() {
    return endforeachToken;
  }

  @Nullable
  @Override
  public SyntaxToken eosToken() {
    return eosToken;
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return IteratorUtils.concat(
      IteratorUtils.iteratorOf(foreachToken(), openParenthesisToken(), expression(), asToken(), key(), doubleArrowToken(), value(), closeParenthesisToken(), colonToken),
      statements.iterator(),
      IteratorUtils.iteratorOf(endforeachToken, eosToken));
  }

  @Override
  public void accept(VisitorCheck visitor) {
    visitor.visitForEachStatement(this);
  }

  /**
   * Utility class hidden from API (it's mainly created to avoid duplication in grammar)
   */
  public static class ForEachStatementHeader {

    private final InternalSyntaxToken foreachToken;
    private final InternalSyntaxToken openParenthesisToken;
    private final ExpressionTree expression;
    private final InternalSyntaxToken asToken;
    private final ExpressionTree key;
    private final InternalSyntaxToken doubleArrowToken;
    private final ExpressionTree value;
    private final InternalSyntaxToken closeParenthesisToken;

    public ForEachStatementHeader(
      InternalSyntaxToken foreachToken, InternalSyntaxToken openParenthesisToken,
      ExpressionTree expression, InternalSyntaxToken asToken, ExpressionTree key, InternalSyntaxToken doubleArrowToken, ExpressionTree value,
      InternalSyntaxToken closeParenthesisToken) {
      this.foreachToken = foreachToken;
      this.openParenthesisToken = openParenthesisToken;
      this.expression = expression;
      this.asToken = asToken;
      this.key = key;
      this.doubleArrowToken = doubleArrowToken;
      this.value = value;
      this.closeParenthesisToken = closeParenthesisToken;
    }

    public InternalSyntaxToken foreachToken() {
      return foreachToken;
    }

    public InternalSyntaxToken openParenthesisToken() {
      return openParenthesisToken;
    }

    public ExpressionTree expression() {
      return expression;
    }

    public InternalSyntaxToken asToken() {
      return asToken;
    }

    @Nullable
    public ExpressionTree key() {
      return key;
    }

    @Nullable
    public InternalSyntaxToken doubleArrowToken() {
      return doubleArrowToken;
    }

    public ExpressionTree value() {
      return value;
    }

    public InternalSyntaxToken closeParenthesisToken() {
      return closeParenthesisToken;
    }
  }
}
