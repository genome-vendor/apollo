package apollo.dataadapter.gff3;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

public class AllTests extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public static Test suite() throws Exception
	{
		TestSuite suite = new TestSuite();
		suite.addTest(GFF3AdapterTest.suite());
		suite.addTest(new TestSuite(GFF3EntryTest.class));
		return suite;
	}
	
}
