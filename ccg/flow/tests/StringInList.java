package ccg.flow.tests;

public class StringInList {

    public static boolean runTest(String needle,String[] haystack) {
	for (String str: haystack) {
	    if(str.equals(needle)){
		return true;
	    }}
	return false;
    }
}
