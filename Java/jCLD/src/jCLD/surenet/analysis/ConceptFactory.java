package jCLD.surenet.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Creates instances of concepts. Ensures that
 * names are unique- an attempt to create a new
 * concept with an existing name will return
 * the original concept.
 */
public class ConceptFactory{

	private static Map<String, Concept> concepts = new HashMap<String, Concept>();
	
	private static boolean lastAddedWasNewlyCreated = false;
	/**
	 * Returns a new concept with the name,
	 * or an existing one if that name has already
	 * been used to create a concept.
	 * @param name
	 * @return
	 */
	public static Concept getConcept(String name) {
		name.intern();
		Concept orig = concepts.get(name);
		if(orig != null) {
			lastAddedWasNewlyCreated = false;
			return orig;
		}
		lastAddedWasNewlyCreated = true;
		Concept c = new Concept(name);
		concepts.put(name,  c);
		return c;
	}
	
	/**
	 * Read the contents of the flag indicating whether
	 * the last concept added was newly created or if
	 * the name provided was already in use.
	 */
	public static boolean getLastAddedWasNewlyCreated() {
		return lastAddedWasNewlyCreated;
	}
	
	public static void loadConceptSetFromNetworkFile(String filename) {
	  System.out.println("Starting load of concepts from network...");
	  try {
        File myObj = new File(filename);
        Scanner myReader = new Scanner(myObj);
    	
        TreeSet<String> allConceptNames = new TreeSet<String>();
        
        String data = myReader.nextLine(); // Skip the first line
        while (myReader.hasNextLine()) {
          data = myReader.nextLine();  
          String[]  info      = data.split(",");
          allConceptNames.add(info[0]);
          allConceptNames.add(info[1]);
        }
        myReader.close();
        
        for(String conceptName: allConceptNames) {
        	Concept c = getConcept(conceptName);
        	System.out.println(c.getId() + ": " + conceptName);
        }
        
      } catch (FileNotFoundException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }
	    
	}
	
	public static Vector<Concept> getAll(){
		Vector<Concept> ret = new Vector<Concept>();
		ret.addAll(concepts.values());
		return ret;
	}
	
	
}
