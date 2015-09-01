package ccg.flow.tests;

import ccg.flow.tests.IntAsString;

public class IntAsStringOrAll {

    public static boolean runTest(String candidate) {
	if (IntAsString.runTest(candidate) || candidate.equals("all")) {
	    return true;
	    }
	return false;
    }
}
