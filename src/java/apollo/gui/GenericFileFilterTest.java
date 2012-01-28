package apollo.gui;

import junit.framework.TestCase;

import java.io.File;

import java.util.Set;

public class GenericFileFilterTest extends TestCase {

	//instance variables
	GenericFileFilter filter;
	
	protected void setUp() throws Exception {
		super.setUp();
		filter = new GenericFileFilter("Test filter");
	}

	public void testAcceptFile() {
		filter.addExtension("txt");
		assertTrue(filter.accept(new File("test.txt")));
	}

	public void testGetDescription() {
		assertEquals("getDescription()", "Test filter", filter.getDescription());
	}

	public void testSetDescription() {
		filter.setDescription("Foo");
		assertEquals("setDescription()", "Foo", filter.getDescription());
	}

	public void testAddExtension() {
		filter.addExtension("doc");
		assertTrue(filter.accept(new File("foo.doc")));
	}

	public void testGetExtensions() {
		filter.addExtension("txt");
		Set<String> exts = filter.getExtensions();
		assertEquals("getExtensions()", 1, exts.size());
		assertEquals("getExtensions()", true, exts.contains("txt"));
	}

}
