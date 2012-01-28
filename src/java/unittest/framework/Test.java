package unittest.framework;

import java.util.*;
import java.io.*;
import java.lang.*;

/*
 * This is the class to extend for each test you need to run.
 * One new class for each test.  JUnit allows multiple tests
 * per test class and is becoming the standard.  Use JUnit instead.
 * setUp() is called before the test is run and can be used to
 * initialize your test.  runTest() is called to actually run
 * your test.  Override it and send the message should(boolean, String)
 * to check if the test has passed or failed.
 * tearDown() is called after your test is run and can be used 
 * to clean up.  
 */
 
public class Test 
	{public boolean success;
    	public String result;
		
	public Test()
		{super();
		this.initialize();}
				
	private void initialize()
		{this.testFailed("not run");}
    
    	public void setUp()
    		{}
    
    	public void setUp(Hashtable prefs_hash)
    		{}
    
    	protected void runTest()throws Exception 
    		{}
    
    	public void tearDown()
    		{}
        
    	protected void should (boolean aTestPassed, String aMessage)
    		{if (!aTestPassed) 
    			{throw new TestFailedException(aMessage);};}
    
    	public void run()
    		{runAndCaptureAborts();}
			
    	private void runAndCaptureAborts() 
    		{try 
    			{runAndCaptureFailures();}
        	catch (Exception exception) 
        		{		
			    System.err.println(exception);
			    exception.printStackTrace();
			    testFailed("Aborted : " + exception.getMessage());};}
        
    	private void runAndCaptureFailures()throws Exception 
    		{try 
    			{runAndAllowExceptions();}
		catch (TestFailedException exception) 
			{testFailed("Failed : " + exception.getMessage());};}
        
    	private void runAndAllowExceptions()throws TestFailedException, Exception 
    		{runTest();
        	testPassed();}
        
    	private void testPassed()
    		{success = true;
        	result = message("Passed");}
        
    	private void testFailed(String aMessage)
    		{success = false;
        	result = message(aMessage);}
            
    	private String message(String aString)
    		{return getClass().getName() + " : " + aString;};}






