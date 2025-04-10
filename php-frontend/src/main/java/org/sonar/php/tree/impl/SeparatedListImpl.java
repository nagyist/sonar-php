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
package org.sonar.php.tree.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import org.sonar.api.utils.Preconditions;
import org.sonar.plugins.php.api.tree.SeparatedList;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;

public class SeparatedListImpl<T extends Tree> implements SeparatedList<T> {

  private final List<T> list;
  private final List<SyntaxToken> separators;

  public SeparatedListImpl(List<T> list, List<SyntaxToken> separators) {
    Preconditions.checkArgument(
      list.size() == separators.size() + 1 || list.size() == separators.size(),
      "Instanciating a SeparatedList with inconsistent number of elements (%s) and separators (%s)",
      list.size(), separators.size());
    this.list = list;
    this.separators = separators;
  }

  public static <T extends Tree> SeparatedListImpl<T> empty() {
    return new SeparatedListImpl<>(Collections.emptyList(), Collections.emptyList());
  }

  @Override
  public SyntaxToken getSeparator(int i) {
    return separators.get(i);
  }

  @Override
  public List<SyntaxToken> getSeparators() {
    return separators;
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return list.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return list.iterator();
  }

  @Override
  public Object[] toArray() {
    return list.toArray();
  }

  @Override
  public <U> U[] toArray(U[] a) {
    return list.toArray(a);
  }

  @Override
  public boolean add(T e) {
    return list.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return list.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    return list.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    return list.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public T get(int index) {
    return list.get(index);
  }

  @Override
  public T set(int index, T element) {
    return list.set(index, element);
  }

  @Override
  public void add(int index, T element) {
    list.add(index, element);
  }

  @Override
  public T remove(int index) {
    return list.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }

  @Override
  public ListIterator<T> listIterator() {
    return list.listIterator();
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return list.listIterator(index);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return list.subList(fromIndex, toIndex);
  }

  @Override
  public Iterator<Tree> elementsAndSeparators(final Function<T, ? extends Tree> elementTransformer) {
    return new ElementAndSeparatorIterator(elementTransformer);
  }

  @Override
  public Iterator<Tree> elementsAndSeparators() {
    return elementsAndSeparators(Function.identity());
  }

  private final class ElementAndSeparatorIterator implements Iterator<Tree> {

    private final Function<T, ? extends Tree> elementTransformer;
    private final Iterator<T> elementIterator = list.iterator();
    private final Iterator<SyntaxToken> separatorIterator = separators.iterator();
    private boolean nextIsElement = true;

    private ElementAndSeparatorIterator(Function<T, ? extends Tree> elementTransformer) {
      this.elementTransformer = elementTransformer;
    }

    @Override
    public boolean hasNext() {
      return elementIterator.hasNext() || separatorIterator.hasNext();
    }

    @Override
    public Tree next() {
      Tree next = nextIsElement ? elementTransformer.apply(elementIterator.next()) : separatorIterator.next();
      nextIsElement = !nextIsElement;
      return next;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
