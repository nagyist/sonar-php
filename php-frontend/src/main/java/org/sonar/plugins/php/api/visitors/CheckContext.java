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
package org.sonar.plugins.php.api.visitors;

import java.io.File;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.Beta;
import org.sonar.plugins.php.api.cache.CacheContext;
import org.sonar.plugins.php.api.symbols.SymbolTable;
import org.sonar.plugins.php.api.tree.CompilationUnitTree;
import org.sonar.plugins.php.api.tree.Tree;

public interface CheckContext {

  /**
   * @return the top tree node of the current file AST representation.
   */
  CompilationUnitTree tree();

  /**
   *
   * <p> To add secondary locations and cost use {@link PreciseIssue#secondary(Tree, String)} and {@link PreciseIssue#cost(double)}. Note, that these calls could be chained.
   * <pre>
   *   newIssue(myCheck, primaryTree, "Primary message")
   *     .secondary(secondaryTree1, "Secondary message")
   *     .secondary(secondaryTree2, null)
   *     .cost(3);
   * </pre>
   *
   * @param check the instance of the rule for which issue should be created
   * @param tree primary location for issue
   * @param message primary message of the issue
   * @return issue with precise location
   */
  PreciseIssue newIssue(PHPCheck check, Tree tree, String message);

  /**
   *
   * <p> To add secondary locations and cost use {@link PreciseIssue#secondary(Tree, String)} and {@link PreciseIssue#cost(double)}. Note, that these calls could be chained.
   * <pre>
   *   newIssue(myCheck, primaryLocationStartTree, primaryLocationEndTree, "Primary message")
   *     .secondary(secondaryTree1, "Secondary message")
   *     .secondary(secondaryTree2, null)
   *     .cost(3);
   * </pre>
   *
   * @param check the instance of the rule for which issue should be created
   * @param startTree start of this tree will be the start of primary location of the issue
   * @param endTree end of this tree will be the end of primary location of the issue
   * @param message primary message of the issue
   * @return issue with precise location
   */
  PreciseIssue newIssue(PHPCheck check, Tree startTree, Tree endTree, String message);

  /**
   * Report an issue with precise issue location based on lines and columns without relaying on tree elements
   *
   * @param check the instance of the rule for which issue should be created
   * @param issueLocation issue location within a file including the location message
   * @return issue with precise location
   * @since 3.32
   */
  PreciseIssue newIssue(PHPCheck check, IssueLocation issueLocation);

  /**
   *
   * <p> To add cost use {@link LineIssue#cost(double)}.
   * <pre>
   *   newLineIssue(myCheck, 42, "Message")
   *     .cost(3);
   * </pre>
   *
   * @param check the instance of the rule for which issue should be created
   * @param line position of the issue in the file
   * @param message message of the issue
   * @return issue with line location
   */
  LineIssue newLineIssue(PHPCheck check, int line, String message);

  /**
   *
   * <p> To add cost use {@link FileIssue#cost(double)}.
   * <pre>
   *   newFileIssue(myCheck, "Message")
   *     .cost(3);
   * </pre>
   *
   * @param check the instance of the rule for which issue should be created
   * @param message message of the issue
   * @return issue at file level
   */
  FileIssue newFileIssue(PHPCheck check, String message);

  List<PhpIssue> getIssues();

  SymbolTable symbolTable();

  /**
   * @return the current file
   */
  PhpFile getPhpFile();

  @CheckForNull
  File getWorkingDirectory();

  @Beta
  CacheContext cacheContext();

  /**
   * Determine if the current file uses a specific framework.
   * @return the framework used by the current file, or {@link SymbolTable.Framework#EMPTY} if the file does not use any framework.
   */
  default SymbolTable.Framework getFramework() {
    return symbolTable().getFramework();
  }
}
