package apollo.util;

import junit.framework.TestCase;

import java.io.File;

public class FileUtilTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetExtensionFile() {
		assertEquals("getExtension()", "txt", FileUtil.getExtension(new File("foo.txt")));
	}

	public void testGetExtensionString() {
		assertEquals("getExtension()", "txt", FileUtil.getExtension("foo.txt"));
	}

}
