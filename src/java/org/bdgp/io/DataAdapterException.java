package org.bdgp.io;

import org.bdgp.util.*;

public class DataAdapterException extends NestedException {

    public DataAdapterException(String message) {
	super(message);
    }

    public DataAdapterException(Throwable ex, String message) {
	super(ex, message);
    }

  public String toString() {
    String s = super.toString();
    if (getSubThrowable() != null) {
      s += "\nNested Exception:\n" + getSubThrowable().toString();
    }
    return s;
  }
}
