package jCLD.surenet.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class Utilities{
	
	public static boolean paused = false;
	
	 // Helper functions
	 public static int[] vectorToIntArray(Vector<Integer> values) {
    	int[] ret = new int[values.size()];
    	for(int i = 0; i < values.size(); i++) ret[i] = values.get(i);
    	return ret;
    }
	    
	 public static void waitAndSee(File continueFlagFile, int seconds) {
		if(paused == false && !continueFlagFile.exists()) {
			System.out.println("- paused - " + ((new Date())));
			paused = true;
		}
		while(!continueFlagFile.exists()) {
			try {
				TimeUnit.SECONDS.sleep(seconds);
			}
			catch(Exception E) {
				
			}
		}
		if(paused) {
			System.out.println("- resumed - " + ((new Date())));
		}
		paused = false;
	 }
	 
    public static int[] rotate(int[] orig) {
    	int[] ret = new int[orig.length];
    	if(orig.length > 0){
	    	ret[ret.length-1] = orig[0];
	    	for(int i = 0; i < orig.length - 1; i++) ret[i] = orig[i+1];
	    }
    	return ret;
    }
    
    public static void rotateInPlace(int[] orig) {
    	int start = orig[0];
    	int i = 0;
    	int end = orig.length - 1;
    	while(i < end) {
    		orig[i] = orig[i+1];
    		i++;
    	}
    	orig[i] = start;
    }
    
    public static String writeIntArray(int[] a) {
    	return writeIntArray(a, " ");	
    }
    
    public static String writeIntArray(int[] a, String separator) {
    	String ret = "";
    	String sep = "";
    	for(int i: a) {
    		ret += sep + i;
    		sep = separator;
    	}
    	return ret;	
    }
    
    // The matrix is static because tearing it down and rebuilding it
    // is memory-intensive; instead, we build a new one whenever we need
    // a larger one, but keep re-using it each time.
    // Note also that we don't need to re-initialize; the algorithm
    // initializes the top row and left column, then fills in the
    // remainder without ever looking ahead. Because it only uses
    // values that have been set correctly, we don't need to clear t
    // out.
    static int[][] matrix = new int[1][1];
    
    /**
     * Calculates the Levenshtein distance between two sequences of integers
     * @param a
     * @param b
     * @param deleteInsertCost
     * @param substituteCost
     * @return
     */
    public static int distLevenshtein(final int[] a, final int[] b, final int deleteInsertCost, final int substituteCost) {
    	final int m = a.length;
    	final int n = b.length;
    	if((m + 1) > matrix.length || (n + 1) > matrix[0].length) {
    		//System.err.println("Expanding matrix");
    		matrix = new int[m + 1][n + 1];
    	}
		for(int i = 0; i <= m; i++) matrix[i][0] = i;
		for(int j = 0; j <= n; j++) matrix[0][j] = j;
    	
    	// Create a matrix like:
    	//
    	//          S  E  Q  U  E  N  C  E
    	//       0  1  2  3  4  5  6  7  8
    	//   T   1
    	//   E   2
    	//   X   3
    	//   T   4
    	//
    	// Values for Uninitialized cells' and cells out of range are irrelevant
    	// Initialization of row values is deferred
    	for(int j = 1, y = 0; j <= n; j++, y++) {
    		for(int i = 1, x = 0; i <= m; i++, x++) {
				matrix[i][j] = Math.min(Math.min(
			         	matrix[x][j] + 1, 
			         	matrix[i][y] + 1),
			        	matrix[x][y] + ((a[x] == b[y]) ? 0 : 1));
    		}
    	}    	
    	return matrix[m][n];
    }

    
    // In this version, we optimize by:
    //
    //    1 Deferring initialization of the zero column
    //          until processing each row
    
    public static int distLevenshteinOptimized(int[] a, int[] b) {
		int m = a.length;
		int n = b.length;
		if((m + 1) > matrix.length || (n + 1) > matrix[0].length) {
			matrix = new int[m + 1][n + 1];
		}
		for(int i = 0; i <= m; i++) matrix[i][0] = i;
		for(int j = 0; j <= n; j++) matrix[0][j] = j;	
		
		for(int j = 1, y = 0; j <= n; j++, y++) {
			for(int i = 1, x = 0; i <= m; i++, x++) {
				matrix[i][j] = Math.min(Math.min(
					         	matrix[x][j] + 1, 
					         	matrix[i][y] + 1),
					        	matrix[x][y] + ((a[x] == b[y]) ? 0 : 1));
			}
		}    	
		return matrix[m][n];
    }
    

    
    public static int distLevenshteinWithRotation(int[] a, int[] b) {
    	int m = a.length;
    	int n = b.length;
    	if((m + 1) > matrix.length || (n + 1) > matrix[0].length) {
    		matrix = new int[m + 1][n + 1];
    	}
    	
    	// Save some time
    	// This takes the arrays and makes them twice as long and repeats
    	// the sequence, allowing 'rotation' just by sliding the start point
    	int[] aDouble = new int[a.length * 2];
    	for(int i = 0; i < a.length; i++) aDouble[i + a.length] = aDouble[i] = a[i];
    	int[] bDouble = new int[b.length * 2];
    	for(int i = 0; i < b.length; i++) bDouble[i + b.length] = bDouble[i] = b[i];
    	    	
		// Determine bail-out condition, the lowest Levenshtein Distance possible
		int min = Math.max(0, Math.abs(a.length - b.length));
		
		// Start with the highest Levenshtein Distance possible
		int lowest = m + n;

		// Initialize the matrix; these never change
		for(int i = 0; i <= m; i++) matrix[i][0] = i;
		for(int j = 0; j <= n; j++) matrix[0][j] = j;

		mainLoop:
	    // These outer loops are for 'rotation', sliding the starting indices for the
		// Arrays of letters
		for(int aStart = 0; aStart < m; aStart++) {
			for(int bStart = 0; bStart < n; bStart++) {
				// This is the standard Levenshtein distance calculation
				innerLoop:
		    	for(int j = 1, y = 0, letterBIndx = bStart; j <= n; j++, y++, letterBIndx++) {
		    		int letterB = bDouble[letterBIndx];
		    		int lowestPossible = j;
		    		for(int i = 1, x = 0, letterAIndx = aStart; i <= m; i++, x++, letterAIndx++) {
						lowestPossible = 
							Math.min(lowestPossible, matrix[i][j] = Math.min(Math.min(
					         	matrix[x][j] + 1, 
					         	matrix[i][y] + 1),
					        	matrix[x][y] + ((aDouble[letterAIndx] == letterB) ? 0 : 1)));
						// Can do a check here: if we are already above the
						// lowest, quit
						// The lowest possible outcome of the rest of the 
						// algorithm is equal to the lowest value in this
						// row
		    		}
		    		if(lowestPossible >= lowest) break innerLoop; 
		    	}    	
		    	lowest = Math.min(matrix[m][n], lowest);
		    	// If you have found one that is equal to the theoretical minimum,
		    	// Stop- no need to look further.
		    	if(lowest == min) break mainLoop;
			}
		}
		return lowest;
		
    }
    
    
    
    
    
    
    // Alternate entry point using strings;
    // Note: Converts to lower case
    public static double distLevenshtein(String a, String b) {
    	char[] aChar = a.toLowerCase().toCharArray();
    	char[] bChar = b.toLowerCase().toCharArray();
    	
    	int[] aInt = new int[aChar.length];
    	for(int i = 0; i < aChar.length; i++) aInt[i] = Character.getNumericValue(aChar[i]);
    	int[] bInt = new int[bChar.length];
    	for(int i = 0; i < bChar.length; i++) bInt[i] = Character.getNumericValue(bChar[i]);
    	
    	return distLevenshtein(aInt, bInt, 1, 1);
    }

    // Calls R and uses its Levenshtein distance calculation
    public static int distLevenshtein_USING_R(int[] a, int[]b) {
    	String[] commandLine = new String[]{"/usr/local/bin/Rscript",
    			                            "/Users/murphy/work/SUReNet/R_Tests/TestLVDist.R", 
    			                            writeIntArray(a).replaceAll(" ", ","),
    			                            writeIntArray(b).replaceAll(" ", ",")};
    	int retVal = -1;
    	try {
	      Process process = null;
	      ProcessBuilder pb = new ProcessBuilder(commandLine);
	      pb.directory(new File("/Users/murphy/work/SUReNet/R_Tests/"));
	      
	      process = pb.start();
	      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); 
	      while(process.isAlive())  TimeUnit.MILLISECONDS.sleep(50);

	      String line = reader.readLine(); // First line
	      String lastLine = line;
	      do {
	    	  lastLine = line;
	    	  //System.out.println(lastLine);
	      }while ((line = reader.readLine()) != null);
	      String toParse = lastLine.substring(lastLine.lastIndexOf(" ") + 1);
	      retVal = Integer.parseInt(toParse);
	    }
	    catch(Exception E) {
	      System.out.println("Exception: " + E.getMessage());
	      System.out.println(E.getLocalizedMessage());
	      E.printStackTrace();
	    }
    	return retVal;
    }
    
    
 // Calls R and uses its Levenshtein distance calculation
    public static int distLevenshtein_USING_R_WithRotation(int[] a, int[]b) {
    	String[] commandLine = new String[]{"/usr/local/bin/Rscript",
    			                            "/Users/murphy/work/SUReNet/R_Tests/TestLVDist_WithRotation.R", 
    			                            writeIntArray(a).replaceAll(" ", ","),
    			                            writeIntArray(b).replaceAll(" ", ",")};
    	//System.out.println(commandLine[0] + " " + commandLine[1] + " " + commandLine[2] + " " + commandLine[3]);
    	int retVal = -1;
    	try {
	      Process process = null;
	      ProcessBuilder pb = new ProcessBuilder(commandLine);
	      pb.directory(new File("/Users/murphy/work/SUReNet/R_Tests/"));
	      
	      
	      process = pb.start();
	      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); 
	      while(process.isAlive()) {
	    	  TimeUnit.MICROSECONDS.sleep(10);
	      }
	      
	      String line = reader.readLine(); // First line
	      String lastLine = line;
	      do {
	    	  lastLine = line;
	    	  //System.out.println(lastLine);
	      }while ((line = reader.readLine()) != null);
	      String toParse = lastLine.substring(lastLine.lastIndexOf(" ") + 1);
	      //System.out.println(toParse);
	      retVal = Integer.parseInt(toParse);
	    }
	    catch(Exception E) {
	      System.out.println("Exception: " + E.getMessage());
	      System.out.println(E.getLocalizedMessage());
	      E.printStackTrace();
	    }
    	return retVal;
    }
    
    
    

    // ALTERNATE IMPLEMENTATION- NOT DEBUGGED
    
//	private static long LCalls       = 0;
//	private static int  currentMin   = 0;
//	private static long HighestIndex = 0;
	

//    public static int distLevenshteinRecursive(int[] a, int aIndex, int[] b, int bIndex, int depth) {
//    	if(depth > currentMin) return a.length + b.length; // Bail if too high already
//    	LCalls++;
//    	if(((aIndex * bIndex) > HighestIndex) || (LCalls % 1000000 == 0)) {
//    		HighestIndex = (aIndex * bIndex);
//    		System.out.println("HighestIndex: " + HighestIndex + " A is " + aIndex + " of " + a.length + " b is " + bIndex + " of " + b.length + " (" + (a.length * b.length) + ") CALLS: " + LCalls);
//    	}
//    	if      (bIndex == b.length) {
//    		return currentMin = Math.min(currentMin, a.length - aIndex);
//    	}
//    	else if (aIndex == a.length) {
//    		return currentMin = Math.min(currentMin, b.length - bIndex);
//    	}
//    	else {
//    		if(depth + 1 > currentMin) return a.length + b.length;
//    		else return a[aIndex] == b[bIndex] ? distLevenshtein(a, aIndex + 1, b, bIndex + 1, depth+1) :
//    	           1 + Math.min(distLevenshtein(a, aIndex + 1, b, bIndex, depth + 1), 
//        		       Math.min(distLevenshtein(a, aIndex, b, bIndex + 1, depth + 1), 
//        					    distLevenshtein(a, aIndex + 1, b, bIndex + 1, depth + 1)));
//    	}
//    }
    
    
    public static int countOfDifferentElements(int[] a, int[] b) {
    	Set<Integer> setA = new TreeSet<Integer>();
    	for(int i: a) setA.add(i);
    	int aPres = setA.size();
    	int aAbs  = 0;
    	for(int i: b) {
    		if(setA.add(i)) aAbs++;
    		else            aPres--;
    	}
    	return aPres + aAbs;
    }
    
    
    
    public static void main(String[] args) {
//      for(int i = 0; i < 1000; i++) {
//    	  int lenA = (int)(Math.floor(Math.random() * 100d) + 1);
//    	  int[] a = new int[lenA];
//    	  for(int j = 0; j < lenA; j++) a[j] = (int)((Math.floor(Math.random() * 20) + 1));
//    	  
//    	  int lenB = (int)(Math.floor(Math.random() * 100d) + 1);
//    	  int[] b = new int[lenB];
//    	  for(int j = 0; j < lenB; j++) b[j] = (int)((Math.floor(Math.random() * 20) + 1));
//
//    	  distLevenshtein(a, b);
//      }

    	int[] a = new int[] {1, 2, 3, 4, 5};
    	int[] b = new int[] {1, 2, 4, 5, 6};
    	System.out.println(countOfDifferentElements(a, b));
    	System.out.println(countOfDifferentElements(b, a));
    	
//      int[] test = new int[]{ 1, 2};
//      rotateInPlace(test);
//      System.out.println(test[0]);
      
    }
    
    
}
