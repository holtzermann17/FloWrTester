package ccg.flow.tests;

public class FloatAsString {

    public static boolean runTest(String candidate) {

	try {
            Float.parseFloat(candidate);
        } catch (NumberFormatException exception) {
            System.err.println("Can't be parsed as float: " + candidate);
	    return false;
        }
	System.out.println("Syntax is ok.");
	return true;
    }
}
