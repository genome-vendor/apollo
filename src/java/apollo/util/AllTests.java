package apollo.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public static Test suite() throws Exception
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new TestSuite(PairTest.class));
		suite.addTest(new TestSuite(OBOParserTest.class));
		suite.addTest(new TestSuite(FileUtilTest.class));
		return suite;
	}
}