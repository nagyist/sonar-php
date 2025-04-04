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
package org.sonar.plugins.php.api.cfg;

import com.sonar.sslr.api.RecognitionException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.utils.Preconditions;
import org.sonar.php.tree.impl.PHPTree;
import org.sonar.php.utils.LiteralUtils;
import org.sonar.php.utils.collections.ListUtils;
import org.sonar.plugins.php.api.tree.ScriptTree;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.Tree.Kind;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.LiteralTree;
import org.sonar.plugins.php.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.php.api.tree.statement.BlockTree;
import org.sonar.plugins.php.api.tree.statement.BreakStatementTree;
import org.sonar.plugins.php.api.tree.statement.CaseClauseTree;
import org.sonar.plugins.php.api.tree.statement.ContinueStatementTree;
import org.sonar.plugins.php.api.tree.statement.DeclareStatementTree;
import org.sonar.plugins.php.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.php.api.tree.statement.ElseifClauseTree;
import org.sonar.plugins.php.api.tree.statement.ForEachStatementTree;
import org.sonar.plugins.php.api.tree.statement.ForStatementTree;
import org.sonar.plugins.php.api.tree.statement.GotoStatementTree;
import org.sonar.plugins.php.api.tree.statement.IfStatementTree;
import org.sonar.plugins.php.api.tree.statement.LabelTree;
import org.sonar.plugins.php.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.php.api.tree.statement.StatementTree;
import org.sonar.plugins.php.api.tree.statement.SwitchCaseClauseTree;
import org.sonar.plugins.php.api.tree.statement.SwitchStatementTree;
import org.sonar.plugins.php.api.tree.statement.ThrowStatementTree;
import org.sonar.plugins.php.api.tree.statement.TryStatementTree;
import org.sonar.plugins.php.api.tree.statement.WhileStatementTree;

/**
 * Builder of a {@link ControlFlowGraph} for a given {@link ScriptTree} or for the body of a function.
 * Implementation note: this class starts from the end and goes backward because it's easier to implement.
 */
class ControlFlowGraphBuilder {

  private final Set<PhpCfgBlock> blocks = new HashSet<>();
  private final PhpCfgEndBlock end = new PhpCfgEndBlock();

  private final LinkedList<Breakable> breakables = new LinkedList<>();
  private final Deque<PhpCfgBlock> throwTargets = new ArrayDeque<>();

  private final Deque<TryBodyEnd> exitTargets = new LinkedList<>();

  // key is label
  private final Map<String, PhpCfgBlock> labelledBlocks = new HashMap<>();
  // key is label, value is a list of blocks that jump to the label
  private final Map<String, List<PhpCfgBlock>> gotosWithoutTarget = new HashMap<>();
  private PhpCfgBlock start;

  ControlFlowGraphBuilder(List<? extends Tree> items) {
    throwTargets.push(end);
    exitTargets.push(new TryBodyEnd(end, end));
    start = build(items, createSimpleBlock(end));
    removeEmptyBlocks();
    blocks.add(end);
    computePredecessors();
  }

  ControlFlowGraph getGraph() {
    return new ControlFlowGraph(new HashSet<>(blocks), start, end);
  }

  private void computePredecessors() {
    for (PhpCfgBlock block : blocks) {
      for (CfgBlock successor : block.successors()) {
        ((PhpCfgBlock) successor).addPredecessor(block);
      }
    }
  }

  private void removeEmptyBlocks() {
    Map<PhpCfgBlock, PhpCfgBlock> emptyBlockReplacements = new HashMap<>();
    for (PhpCfgBlock block : blocks) {
      if (block.elements().isEmpty() && block.successors().size() == 1) {
        PhpCfgBlock firstNonEmptySuccessor = block.skipEmptyBlocks();
        emptyBlockReplacements.put(block, firstNonEmptySuccessor);
      }
    }

    blocks.removeAll(emptyBlockReplacements.keySet());

    for (PhpCfgBlock block : blocks) {
      block.replaceSuccessors(emptyBlockReplacements);
    }

    start = emptyBlockReplacements.getOrDefault(start, start);
  }

  private PhpCfgBlock build(List<? extends Tree> trees, PhpCfgBlock successor) {
    PhpCfgBlock currentBlock = successor;
    for (Tree tree : ListUtils.reverse(trees)) {
      currentBlock = build(tree, currentBlock);
    }

    return currentBlock;
  }

  private PhpCfgBlock build(Tree tree, PhpCfgBlock currentBlock) {
    return switch (tree.getKind()) {
      case TRY_STATEMENT -> buildTryStatement((TryStatementTree) tree, currentBlock);
      case THROW_STATEMENT -> buildThrowStatement((ThrowStatementTree) tree, currentBlock);
      case RETURN_STATEMENT -> buildReturnStatement((ReturnStatementTree) tree, currentBlock);
      case BREAK_STATEMENT -> buildBreakStatement((BreakStatementTree) tree, currentBlock);
      case CONTINUE_STATEMENT -> buildContinueStatement((ContinueStatementTree) tree, currentBlock);
      case GOTO_STATEMENT -> buildGotoStatement((GotoStatementTree) tree, currentBlock);
      case DO_WHILE_STATEMENT -> buildDoWhileStatement((DoWhileStatementTree) tree, currentBlock);
      case WHILE_STATEMENT, ALTERNATIVE_WHILE_STATEMENT -> buildWhileStatement((WhileStatementTree) tree, currentBlock);
      case IF_STATEMENT, ALTERNATIVE_IF_STATEMENT -> buildIfStatement((IfStatementTree) tree, currentBlock);
      case FOR_STATEMENT, ALTERNATIVE_FOR_STATEMENT -> buildForStatement((ForStatementTree) tree, currentBlock);
      case FOREACH_STATEMENT, ALTERNATIVE_FOREACH_STATEMENT -> buildForEachStatement((ForEachStatementTree) tree, currentBlock);
      case BLOCK -> buildBlock((BlockTree) tree, currentBlock);
      case SWITCH_STATEMENT, ALTERNATIVE_SWITCH_STATEMENT -> buildSwitchStatement((SwitchStatementTree) tree, currentBlock);
      case LABEL -> createLabelBlock((LabelTree) tree, currentBlock);
      case DECLARE_STATEMENT -> buildDeclareStatement((DeclareStatementTree) tree, currentBlock);
      case GLOBAL_STATEMENT, STATIC_STATEMENT, UNSET_VARIABLE_STATEMENT, EXPRESSION_LIST_STATEMENT, FUNCTION_DECLARATION, USE_STATEMENT, GROUP_USE_STATEMENT, CONSTANT_DECLARATION, NAMESPACE_STATEMENT, INLINE_HTML, EXPRESSION_STATEMENT, ECHO_TAG_STATEMENT -> {
        currentBlock.addElement(tree);
        yield currentBlock;
      }
      case TRAIT_DECLARATION, INTERFACE_DECLARATION, CLASS_DECLARATION, EMPTY_STATEMENT, ENUM_DECLARATION -> currentBlock;
      default -> throw new UnsupportedOperationException("Not supported tree kind " + tree.getKind());
    };
  }

  private PhpCfgBlock buildDeclareStatement(DeclareStatementTree declare, PhpCfgBlock successor) {
    List<StatementTree> statements = declare.statements();
    if (statements.isEmpty()) {
      successor.addElement(declare);
      return successor;
    }
    return build(statements, successor);
  }

  private PhpCfgBlock buildSwitchStatement(SwitchStatementTree tree, PhpCfgBlock successor) {
    ForwardingBlock defaultBlock = createForwardingBlock();
    defaultBlock.setSuccessor(successor);
    PhpCfgBlock nextCase = defaultBlock;
    PhpCfgBlock caseBody = successor;
    addBreakable(successor, successor);
    for (SwitchCaseClauseTree caseTree : ListUtils.reverse(tree.cases())) {
      caseBody = buildSubFlow(caseTree.statements(), caseBody);
      if (caseTree.is(Tree.Kind.CASE_CLAUSE)) {
        PhpCfgBranchingBlock caseBranch = createBranchingBlock(caseTree, caseBody, nextCase);
        caseBranch.addElement(((CaseClauseTree) caseTree).expression());
        nextCase = caseBranch;
      } else {
        // default case
        defaultBlock.setSuccessor(caseBody);
      }
    }
    removeBreakable();
    PhpCfgBlock block = createSimpleBlock(nextCase);
    block.addElement(tree.expression());
    return block;
  }

  private PhpCfgBlock buildTryStatement(TryStatementTree tree, PhpCfgBlock successor) {
    PhpCfgBlock exitBlock = exitTargets.peek().exitBlock;
    PhpCfgBlock finallyBlockEnd = createMultiSuccessorBlock(Set.of(successor, exitBlock));
    PhpCfgBlock finallyBlock;
    if (tree.finallyBlock() != null) {
      finallyBlock = build(tree.finallyBlock().statements(), finallyBlockEnd);
    } else {
      finallyBlock = finallyBlockEnd;
    }

    List<PhpCfgBlock> catchBlocks = tree.catchBlocks().stream()
      .map(catchBlockTree -> buildSubFlow(catchBlockTree.block().statements(), finallyBlock))
      .toList();

    if (catchBlocks.isEmpty()) {
      throwTargets.push(finallyBlock);
    } else {
      throwTargets.push(catchBlocks.get(0));
    }
    Set<PhpCfgBlock> bodySuccessors = new HashSet<>(catchBlocks);
    bodySuccessors.add(finallyBlock);
    PhpCfgBlock tryBodySuccessors = createMultiSuccessorBlock(bodySuccessors);
    addBreakable(tryBodySuccessors, tryBodySuccessors);
    exitTargets.push(new TryBodyEnd(tryBodySuccessors, finallyBlock));
    PhpCfgBlock tryBodyStartingBlock = build(tree.block().statements(), tryBodySuccessors);
    throwTargets.pop();
    exitTargets.pop();
    removeBreakable();

    return tryBodyStartingBlock;
  }

  private PhpCfgBlock buildThrowStatement(ThrowStatementTree tree, PhpCfgBlock successor) {
    // taking "latest" throw target is an estimation
    // In real a matching `catch` clause should be found (by exception type)
    PhpCfgBlock simpleBlock = createBlockWithSyntacticSuccessor(throwTargets.peek(), successor);
    simpleBlock.addElement(tree);
    return simpleBlock;
  }

  private PhpCfgBlock buildReturnStatement(ReturnStatementTree tree, PhpCfgBlock successor) {
    PhpCfgBlock simpleBlock = createBlockWithSyntacticSuccessor(exitTargets.peek().catchAndFinally, successor);
    simpleBlock.addElement(tree);
    return simpleBlock;
  }

  private PhpCfgBlock createLabelBlock(LabelTree tree, PhpCfgBlock currentBlock) {
    String label = tree.label().text();
    labelledBlocks.put(label, currentBlock);
    currentBlock.addElement(tree);
    List<PhpCfgBlock> gotoBlocks = gotosWithoutTarget.get(label);
    if (gotoBlocks != null) {
      gotoBlocks.forEach(gotoBlock -> gotoBlock.replaceSuccessor(end, currentBlock));
      gotosWithoutTarget.remove(label);
    }
    // create the block for the code above the label
    return createSimpleBlock(currentBlock);
  }

  private PhpCfgBlock buildGotoStatement(GotoStatementTree tree, PhpCfgBlock successor) {
    String label = tree.identifier().text();
    PhpCfgBlock gotoTarget = labelledBlocks.get(label);
    PhpCfgBlock newBlock;
    if (gotoTarget == null) {
      newBlock = createBlockWithSyntacticSuccessor(end, successor);
      List<PhpCfgBlock> gotosList = gotosWithoutTarget.computeIfAbsent(label, k -> new LinkedList<>());
      gotosList.add(newBlock);
    } else {
      newBlock = createBlockWithSyntacticSuccessor(gotoTarget, successor);
    }
    newBlock.addElement(tree);
    return newBlock;
  }

  private PhpCfgBlock buildBreakStatement(BreakStatementTree tree, PhpCfgBlock successor) {
    PhpCfgBlock newBlock = createBlockWithSyntacticSuccessor(getBreakable(tree.argument(), tree).breakTarget, successor);
    newBlock.addElement(tree);
    return newBlock;
  }

  private PhpCfgBlock buildContinueStatement(ContinueStatementTree tree, PhpCfgBlock successor) {
    PhpCfgBlock newBlock = createBlockWithSyntacticSuccessor(getBreakable(tree.argument(), tree).continueTarget, successor);
    newBlock.addElement(tree);
    return newBlock;
  }

  private Breakable getBreakable(@Nullable ExpressionTree argument, StatementTree jumpStmp) {
    try {
      int breakLevels = getBreakLevels(argument);
      return breakables.get(breakLevels - 1);
    } catch (IndexOutOfBoundsException e) {
      throw exception(jumpStmp, e);
    }
  }

  private static int getBreakLevels(@Nullable ExpressionTree argument) {
    if (argument == null) {
      return 1;
    }
    ExpressionTree levelsExpression = argument;
    if (levelsExpression.is(Kind.PARENTHESISED_EXPRESSION)) {
      levelsExpression = ((ParenthesisedExpressionTree) levelsExpression).expression();
    }
    if (!levelsExpression.is(Kind.NUMERIC_LITERAL)) {
      throw exception(argument);
    }
    try {
      int breakLevels = (int) LiteralUtils.longLiteralValue(((LiteralTree) levelsExpression).value());
      if (breakLevels == 0) {
        return 1;
      }
      return breakLevels;
    } catch (NumberFormatException e) {
      throw exception(argument, e);
    }
  }

  private static RecognitionException exception(Tree tree) {
    return new RecognitionException(((PHPTree) tree).getLine(), "Failed to build CFG");
  }

  private static RecognitionException exception(Tree tree, Throwable cause) {
    return new RecognitionException(((PHPTree) tree).getLine(), "Failed to build CFG", cause);
  }

  private PhpCfgBlock buildForEachStatement(ForEachStatementTree tree, PhpCfgBlock successor) {
    ForwardingBlock linkToCondition = createForwardingBlock();

    addBreakable(successor, linkToCondition);
    PhpCfgBlock loopBodyBlock = buildSubFlow(tree.statements(), linkToCondition);
    removeBreakable();

    PhpCfgBranchingBlock conditionBlock = createBranchingBlock(tree, loopBodyBlock, successor);
    conditionBlock.addElement(tree.expression());
    linkToCondition.setSuccessor(conditionBlock);
    return createSimpleBlock(conditionBlock);
  }

  private PhpCfgBlock buildForStatement(ForStatementTree tree, PhpCfgBlock successor) {
    // we need to reverse the 'update' and 'condition' and 'init' expressions
    // because they are sequential inside the ForStatementTree
    // (and not bottom-up like how we build the CFG)

    ForwardingBlock linkToCondition = createForwardingBlock();
    PhpCfgBlock updateBlock = createSimpleBlock(linkToCondition);
    ListUtils.reverse(tree.update()).forEach(updateBlock::addElement);

    addBreakable(successor, updateBlock);
    PhpCfgBlock loopBodyBlock = buildSubFlow(tree.statements(), updateBlock);
    removeBreakable();

    PhpCfgBranchingBlock conditionBlock = createBranchingBlock(tree, loopBodyBlock, successor);
    ListUtils.reverse(tree.condition()).forEach(conditionBlock::addElement);
    linkToCondition.setSuccessor(conditionBlock);

    PhpCfgBlock beforeFor = createSimpleBlock(conditionBlock);
    ListUtils.reverse(tree.init()).forEach(beforeFor::addElement);

    return beforeFor;
  }

  private PhpCfgBlock buildDoWhileStatement(DoWhileStatementTree tree, PhpCfgBlock successor) {
    ForwardingBlock linkToBody = createForwardingBlock();
    PhpCfgBranchingBlock conditionBlock = createBranchingBlock(tree, linkToBody, successor);
    conditionBlock.addElement(tree.condition().expression());

    addBreakable(successor, conditionBlock);
    PhpCfgBlock loopBodyBlock = buildSubFlow(Collections.singletonList(tree.statement()), conditionBlock);
    removeBreakable();

    linkToBody.setSuccessor(loopBodyBlock);
    return createSimpleBlock(loopBodyBlock);
  }

  private PhpCfgBlock buildWhileStatement(WhileStatementTree tree, PhpCfgBlock successor) {
    ForwardingBlock linkToCondition = createForwardingBlock();

    addBreakable(successor, linkToCondition);
    PhpCfgBlock loopBodyBlock = buildSubFlow(tree.statements(), linkToCondition);
    removeBreakable();

    PhpCfgBranchingBlock conditionBlock = createBranchingBlock(tree, loopBodyBlock, successor);
    conditionBlock.addElement(tree.condition().expression());
    linkToCondition.setSuccessor(conditionBlock);
    return createSimpleBlock(conditionBlock);
  }

  private void removeBreakable() {
    breakables.pop();
  }

  private void addBreakable(PhpCfgBlock breakTarget, PhpCfgBlock continueTarget) {
    breakables.push(new Breakable(breakTarget, continueTarget));
  }

  private ForwardingBlock createForwardingBlock() {
    ForwardingBlock block = new ForwardingBlock();
    blocks.add(block);
    return block;
  }

  private PhpCfgBlock buildBlock(BlockTree block, PhpCfgBlock successor) {
    return build(block.statements(), successor);
  }

  private PhpCfgBlock buildIfStatement(IfStatementTree tree, PhpCfgBlock successor) {
    PhpCfgBlock falseBlock = successor;
    if (tree.elseClause() != null) {
      falseBlock = buildSubFlow(tree.elseClause().statements(), successor);
    }
    if (!tree.elseifClauses().isEmpty()) {
      for (ElseifClauseTree elseifClause : ListUtils.reverse(tree.elseifClauses())) {
        falseBlock = buildElseIfStatement(elseifClause, successor, falseBlock);
      }
    }
    PhpCfgBlock trueBlock = buildSubFlow(tree.statements(), successor);
    PhpCfgBranchingBlock conditionBlock = createBranchingBlock(tree, trueBlock, falseBlock);
    conditionBlock.addElement(tree.condition().expression());
    return conditionBlock;
  }

  private PhpCfgBlock buildElseIfStatement(ElseifClauseTree tree, PhpCfgBlock ifSuccessor, PhpCfgBlock nextCondition) {
    PhpCfgBlock thenBlock = buildSubFlow(tree.statements(), ifSuccessor);
    PhpCfgBranchingBlock conditionBlock = createBranchingBlock(tree, thenBlock, nextCondition);
    conditionBlock.addElement(tree.condition().expression());
    return conditionBlock;
  }

  private PhpCfgBlock buildSubFlow(List<StatementTree> subFlowTree, PhpCfgBlock successor) {
    return build(subFlowTree, createSimpleBlock(successor));
  }

  private PhpCfgBranchingBlock createBranchingBlock(Tree branchingTree, PhpCfgBlock trueSuccessor, PhpCfgBlock falseSuccessor) {
    PhpCfgBranchingBlock block = new PhpCfgBranchingBlock(branchingTree, trueSuccessor, falseSuccessor);
    blocks.add(block);
    return block;
  }

  private PhpCfgBlock createMultiSuccessorBlock(Set<PhpCfgBlock> successors) {
    PhpCfgBlock block = new PhpCfgBlock(successors);
    blocks.add(block);
    return block;
  }

  private PhpCfgBlock createSimpleBlock(PhpCfgBlock successor) {
    PhpCfgBlock block = new PhpCfgBlock(successor);
    blocks.add(block);
    return block;
  }

  private PhpCfgBlock createBlockWithSyntacticSuccessor(PhpCfgBlock successor, PhpCfgBlock syntacticSuccessor) {
    PhpCfgBlock block = new PhpCfgBlock(successor, syntacticSuccessor);
    blocks.add(block);
    return block;
  }

  private static class ForwardingBlock extends PhpCfgBlock {

    private PhpCfgBlock successor;

    @Override
    public Set<CfgBlock> successors() {
      Preconditions.checkState(successor != null, "No successor was set on %s", this);
      return Set.of(successor);
    }

    @Override
    public void addElement(Tree element) {
      throw new UnsupportedOperationException("Cannot add an element to a forwarding block");
    }

    void setSuccessor(PhpCfgBlock successor) {
      this.successor = successor;
    }

    @Override
    public void replaceSuccessors(Map<PhpCfgBlock, PhpCfgBlock> replacements) {
      throw new UnsupportedOperationException("Cannot replace successors for a forwarding block");
    }
  }

  private static class Breakable {
    PhpCfgBlock breakTarget;
    PhpCfgBlock continueTarget;

    Breakable(PhpCfgBlock breakTarget, PhpCfgBlock continueTarget) {
      this.breakTarget = breakTarget;
      this.continueTarget = continueTarget;
    }
  }

  static class TryBodyEnd {
    final PhpCfgBlock catchAndFinally;
    final PhpCfgBlock exitBlock;

    TryBodyEnd(PhpCfgBlock catchAndFinally, PhpCfgBlock exitBlock) {
      this.catchAndFinally = catchAndFinally;
      this.exitBlock = exitBlock;
    }
  }
}
