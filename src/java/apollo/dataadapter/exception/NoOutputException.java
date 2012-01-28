package apollo.dataadapter.exception;

public class NoOutputException extends BopException {
  public NoOutputException() {super();}
  public NoOutputException(String msg) {super(msg);}
  public NoOutputException(String msg, Exception e) {super(msg, e);}
}
