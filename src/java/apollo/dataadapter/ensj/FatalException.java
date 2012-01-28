package apollo.dataadapter.ensj;

public class FatalException extends RuntimeException{
  
  public FatalException(String message) {
    this(message, null);
  }
  
  public FatalException(String message, Throwable originalException) {
    super(message, originalException);
  }
  
}
