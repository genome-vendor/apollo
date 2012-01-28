package unittest.framework;

public class FailTest extends Test 
	{protected void runTest() 
		{should(false, "this test always fails");};}
