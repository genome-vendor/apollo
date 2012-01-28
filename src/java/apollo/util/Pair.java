package apollo.util;

import java.util.Comparator;

/**This is an utility class to give pairs and uses generics
 * 
 * @author elee
 *
 * @param <T> - the type of the first value
 * @param <U> - the type of the second value
 */

public class Pair<T, U>
{
    //instance variables
    private T first;
    private U second;

    /**Constructs a Pair object containing two elements
     * 
     * @param first - first value of the pair
     * @param second - second value of the pair
     */
    public Pair(T first, U second)
    {
        this.first = first;
        this.second = second;
    }

    /**Get the first value
     * 
     * @return first value of the pair
     */
    public T getFirst()
    {
        return first;
    }

    /**Get the second value
     * 
     * @return second value of the pair
     */
    public U getSecond()
    {
        return second;
    }

    /**Overload Object.equals to compare if the two elements are the same
     * 
     * @param other - instance of Pair being compared to
     * @return whether the two objects are the equal
     */
    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pair)) {
          return false;
        }
        Pair p = (Pair)other;
        return getFirst().equals(p.getFirst()) &&
                getSecond().equals(p.getSecond());
    }

    /**Overload Object.hashCode to return the sum of the the values hashCode
     * 
     * @return new hash code for this object
     */
    public int hashCode()
    {
      return getFirst().hashCode() + getSecond().hashCode();
    }
    
    public String toString()
    {
      return "[" + getFirst().toString() + ", " + getSecond().toString() + "]";
    }
}
