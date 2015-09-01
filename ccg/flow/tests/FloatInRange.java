package ccg.flow.tests;

public class FloatInRange {

    public static boolean runTest(float candidate, int inf, int sup) {
	if(candidate >= inf && candidate <= sup){
	    return true;
	}
	return false;
    }
}
