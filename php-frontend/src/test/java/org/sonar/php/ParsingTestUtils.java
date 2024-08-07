/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.php;

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.sonar.php.parser.PHPLexicalGrammar;
import org.sonar.php.parser.PHPParserBuilder;
import org.sonar.plugins.php.api.tree.CompilationUnitTree;
import org.sonar.plugins.php.api.tree.Tree;

public class ParsingTestUtils {

  public static ActionParser<Tree> p = PHPParserBuilder.createParser(PHPLexicalGrammar.COMPILATION_UNIT);

  public static CompilationUnitTree parse(String filename) {
    File file = new File("src/test/resources/", filename);
    return parse(file);
  }

  public static CompilationUnitTree parse(File file) {
    ActionParser<Tree> parser = PHPParserBuilder.createParser();
    return (CompilationUnitTree) parser.parse(file);
  }

  public static CompilationUnitTree parseSource(String sourceCode) {
    ActionParser<Tree> parser = PHPParserBuilder.createParser();
    return (CompilationUnitTree) parser.parse(sourceCode);
  }

  public static String asCode(String... args) {
    return StringUtils.join(args, System.lineSeparator());
  }
}
