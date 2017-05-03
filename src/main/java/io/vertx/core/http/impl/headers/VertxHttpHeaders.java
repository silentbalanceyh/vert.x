/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.core.http.impl.headers;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import io.vertx.core.MultiMap;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxHttpHeaders extends HttpHeaders implements MultiMap {

  private MultiMap set0(Iterable<Map.Entry<String, String>> map) {
    clear();
    for (Map.Entry<String, String> entry: map) {
      add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public MultiMap setAll(MultiMap headers) {
    return set0(headers);
  }

  @Override
  public MultiMap setAll(Map<String, String> headers) {
    return set0(headers.entrySet());
  }

  @Override
  public int size() {
    return names().size();
  }

  private static int index(int hash) {
    return hash & 0x0000000F;
  }

  private final VertxHttpHeaders.MapEntry[] entries = new VertxHttpHeaders.MapEntry[16];
  private final VertxHttpHeaders.MapEntry head = new VertxHttpHeaders.MapEntry(-1, null, null);

  public VertxHttpHeaders() {
    head.before = head.after = head;
  }

  public boolean contentLengthSet() {
    return contains(io.vertx.core.http.HttpHeaders.CONTENT_LENGTH);
  }

  public boolean contentTypeSet() {
    return contains(io.vertx.core.http.HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public VertxHttpHeaders add(CharSequence name, Object value) {
    int h = AsciiString.hashCode(name);
    int i = index(h);
    add0(h, i, name, (CharSequence) value);
    return this;
  }

  @Override
  public VertxHttpHeaders add(final String name, final String strVal) {
    int h = AsciiString.hashCode(name);
    int i = index(h);
    add0(h, i, name, strVal);
    return this;
  }

  @Override
  public VertxHttpHeaders add(String name, Iterable values) {
    int h = AsciiString.hashCode(name);
    int i = index(h);
    for (Object vstr: values) {
      add0(h, i, name, (String) vstr);
    }
    return this;
  }

  @Override
  public MultiMap addAll(MultiMap headers) {
    for (Map.Entry<String, String> entry: headers.entries()) {
      add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public MultiMap addAll(Map<String, String> map) {
    for (Map.Entry<String, String> entry: map.entrySet()) {
      add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  private void add0(int h, int i, final CharSequence name, final CharSequence value) {
    // Update the hash table.
    VertxHttpHeaders.MapEntry e = entries[i];
    VertxHttpHeaders.MapEntry newEntry;
    entries[i] = newEntry = new VertxHttpHeaders.MapEntry(h, name, value);
    newEntry.next = e;

    // Update the linked list.
    newEntry.addBefore(head);
  }

  @Override
  public VertxHttpHeaders remove(final String name) {
    Objects.requireNonNull(name, "name");
    int h = AsciiString.hashCode(name);
    int i = index(h);
    remove0(h, i, name);
    return this;
  }

  private void remove0(int h, int i, String name) {
    VertxHttpHeaders.MapEntry e = entries[i];
    if (e == null) {
      return;
    }

    for (;;) {
      if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
        e.remove();
        VertxHttpHeaders.MapEntry next = e.next;
        if (next != null) {
          entries[i] = next;
          e = next;
        } else {
          entries[i] = null;
          return;
        }
      } else {
        break;
      }
    }

    for (;;) {
      VertxHttpHeaders.MapEntry next = e.next;
      if (next == null) {
        break;
      }
      if (next.hash == h && AsciiString.contentEqualsIgnoreCase(name, next.key)) {
        e.next = next.next;
        next.remove();
      } else {
        e = next;
      }
    }
  }

  @Override
  public VertxHttpHeaders set(final String name, final String strVal) {
    return set0(name, strVal);
  }

  private VertxHttpHeaders set0(final String name, final CharSequence strVal) {
    int h = AsciiString.hashCode(name);
    int i = index(h);
    remove0(h, i, name);
    add0(h, i, name, strVal);
    return this;
  }

  @Override
  public VertxHttpHeaders set(final String name, final Iterable values) {
    Objects.requireNonNull(values, "values");

    int h = AsciiString.hashCode(name);
    int i = index(h);

    remove0(h, i, name);
    for (Object v: values) {
      if (v == null) {
        break;
      }
      add0(h, i, name, (String) v);
    }

    return this;
  }

  @Override
  public VertxHttpHeaders clear() {
    for (int i = 0; i < entries.length; i ++) {
      entries[i] = null;
    }
    head.before = head.after = head;
    return this;
  }

  @Override
  public String get(final String name) {
    return get((CharSequence) name);
  }

  private CharSequence get0(CharSequence name) {
    int h = AsciiString.hashCode(name);
    int i = index(h);
    VertxHttpHeaders.MapEntry e = entries[i];
    while (e != null) {
      if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
        return e.getValue();
      }
      e = e.next;
    }
    return null;
  }

  @Override
  public List<String> getAll(final String name) {
    Objects.requireNonNull(name, "name");

    LinkedList<String> values = new LinkedList<>();

    int h = AsciiString.hashCode(name);
    int i = index(h);
    VertxHttpHeaders.MapEntry e = entries[i];
    while (e != null) {
      if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
        values.addFirst(e.getValue().toString());
      }
      e = e.next;
    }
    return values;
  }

  @Override
  public void forEach(Consumer<? super Map.Entry<String, String>> action) {
    VertxHttpHeaders.MapEntry e = head.after;
    while (e != head) {
      action.accept(new AbstractMap.SimpleEntry<>(e.key.toString(), e.value.toString()));
      e = e.after;
    }
  }

  @Override
  public List<Map.Entry<String, String>> entries() {
    List<Map.Entry<String, String>> all =
        new LinkedList<>();

    VertxHttpHeaders.MapEntry e = head.after;
    while (e != head) {
      all.add(new AbstractMap.SimpleEntry<>(e.key.toString(), e.value.toString()));
      e = e.after;
    }
    return all;
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return entries().iterator();
  }

  @Override
  public boolean contains(String name) {
    return contains((CharSequence) name);
  }

  @Override
  public boolean isEmpty() {
    return head == head.after;
  }

  @Override
  public Set<String> names() {

    Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    VertxHttpHeaders.MapEntry e = head.after;
    while (e != head) {
      names.add(e.getKey().toString());
      e = e.after;
    }
    return names;
  }

  @Override
  public String get(CharSequence name) {
    Objects.requireNonNull(name, "name");
    CharSequence ret = get0(name);
    return ret != null ? ret.toString() : null;
  }

  @Override
  public List<String> getAll(CharSequence name) {
    return getAll(name.toString());
  }

  @Override
  public boolean contains(CharSequence name) {
    return get0(name) != null;
  }

  @Override
  public VertxHttpHeaders add(CharSequence name, CharSequence value) {
    return add(name.toString(), value.toString());
  }

  @Override
  public VertxHttpHeaders add(CharSequence name, Iterable values) {
    String n = name.toString();
    for (Object seq: values) {
      add(n, seq.toString());
    }
    return this;
  }

  @Override
  public MultiMap set(CharSequence name, CharSequence value) {
    return set(name.toString(), value.toString());
  }

  @Override
  public VertxHttpHeaders set(CharSequence name, Iterable values) {
    remove(name);
    String n = name.toString();
    for (Object seq: values) {
      add(n, seq.toString());
    }
    return this;
  }

  @Override
  public VertxHttpHeaders remove(CharSequence name) {
    return remove(name.toString());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry: this) {
      sb.append(entry).append('\n');
    }
    return sb.toString();
  }

  @Override
  public Integer getInt(CharSequence name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getInt(CharSequence name, int defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Short getShort(CharSequence name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public short getShort(CharSequence name, short defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long getTimeMillis(CharSequence name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getTimeMillis(CharSequence name, long defaultValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Map.Entry<CharSequence, CharSequence>> iteratorCharSequence() {
    return new Iterator<Map.Entry<CharSequence, CharSequence>>() {
      VertxHttpHeaders.MapEntry current = head.after;
      @Override
      public boolean hasNext() {
        return current != head;
      }
      @Override
      public Map.Entry<CharSequence, CharSequence> next() {
        Map.Entry<CharSequence, CharSequence> next = current;
        current = current.after;
        return next;
      }
    };
  }

  @Override
  public HttpHeaders add(String name, Object value) {
    return add((CharSequence) name, (CharSequence) value);
  }

  @Override
  public HttpHeaders addInt(CharSequence name, int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpHeaders addShort(CharSequence name, short value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpHeaders set(String name, Object value) {
    return set0(name, (CharSequence) value);
  }

  @Override
  public HttpHeaders setInt(CharSequence name, int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpHeaders setShort(CharSequence name, short value) {
    throw new UnsupportedOperationException();
  }

  private static final class MapEntry implements Map.Entry<CharSequence, CharSequence> {
    final int hash;
    final CharSequence key;
    CharSequence value;
    VertxHttpHeaders.MapEntry next;
    VertxHttpHeaders.MapEntry before, after;

    MapEntry(int hash, CharSequence key, CharSequence value) {
      this.hash = hash;
      this.key = key;
      this.value = value;
    }

    void remove() {
      before.after = after;
      after.before = before;
    }

    void addBefore(VertxHttpHeaders.MapEntry e) {
      after  = e;
      before = e.before;
      before.after = this;
      after.before = this;
    }

    @Override
    public CharSequence getKey() {
      return key;
    }

    @Override
    public CharSequence getValue() {
      return value;
    }

    @Override
    public CharSequence setValue(CharSequence value) {
      Objects.requireNonNull(value, "value");
      CharSequence oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public String toString() {
      return getKey() + ": " + getValue();
    }
  }
}