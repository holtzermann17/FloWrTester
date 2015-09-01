package ccg.flow.tests;

public class IntInRange {

    public static boolean runTest(int candidate, int inf, int sup) {
	if(candidate >= inf && candidate <= sup){
	    return true;
	}
	return false;
    }
}
