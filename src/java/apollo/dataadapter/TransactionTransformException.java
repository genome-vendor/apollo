package apollo.dataadapter;

public class TransactionTransformException extends Exception {
  
  public TransactionTransformException(String m) { super(m); }
  

  // i kinda like requiring an error message
  // public TransactionTransformException() { super(); }
}
