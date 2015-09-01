package ccg.flow.tests;

import java.util.ArrayList;

public class IntMinimizesTuplesLengths {

    public static int inf = Integer.MAX_VALUE;
    
    public static boolean runTest(int candidate, ArrayList<String[]> tuples) {

	for (String[] tuple : tuples) {
	    if (tuple.length < inf){
		inf = tuple.length;
	    }
	}
	    
	if(candidate < inf){
	    return true;
	}
	return false;
    }
}
