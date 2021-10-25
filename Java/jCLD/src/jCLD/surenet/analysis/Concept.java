package jCLD.surenet.analysis;

/**
 * The Concept is the fundamental element of the
 * Causal Loop Diagram- it represents a single 
 * 'sticky note' from a whiteboard session, which
 * is a single conceptual element that can increase
 * or decrease and can impact other elements.
 * 
 */
public class Concept implements NamedPolarityItem, Comparable<NamedPolarityItem>{

	private static int idCounter;           // Counter used to set unique IDs
	
	private String   name;                   // Names should be unique; this is enforced by the Concept Factory
	public  Integer  id;                     // Each Concept (and its opposite polarity pair) gets a unique ID number
	
	private Polarity polarity;               // Every concept has a polarity; only positive polarity can be created; the negative pair is created automatically
	private String   representation;         // A string representing this concept; will be the name, or "!name" if polarity has been reversed
	
	/**
	 * Creates a new instance with Positive Polarity
	 * @param n
	 */
	protected Concept(String n) {
		name           = n;
		representation = name;
		polarity       = Polarity.POSITIVE;
		id             = idCounter++;
	}
	

	@Override
	public Polarity getPolarity(){
		return polarity;
	}

	@Override
	public String getName(){
		return name;
	}
	
	@Override
	public int getId() {
		return id;
	}

	// For comparisons
	@Override
	public int compareTo(NamedPolarityItem other){
		return other.getId() - id;
	}
	
	/**
	 * Gets a string representation; will be the name if the polarity
	 * is positive, and !name if it is negative.
	 * @return the current representation of the Concept
	 */
	public String getRepresentation() {
		return representation;
	}

	/**
	 * Changes the polarity from positive to negative
	 * @return the new value of the polarity
	 */
	public Polarity reversePolarity() {
		if(polarity == Polarity.POSITIVE) {
			polarity = Polarity.NEGATIVE;
			representation = "!" + name;
		}
		else {
			polarity = Polarity.POSITIVE;
			representation = name;
		}
		return polarity;
	}
	
}
