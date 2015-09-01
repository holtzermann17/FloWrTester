package ccg.flow.tests;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/* Adapted from http://stackoverflow.com/questions/2704857/how-to-check-if-a-given-regex-is-valid
 */
public class IsRegex {
    
    // you could also return strings that would say
    // true/false + what was the error
    // OR put the error message in the log.
    
    public static boolean runTest(String candidate) {

        try {
            Pattern.compile(candidate);
        } catch (PatternSyntaxException exception) {
            System.err.println(exception.getDescription());
	    return false;
        }
        System.out.println("Syntax is ok.");
	return true;
    }
}
