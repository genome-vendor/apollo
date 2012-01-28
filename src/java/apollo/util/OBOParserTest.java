package apollo.util;

import junit.framework.TestCase;

import apollo.util.IOUtil;
import apollo.util.Pair;

import org.obo.datamodel.IdentifiedObject;

import java.util.Arrays;

public class OBOParserTest extends TestCase {

	private OBOParser oboParser;
	
	protected void setUp() throws Exception {
		String []oboFiles = { IOUtil.findFile("so.obo"), IOUtil.findFile("apollo.obo") };
		oboParser = new OBOParser(Arrays.asList(oboFiles));
	}

	public void testGetTermById() {
		String id1 = "SO:0000704";
		String id2 = "FAKE:0000000";
		IdentifiedObject io1 = oboParser.getTermById(id1);
		IdentifiedObject io2 = oboParser.getTermById(id2);
		assertNotNull("getTermById() not null", io1);
		assertEquals("getTermById()", id1, io1.getID());
		assertNull("getTermById() null", io2);
	}

	public void testGetTermByName() {
		String name1 = "gene";
		String name2 = "fake";
		Pair<IdentifiedObject, String> io1 = oboParser.getTermByName(name1);
		Pair<IdentifiedObject, String> io2 = oboParser.getTermByName(name2);
		assertNotNull("getTermByName() not null", io1);
		assertEquals("getTermByName()", name1, io1.getFirst().getName());
		assertNull("getTermByName() null", io2);
	}

	public void testHasTermWithId() {
		String id1 = "SO:0000704";
		String id2 = "FAKE:0000000";
		assertTrue("hasTermWithId()", oboParser.hasTermWithId(id1));
		assertFalse("!hasTermWithId()", oboParser.hasTermWithId(id2));
	}

	public void testHasTermWithName() {
		String name1 = "gene";
		String name2 = "fake";
		assertTrue("hasTermWithName()", oboParser.hasTermWithName(name1));
		assertFalse("!hasTermWithName()", oboParser.hasTermWithName(name2));
	}

}
