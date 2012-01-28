package apollo.util;

import junit.framework.TestCase;

public class PairTest extends TestCase {

	private Pair<String, Integer> p1;
	
	public PairTest(String testMethod)
	{
		super(testMethod);
	}
	
	public void setUp()
	{
		p1 = new Pair<String, Integer>("one", 1);
	}
	
	public void testGetFirst() {
		assertEquals("getFirst()", "one", p1.getFirst());
	}

	public void testGetSecond() {
		assertEquals("getSecond()", new Integer(1), p1.getSecond());
	}

	public void testEquals() {
		Pair<String, Integer> p2 = new Pair<String, Integer>("one", 1);
		Pair<String, Integer> p3 = new Pair<String, Integer>("two", 2);
		Pair<String, Integer> p4 = p1;
		assertTrue("equals", p1.equals(p2));
		assertFalse("!equals", p1.equals(p3));
		assertTrue("equals", p1.equals(p4));
	}

}
