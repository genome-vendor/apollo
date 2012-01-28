package unittest.framework;

public class AbortTest extends Test 
	{private int number[] = {0,1,2,3};
	
	protected void runTest() 
		{should(number[1] / number[0] == 0, "this test always fails");};}
