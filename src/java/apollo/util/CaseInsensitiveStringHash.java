// Based on Java 1.1 java.util.Hashtable
// This class has been implemented to stop lots of
// wasted time creating lowercase strings for key
// lookups in the TierProperty Hashtables.

package apollo.util;

import java.util.*;

class HashtableEntry
  implements java.io.Serializable {
  int hash;
  String key;
  Object value;
  HashtableEntry
  next;

  protected Object clone() {
    HashtableEntry
    entry
    = new HashtableEntry
      ();
    entry.hash = hash;
    entry.key = key;
    entry.value = value;
    entry.next = (next != null) ? (HashtableEntry
                                  )next.clone() : null;
    return entry
           ;
  }
}

public class CaseInsensitiveStringHash implements java.io.Serializable {
  private transient HashtableEntry
  table[];

  private transient int count;

  private int threshold;

  private float loadFactor;

  private static final char[] lc = new char[256];
  private static final char [] tmpStr = new char[16];

  static {
    for (char idx=0; idx<256; idx++)
      lc[idx] = Character.toLowerCase(idx);
  }

  private static final int calcHashCode(String str) {
    //        int  hash  = 0;
    //        char llc[] = lc;
    //        int  len   = str.length();
    //
    //        for (int idx= 0; idx<len; idx++)
    //            hash = 31*hash + llc[str.charAt(idx)];
    //
    //        return hash;

    int h = 0;
    int off = 0;
    char llc[] = lc;
    int len = str.length();

    if (len < 16) {
      str.getChars(0,len,tmpStr,0);
      for (int i = len ; i > 0; i--) {
        h = (h * 37) + llc[tmpStr[off++]];
      }
    } else {
      // only sample some characters
      int skip = len / 8;
      for (int i = len ; i > 0; i -= skip, off += skip) {
        h = (h * 39) + llc[str.charAt(off)];
      }
    }

    return h;
  }

  public CaseInsensitiveStringHash(int initialCapacity, float loadFactor) {
    if ((initialCapacity <= 0) || (loadFactor <= 0.0)) {
      throw new IllegalArgumentException();
    }
    this.loadFactor = loadFactor;
    table = new HashtableEntry
            [initialCapacity];
    threshold = (int)(initialCapacity * loadFactor);
  }

  public CaseInsensitiveStringHash(int initialCapacity) {
    this(initialCapacity, 0.75f);
  }

  public CaseInsensitiveStringHash() {
    this(101, 0.75f);
  }

  public int size() {
    return count;
  }

  public boolean isEmpty() {
    return count == 0;
  }

  public synchronized Enumeration keys() {
    return new HashtableEnumerator(table, true);
  }

  public synchronized Enumeration elements() {
    return new HashtableEnumerator(table, false);
  }

  public synchronized boolean contains(Object value) {
    if (value == null) {
      throw new NullPointerException();
    }

    HashtableEntry
    tab[] = table;
    for (int i = tab.length ; i-- > 0 ;) {
      for (HashtableEntry
           e = tab[i] ; e != null ; e = e.next) {
        if (e.value.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  public synchronized boolean containsKey(String key) {
    HashtableEntry
    tab[] = table;
    int hash = calcHashCode(key);
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (HashtableEntry
         e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equalsIgnoreCase(key)) {
        return true;
      }
    }
    return false;
  }

  public synchronized Object get(String key) {
    HashtableEntry
    tab[] = table;
    int hash = calcHashCode(key);
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (HashtableEntry
         e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equalsIgnoreCase(key)) {
        return e.value;
      }
    }
    return null;
  }

  protected void rehash() {
    int oldCapacity = table.length;
    HashtableEntry
    oldTable[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    HashtableEntry
    newTable[] = new HashtableEntry
                 [newCapacity];

    threshold = (int)(newCapacity * loadFactor);
    table = newTable;

    //System.out.println("rehash old=" + oldCapacity + ", new=" + newCapacity + ", thresh=" + threshold + ", count=" + count);

    for (int i = oldCapacity ; i-- > 0 ;) {
      for (HashtableEntry
           old = oldTable[i] ; old != null ; ) {
        HashtableEntry
        e = old;
        old = old.next;

        int index = (e.hash & 0x7FFFFFFF) % newCapacity;
        e.next = newTable[index];
        newTable[index] = e;
      }
    }
  }

  public synchronized Object put(String key, Object value) {
    // Make sure the value is not null
    if (value == null) {
      throw new NullPointerException();
    }

    // Makes sure the key is not already in the hashtable.
    HashtableEntry
    tab[] = table;
    int hash = calcHashCode(key);
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (HashtableEntry
         e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equalsIgnoreCase(key)) {
        Object old = e.value;
        e.value = value;
        return old;
      }
    }

    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();
      return put(key, value);
    }

    // Creates the new entry.
    HashtableEntry
    e = new HashtableEntry
        ();
    e.hash = hash;
    e.key = key;
    e.value = value;
    e.next = tab[index];
    tab[index] = e;
    count++;
    return null;
  }

  public synchronized Object remove(String key) {
    HashtableEntry
    tab[] = table;
    int hash = calcHashCode(key);
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (HashtableEntry
         e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
      if ((e.hash == hash) && e.key.equalsIgnoreCase(key)) {
        if (prev != null) {
          prev.next = e.next;
        } else {
          tab[index] = e.next;
        }
        count--;
        return e.value;
      }
    }
    return null;
  }

  public synchronized void clear() {
    HashtableEntry
    tab[] = table;
    for (int index = tab.length; --index >= 0; )
      tab[index] = null;
    count = 0;
  }

  public synchronized String toString() {
    int max = size() - 1;
    StringBuffer buf = new StringBuffer();
    Enumeration k = keys();
    Enumeration e = elements();
    buf.append("{");

    for (int i = 0; i <= max; i++) {
      String s1 = k.nextElement().toString();
      String s2 = e.nextElement().toString();
      buf.append(s1 + "=" + s2);
      if (i < max) {
        buf.append(", ");
      }
    }
    buf.append("}");
    return buf.toString();
  }

}

/**
 * A hashtable enumerator class.  This class should remain opaque 
 * to the client. It will use the Enumeration interface. 
 */
class HashtableEnumerator implements Enumeration {
  boolean keys;
  int index;
  HashtableEntry
  table[];
  HashtableEntry
  entry
  ;

  HashtableEnumerator(HashtableEntry
                      table[], boolean keys) {
    this.table = table;
    this.keys = keys;
    this.index = table.length;
  }

  public boolean hasMoreElements() {
    if (entry
        != null) {
      return true;
    }
    while (index-- > 0) {
      if ((entry
           = table[index]) != null) {
        return true;
      }
    }
    return false;
  }

  public Object nextElement() {
    if (entry
        == null) {
      while ((index-- > 0) && ((entry
                                = table[index]) == null));
    }
    if (entry
        != null) {
      HashtableEntry
      e = entry
          ;
      entry
      = e.next;
      return keys ? e.key : e.value;
    }
    throw new NoSuchElementException("HashtableEnumerator");
  }
}
