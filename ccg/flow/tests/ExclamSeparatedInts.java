package ccg.flow.tests;

import ccg.flow.tests.IntAsString;

public class ExclamSeparatedInts {

    public static boolean runTest(String candidate) {
	
	String[] parts = candidate.split("!!");
	for (String str: parts) {
	    if(!IntAsString.runTest(str)){
		return false;
	    }
	}
	return true;
    }
}
