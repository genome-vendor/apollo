package apollo.dataadapter;

/**
 * I can be thrown by code being driven by an Apollo dataadapter - I will
 * be caught by the dataadapter's gui, and a dialog will be presented to the user,
 * who will then be allowed to reinput data and try a read again.
**/
public class
      NonFatalDataAdapterException
      extends
  RuntimeException {
  public NonFatalDataAdapterException(String theMessage) {
    super(theMessage);
  }
}//end NonFatalDataAdapterException


