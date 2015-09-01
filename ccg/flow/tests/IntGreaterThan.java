package ccg.flow.tests;

public class IntGreaterThan {

    public static boolean runTest(int candidate, int inf) {
	if(candidate > inf){
	    return true;
	}
	return false;
    }
}
