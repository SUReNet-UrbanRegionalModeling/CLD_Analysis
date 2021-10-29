package jCLD.surenet.analysis;

import java.util.HashMap;
import java.util.Map;

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
	
}
