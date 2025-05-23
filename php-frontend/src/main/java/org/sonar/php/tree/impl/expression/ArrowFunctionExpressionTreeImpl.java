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
package org.sonar.php.tree.impl.expression;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.php.tree.impl.PHPTree;
import org.sonar.php.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.php.utils.collections.IteratorUtils;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.declaration.AttributeGroupTree;
import org.sonar.plugins.php.api.tree.declaration.ParameterListTree;
import org.sonar.plugins.php.api.tree.declaration.ReturnTypeClauseTree;
import org.sonar.plugins.php.api.tree.expression.ArrowFunctionExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.visitors.VisitorCheck;

public class ArrowFunctionExpressionTreeImpl extends PHPTree implements ArrowFunctionExpressionTree {

  private static final Kind KIND = Kind.ARROW_FUNCTION_EXPRESSION;

  private final List<AttributeGroupTree> attributeGroups;
  private final InternalSyntaxToken staticToken;
  private final InternalSyntaxToken fnToken;
  private final InternalSyntaxToken referenceToken;
  private final ParameterListTree parameters;
  private final ReturnTypeClauseTree returnTypeClause;
  private final InternalSyntaxToken doubleArrowToken;
  private final ExpressionTree body;

  public ArrowFunctionExpressionTreeImpl(
    List<AttributeGroupTree> attributeGroups,
    @Nullable InternalSyntaxToken staticToken,
    InternalSyntaxToken fnToken,
    @Nullable InternalSyntaxToken referenceToken,
    ParameterListTree parameters,
    @Nullable ReturnTypeClauseTree returnTypeClause,
    InternalSyntaxToken doubleArrowToken,
    ExpressionTree body) {
    this.attributeGroups = attributeGroups;
    this.staticToken = staticToken;
    this.fnToken = fnToken;
    this.referenceToken = referenceToken;
    this.parameters = parameters;
    this.returnTypeClause = returnTypeClause;
    this.doubleArrowToken = doubleArrowToken;
    this.body = body;
  }

  @Nullable
  @Override
  public SyntaxToken staticToken() {
    return staticToken;
  }

  @Nullable
  @Override
  public SyntaxToken referenceToken() {
    return referenceToken;
  }

  @Override
  public List<AttributeGroupTree> attributeGroups() {
    return attributeGroups;
  }

  @Override
  public SyntaxToken functionToken() {
    return fnToken;
  }

  @Nullable
  @Override
  public ReturnTypeClauseTree returnTypeClause() {
    return returnTypeClause;
  }

  @Override
  public ParameterListTree parameters() {
    return parameters;
  }

  @Override
  public SyntaxToken doubleArrowToken() {
    return doubleArrowToken;
  }

  @Override
  public ExpressionTree body() {
    return body;
  }

  @Override
  public Kind getKind() {
    return KIND;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return IteratorUtils.concat(
      attributeGroups.iterator(),
      IteratorUtils.iteratorOf(staticToken, fnToken, referenceToken, parameters, returnTypeClause, doubleArrowToken, body));
  }

  @Override
  public void accept(VisitorCheck visitor) {
    visitor.visitArrowFunctionExpression(this);
  }

}
