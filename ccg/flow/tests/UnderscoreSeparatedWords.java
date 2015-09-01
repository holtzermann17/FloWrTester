package ccg.flow.tests;

import ccg.flow.tests.IsWord;

public class UnderscoreSeparatedWords {

    public static boolean runTest(String candidate) {
	
	String[] parts = candidate.split("_");
	for (String str: parts) {
	    if(!IsWord.runTest(str)){
		return false;
	    }
	}
	return true;
    }
}
