package ccg.flow.tests;

public class IntAsString {

    public static boolean runTest(String candidate) {

	try {
            Integer.parseInt(candidate);
        } catch (NumberFormatException exception) {
            System.err.println("Can't be parsed as integer: " + candidate);
	    return false;
        }
	System.out.println("Syntax is ok.");
	return true;
    }
}
