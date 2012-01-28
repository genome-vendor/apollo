/**
 * test framework downloaded from
 * http://www.extremeprogramming.org/example/utframesource.html
 *
 */

package unittest.framework;

import java.awt.*;
import java.applet.*;

public class TestGUI extends Applet 
	{Label scoreLabel;
	Button runTestsButton;
	List listOfTests;
	Test tests [];
        	
     public void init()
		{initializeTests();
		setLayout(new BorderLayout());
        	add("North", scoreLabel());
        	add("Center", listOfTests());
        	add("South", runButton());
        	scoreLabel.setBackground(Color.lightGray);}
		
    	public void initializeTests() 
    		{String testSuiteName = getParameter("TestSuite");
    		tests = testSuiteNamed(testSuiteName).tests;}
	        
	private TestSuite testSuiteNamed(String aClassName)
		{try 
			{return (TestSuite) Class.forName(aClassName).newInstance();}
	    	catch (Exception exception) 
		    {return new TestSuite();}}
	    
	void runTests() 
		{for (int each = 0; each < tests.length; each++)
	    		{runTest(each);
	        	showResults();};}
	        
	void runTest(int anIndex)
		{tests[anIndex].setUp();
	    	tests[anIndex].run();
	    	tests[anIndex].tearDown();}
        
    	private void showResults() 
    		{for (int each = 0; each < tests.length; each++) 
        		{listOfTests.replaceItem(tests[each].result, each);}
        	showScore();}
        
     private void showScore()
     	{int passed = numberPassed();
        	float total = (float) tests.length;
        	int score = (int)(passed / total * 100);
        	scoreLabel.setText(new Integer(score).toString() + "%");
        	showPassFail(score);}
        
    	private int numberPassed ()
    		{int passed = 0;
        	for (int each = 0; each < tests.length; each++) 
        		{if (tests[each].success) 
            		{passed++;};}
        	return passed;}
        
    	private void showPassFail (int aScore)
    		{scoreLabel.setBackground((aScore == 100) ? Color.green : Color.red);}
    
    	private Label scoreLabel()
    		{return scoreLabel = new Label("Not Run", Label.CENTER);}
		
	private List listOfTests ()
		{listOfTests = new List(tests.length, false);
		for (int each = 0; each < tests.length; each++)
			{listOfTests.addItem(tests[each].result);};
		return listOfTests;}
        
    	private Button runButton() 
    		{runTestsButton = new Button("Run Tests");
    		return runTestsButton;}
    		
    	public boolean action(Event anEvent, Object anObject)
    		{if(wasRunTestsPressed(anEvent)) 
    			{runTests();
    			return true;}
    		else 
    			{return false;}}
    		
    	private boolean wasRunTestsPressed(Event anEvent)
    		{return anEvent.target == runTestsButton;};}	
