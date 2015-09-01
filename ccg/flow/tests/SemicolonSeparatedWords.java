package ccg.flow.tests;

import ccg.flow.tests.IsWord;

public class SemicolonSeparatedWords {

    public static boolean runTest(String candidate) {
	
	String[] parts = candidate.split(";");
	for (String str: parts) {
	    if(!IsWord.runTest(str)){
		return false;
	    }
	}
	return true;
    }
}
