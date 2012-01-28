package apollo.dataadapter.exception;

import java.io.*;
import java.lang.Exception;

public class BopException extends Exception {
  protected String stackTraceAtCreate = "";
  
  public BopException() {
    super();
  }

  public BopException(String msg) {
    super(msg);
  }

  public BopException(String s, String msg) {
    super(s+"\n"+msg);
  }

  //create exception when just catch a exception
  //usefull to print real stack trace when firt exception occurs
  public BopException(String msg, Exception e) {
    super(msg);
    if (e instanceof BopException) {
      BopException ex = (BopException)e;
      stackTraceAtCreate = ex.getOriginalStackTrace();
      return; //don't set trace again
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintWriter ps = new PrintWriter(bos, true);
    e.printStackTrace(ps);
    stackTraceAtCreate = bos.toString();
  }

  public BopException(String s, String msg, Exception e) {
    super(s+"\n"+msg);
    if (e instanceof BopException) {
      BopException ex = (BopException)e;
      stackTraceAtCreate = ex.getOriginalStackTrace();
      return; //don't set trace again
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintWriter ps = new PrintWriter(bos, true);
    e.printStackTrace(ps);
    stackTraceAtCreate = bos.toString();
  }

  public String getOriginalStackTrace() {
    return stackTraceAtCreate;
  }

  public String getMessageWStackTrace() {
    return this.getMessage()+"\n"+this.getStackTraceString();
  }

  public String getStackTraceString() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintWriter ps = new PrintWriter(bos, true);
    this.printStackTrace(ps);
    //System.out.println("get trace after conversion\n"+bos.toString());
    if (stackTraceAtCreate.length() > 0) {
      String className = "";
      int index = 0;
      index = this.toString().indexOf(":");
      if (index > 0) {
	className = this.toString().substring(0, index);
      }
      return "Orignal StackTrace when creating this exception, "
	+className+"\n"+stackTraceAtCreate;
    }
    return bos.toString();
  }
}

