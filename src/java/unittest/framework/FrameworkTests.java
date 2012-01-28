package unittest.framework;

public class FrameworkTests extends TestSuite
{public FrameworkTests()
	{tests = new Test[3];
	tests[0] = new unittest.framework.GoodTest();
	tests[1] = new unittest.framework.FailTest();
	tests[2] = new unittest.framework.AbortTest();};}
