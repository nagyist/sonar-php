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
package org.sonar.php.symbols;

import org.sonar.plugins.php.api.visitors.LocationInFile;

public enum UnknownLocationInFile implements LocationInFile {

  UNKNOWN_LOCATION;

  @Override
  public String filePath() {
    return "[unknown file]";
  }

  @Override
  public int startLine() {
    return 1;
  }

  @Override
  public int startLineOffset() {
    return 0;
  }

  @Override
  public int endLine() {
    return 1;
  }

  @Override
  public int endLineOffset() {
    return 1;
  }
}
