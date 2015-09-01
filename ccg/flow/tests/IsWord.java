package ccg.flow.tests;

public class IsWord {

    // Using a simple definition of words: a string with no spaces in them
    // A more restrictive definition would check whether the argument is actually in one of our various dictionaries.
    public static boolean runTest(String candidate) {
	if (candidate.contains(" ")){
	    return false;
        }
	return true;
    }
}
