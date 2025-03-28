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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.php.PHPTreeModelTest;
import org.sonar.php.parser.PHPLexicalGrammar;
import org.sonar.php.tree.impl.PHPTree;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;

import static org.assertj.core.api.Assertions.assertThat;

// Suppressing issues about too many assertions
@SuppressWarnings("java:S5961")
class ExpressionPrecedenceTest extends PHPTreeModelTest {

  @Test
  void operatorPrecedenceAndAssociativity() {
    assertPrecedence("2 ** 3 ** 4", "2 ** (3 ** 4)");
    assertPrecedence("2 ** 3 * 4", "(2 ** 3) * 4");
    assertPrecedence("2 * 3 ** 4", "2 * (3 ** 4)");
    assertPrecedence("2 * 3 * 4", "(2 * 3) * 4");
    assertPrecedence("2 * 3 / 4", "(2 * 3) / 4");
    assertPrecedence("2 / 3 * 4", "(2 / 3) * 4");
    assertPrecedence("2 / 3 % 4", "(2 / 3) % 4");
    assertPrecedence("2 % 3 / 4", "(2 % 3) / 4");
    assertPrecedence("2 * 3 + 4", "(2 * 3) + 4");
    assertPrecedence("2 + 3 * 4", "2 + (3 * 4)");
    assertPrecedence("2 + 3 + 4", "(2 + 3) + 4");
    assertPrecedence("2 + 3 - 4", "(2 + 3) - 4");
    assertPrecedence("2 - 3 + 4", "(2 - 3) + 4");
    assertPrecedence("2 - 3 . 4", "(2 - 3) . 4");
    assertPrecedence("2 . 3 - 4", "(2 . 3) - 4");
    assertPrecedence("2 + 3 << 4", "(2 + 3) << 4");
    assertPrecedence("2 << 3 + 4", "2 << (3 + 4)");
    assertPrecedence("2 << 3 << 4", "(2 << 3) << 4");
    assertPrecedence("2 << 3 >> 4", "(2 << 3) >> 4");
    assertPrecedence("2 >> 3 << 4", "(2 >> 3) << 4");
    assertPrecedence("2 << 3 < 4", "(2 << 3) < 4");
    assertPrecedence("2 < 3 << 4", "2 < (3 << 4)");
    assertPrecedence("2 < 3 < 4", "(2 < 3) < 4");
    assertPrecedence("2 < 3 <= 4", "(2 < 3) <= 4");
    assertPrecedence("2 <= 3 < 4", "(2 <= 3) < 4");
    assertPrecedence("2 <= 3 > 4", "(2 <= 3) > 4");
    assertPrecedence("2 > 3 <= 4", "(2 > 3) <= 4");
    assertPrecedence("2 > 3 >= 4", "(2 > 3) >= 4");
    assertPrecedence("2 >= 3 > 4", "(2 >= 3) > 4");
    assertPrecedence("2 < 3 == 4", "(2 < 3) == 4");
    assertPrecedence("2 == 3 < 4", "2 == (3 < 4)");
    assertPrecedence("2 == 3 == 4", "(2 == 3) == 4");
    assertPrecedence("2 == 3 != 4", "(2 == 3) != 4");
    assertPrecedence("2 != 3 == 4", "(2 != 3) == 4");
    assertPrecedence("2 != 3 === 4", "(2 != 3) === 4");
    assertPrecedence("2 === 3 != 4", "(2 === 3) != 4");
    assertPrecedence("2 === 3 !== 4", "(2 === 3) !== 4");
    assertPrecedence("2 !== 3 === 4", "(2 !== 3) === 4");
    assertPrecedence("2 !== 3 <> 4", "(2 !== 3) <> 4");
    assertPrecedence("2 <> 3 !== 4", "(2 <> 3) !== 4");
    assertPrecedence("2 <> 3 <=> 4", "(2 <> 3) <=> 4");
    assertPrecedence("2 <=> 3 <> 4", "(2 <=> 3) <> 4");
    assertPrecedence("2 == 3 & 4", "(2 == 3) & 4");
    assertPrecedence("2 & 3 == 4", "2 & (3 == 4)");
    assertPrecedence("2 & 3 & 4", "(2 & 3) & 4");
    assertPrecedence("2 & 3 ^ 4", "(2 & 3) ^ 4");
    assertPrecedence("2 ^ 3 & 4", "2 ^ (3 & 4)");
    assertPrecedence("2 ^ 3 ^ 4", "(2 ^ 3) ^ 4");
    assertPrecedence("2 ^ 3 | 4", "(2 ^ 3) | 4");
    assertPrecedence("2 | 3 ^ 4", "2 | (3 ^ 4)");
    assertPrecedence("2 | 3 | 4", "(2 | 3) | 4");
    assertPrecedence("2 | 3 && 4", "(2 | 3) && 4");
    assertPrecedence("2 && 3 | 4", "2 && (3 | 4)");
    assertPrecedence("2 && 3 && 4", "(2 && 3) && 4");
    assertPrecedence("2 && 3 || 4", "(2 && 3) || 4");
    assertPrecedence("2 || 3 && 4", "2 || (3 && 4)");
    assertPrecedence("2 || 3 || 4", "(2 || 3) || 4");
    assertPrecedence("2 || 3 ?? 4", "(2 || 3) ?? 4");
    assertPrecedence("2 ?? 3 || 4", "2 ?? (3 || 4)");
    assertPrecedence("2 ?? 3 ?? 4", "2 ?? (3 ?? 4)");
    assertPrecedence("2 and $a = 4", "2 and ($a = 4)");
    assertPrecedence("2 and 3 and 4", "(2 and 3) and 4");
    assertPrecedence("2 and 3 xor 4", "(2 and 3) xor 4");
    assertPrecedence("2 xor 3 and 4", "2 xor (3 and 4)");
    assertPrecedence("2 xor 3 xor 4", "(2 xor 3) xor 4");
    assertPrecedence("2 xor 3 or 4", "(2 xor 3) or 4");
    assertPrecedence("2 or 3 xor 4", "2 or (3 xor 4)");
    assertPrecedence("2 or 3 or 4", "(2 or 3) or 4");
    assertPrecedence("$a ?? $b = 4", "$a ?? ($b = 4)");
    assertPrecedence("!!$a", "! (! $a)");
    assertPrecedence("++ $a --", "++ ($a --)");
    assertPrecedence("++ -- $a", "++ (-- $a)");
    assertPrecedence("(int) $a ++", "( int ) ($a ++)");
    assertPrecedence("! $a instanceof B", "! ($a instanceof B)");
    assertPrecedence("$a = true ? 0 : true ? 1 : 2", "$a = ((true ? 0 : true) ? 1 : 2)");
    assertPrecedence("$x &&  $y ? $a : $b", "($x && $y) ? $a : $b");
    assertPrecedence("$x and $y ? $a : $b", "$x and ($y ? $a : $b)");
    assertPrecedence("- 3 ** 2", "- (3 ** 2)");
    assertPrecedence("(int) $a ** 2", "( int ) ($a ** 2)");
    assertPrecedence("throw $a || $b", "throw ($a || $b)");
    assertPrecedence("$a || throw $b", "$a || (throw $b)");
    assertPrecedence("throw $a && $b", "throw ($a && $b)");
    assertPrecedence("$a && throw $b", "$a && (throw $b)");
  }

  @Test
  void assignment() {
    assertPrecedence("$a = 3 ?? 4", "$a = (3 ?? 4)");
    assertPrecedence("$a = $b = 4", "$a = ($b = 4)");
    assertPrecedence("$a = $b += 4", "$a = ($b += 4)");
    assertPrecedence("$a += $b = 4", "$a += ($b = 4)");
    assertPrecedence("$a += $b -= 4", "$a += ($b -= 4)");
    assertPrecedence("$a -= $b += 4", "$a -= ($b += 4)");
    assertPrecedence("$a -= $b *= 4", "$a -= ($b *= 4)");
    assertPrecedence("$a *= $b -= 4", "$a *= ($b -= 4)");
    assertPrecedence("$a *= $b **= 4", "$a *= ($b **= 4)");
    assertPrecedence("$a **= $b *= 4", "$a **= ($b *= 4)");
    assertPrecedence("$a **= $b /= 4", "$a **= ($b /= 4)");
    assertPrecedence("$a /= $b **= 4", "$a /= ($b **= 4)");
    assertPrecedence("$a /= $b .= 4", "$a /= ($b .= 4)");
    assertPrecedence("$a .= $b /= 4", "$a .= ($b /= 4)");
    assertPrecedence("$a .= $b %= 4", "$a .= ($b %= 4)");
    assertPrecedence("$a %= $b .= 4", "$a %= ($b .= 4)");
    assertPrecedence("$a %= $b &= 4", "$a %= ($b &= 4)");
    assertPrecedence("$a &= $b %= 4", "$a &= ($b %= 4)");
    assertPrecedence("$a &= $b |= 4", "$a &= ($b |= 4)");
    assertPrecedence("$a |= $b &= 4", "$a |= ($b &= 4)");
    assertPrecedence("$a |= $b ^= 4", "$a |= ($b ^= 4)");
    assertPrecedence("$a ^= $b |= 4", "$a ^= ($b |= 4)");
    assertPrecedence("$a ^= $b <<= 4", "$a ^= ($b <<= 4)");
    assertPrecedence("$a <<= $b ^= 4", "$a <<= ($b ^= 4)");
    assertPrecedence("$a <<= $b >>= 4", "$a <<= ($b >>= 4)");
    assertPrecedence("$a >>= $b <<= 4", "$a >>= ($b <<= 4)");
    assertPrecedence("$a = 3 and 4", "($a = 3) and 4");
    assertPrecedence("4 and $a = 3", "4 and ($a = 3)");
    assertPrecedence("4 && $a = 3", "4 && ($a = 3)");
    assertPrecedence("$a = 3 && 4", "$a = (3 && 4)");
    assertPrecedence("$x and $a = $a ? 1 : 2", "$x and ($a = ($a ? 1 : 2))");
    assertPrecedence("throw $a = $b", "throw ($a = $b)");
    assertPrecedence("42 + match ($a) {default=>0}", "42 + (match ( $a ) { (default => 0) })");
  }

  private void assertPrecedence(String code, String expected) {
    ExpressionTree expression = parse(code, PHPLexicalGrammar.EXPRESSION);
    String actual = dumpWithParentheses(expression).stream().collect(Collectors.joining(" "));
    assertThat(actual).isEqualTo(expected);
  }

  private static List<String> dumpWithParentheses(@Nullable Tree tree) {
    if (tree == null) {
      return Collections.emptyList();
    } else if (tree instanceof SyntaxToken) {
      return Collections.singletonList(((SyntaxToken) tree).text());
    } else {
      List<String> children = new ArrayList<>();
      Iterator<Tree> iterator = ((PHPTree) tree).childrenIterator();
      while (iterator.hasNext()) {
        List<String> child = dumpWithParentheses(iterator.next());
        if (child.size() == 1) {
          children.add(child.get(0));
        } else if (child.size() > 1) {
          children.add("(" + child.stream().collect(Collectors.joining(" ")) + ")");
        }
      }
      return children;
    }
  }

}
