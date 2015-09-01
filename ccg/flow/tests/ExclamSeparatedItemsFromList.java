package ccg.flow.tests;

import java.util.Arrays;

import ccg.flow.tests.IsWord;

public class ExclamSeparatedItemsFromList {

    public static boolean runTest(String candidate,String[] availableItems) {
	
	String[] parts = candidate.split("!!");
	for (String str: parts) {
	    if(! Arrays.asList(availableItems).contains(str)){
		return false;
	    }
	}
	return true;
    }
}
