package apollo.dataadapter.ensj;

public class NonFatalException extends RuntimeException{
  
  public NonFatalException(String message) {
    this(message, null);
  }
  
  public NonFatalException(String message, Throwable originalException) {
    super(message, originalException);
  }
  
}
