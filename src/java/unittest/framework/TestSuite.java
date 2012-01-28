package unittest.framework;

/*
 * Extend this class to create sets of tests to be run together.
 * Override the creation method and initialize the tests variable to 
 * contain an array of Test subclasses.  One instance for each
 * test that needs to be run.  This superclass also provides a
 * default empty suite of tests.
 */

public class TestSuite
{public Test tests [];

public TestSuite()
	{tests = new Test[0];};}
