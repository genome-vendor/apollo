/*********************
 * DataAdapterException
 *
 * Thrown by classes that implement DataAdapterI
 * when there is an exception thrown during the
 * data fetch. Since the exception will probably
 * be different depending on the datasource,
 * DataAdapterException wraps the original exception,
 * which can be fetched by calling getParentException()

This desparately needs to be renamed! Its way too confusing to have both an apollo 
DataAdapterException and an org.bdgp DataAdapterException. Rename this 
ApolloDataAdapterException or just ApolloAdapterException

 */

package apollo.dataadapter;

import java.lang.reflect.InvocationTargetException;

public class ApolloAdapterException extends org.bdgp.io.DataAdapterException {
  Exception parentException;

  public ApolloAdapterException(String desc) {
    this(desc, null);
  }

  public ApolloAdapterException(String desc, Exception parentException) {
    super(desc);
    this.parentException = parentException;
  }

  public ApolloAdapterException(Exception parentException) {
    super("Exception from "+parentException.getClass().getName());
    this.parentException = parentException;
  }

  public Exception getParentException() {
    return parentException;
  }

  public static Throwable getRealException(ApolloAdapterException e) {
    Throwable child = e.getParentException();
    if (child instanceof ApolloAdapterException)
      return getRealException((ApolloAdapterException) child);
    else if (child instanceof InvocationTargetException)
      return getRealException((InvocationTargetException) child);
    else
      return child;
  }

  public static Throwable getRealException(InvocationTargetException e) {
    Throwable child = e.getTargetException();
    if (child instanceof ApolloAdapterException)
      return getRealException((ApolloAdapterException) child);
    else if (child instanceof InvocationTargetException)
      return getRealException((InvocationTargetException) child);
    else
      return child;
  }

  /** Append both the data adapter exception and the parent exception */
  public String toString() {
    String s = super.toString();
    if (getParentException() != null) {
      s += "\nParent Exception:\n" + getParentException().toString();
    }
    return s;
  }

}
