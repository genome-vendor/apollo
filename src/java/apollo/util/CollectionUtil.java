package apollo.util;

import java.util.Collection;

/** This class provides static utility methods for handling Collections.
 * 
 * @author elee
 *
 */

public class CollectionUtil {

  /** Append elements from src to dest.
   * 
   * @param src - Collection containing data to be appended.
   * @param dest - Collection to have data appended to.
   */
  public static <T> void appendElements(Collection<? extends T> src, Collection<? super T> dest)
  {
    copyElements(src, dest, false);
  }
  
  /** Copy elements from src to dest.
   * 
   * @param src - Collection containing data to be copied.
   * @param dest - Collection to have the data copied to.
   * @param clearDestination - whether to clear all contents from destination prior to copying.
   */
  public static <T> void copyElements(Collection<? extends T> src, Collection<? super T> dest,
      boolean clearDestination)
  {
    if (clearDestination) {
      dest.clear();
    }
    for (T t : src) {
      dest.add(t);
    }
  }
  
}
