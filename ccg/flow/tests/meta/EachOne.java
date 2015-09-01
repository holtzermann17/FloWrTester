package ccg.flow.tests.meta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.lang.Object;

import ccg.flow.tests.*;

/* My idea for a meta-level test that can run any named test in
 * ccg.flow.tests that takes a single string on each element of an
 * input ArrayList, and that will return false if the test fails on
 * any of the elements of the ArrayList.  Perhaps the same function
 * will work for String[]?  It would be useful to generalise
 * further.  But this is slightly beyond my depth at the moment. -joe,
 * July 21, 2015
 */ 

// Warning, this is an untested sample implementation, and may not work properly yet.

public class EachOne {

    // rather than ...
    // throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
    // it would be good to handle all of these here.

    public static boolean runTest(String test, Object candidate) {

	Class<?> c = Class.forName(test);
	Class[] parameterType = new Class[1];
	parameterType[0] = String.class;
        Method m = c.getDeclaredMethod("runTest", parameterType);

	// for now just implement for various string array types
	if (  (candidate instanceof ArrayList<?> && ((ArrayList<?>)candidate).get(0) instanceof String)
            || candidate instanceof String[] ) {

	    for (String str: candidate) {
		Object ret = m.invoke(str);
		if( (boolean) ret == false) {
		    return false;
		}
	    }
	    return true;
	}
    }
}
