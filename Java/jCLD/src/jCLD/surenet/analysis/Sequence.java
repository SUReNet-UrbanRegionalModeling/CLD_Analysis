package jCLD.surenet.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jCLD.surenet.utils.Utilities;

/**
 * A Sequence is a collection of links.
 * 
 * The first link has a 'source' that is the start of the
 * Sequence. The 'influence' of this link is the connection
 * to the next Concept in the Sequence. The last link
 * will have a null influence if he Sequence is open;
 * if the last link has a non-null influence, it will be
 * to one of the preceding elements.
 * 
 * A Sequence is 'Closed' if the influence of the last link
 * has a target equal to any Concept earlier in the Sequence.
 * Once a Sequence is 'Closed', new links cannot be added.
 * 
 * A Sequence is a 'Loop' if the influence of the last link
 * has a target equal to the source of the first link 
 * 
 */
public class Sequence{
	
	/**
	 * Specifies the possible sequence types:
	 * 
	 *  - Open              = A sequence starting at one node, ending at another, and never visiting the same node twice
	 *  - Closed            = A sequence in which the last link is to one of the nodes previously in the sequence
	 *  - Reinforcing Loop  = A sequence in which the last link is to the first node and there is an even number (including zero) of negative influence links
	 *  - Balancing Loop    = A sequence in which the last link is to the first node and there is an off number of negative influence links
	 */
	public static enum SequenceType {
		OPEN,
		CLOSED,
		REINFORCING_LOOP,
		BALANCING_LOOP
	}
	
	
	private static int idCounter = 0;
	
	LinkedList<Link>   links = new LinkedList<Link>();
	Vector<Integer>    values = null;
	
	private Vector<Integer> l1 = new Vector<Integer>();
	private Vector<Integer> l2 = new Vector<Integer>();
	
	private Map<Long, Double> distances = new HashMap<Long, Double>();

	boolean isLoop   = false;
	boolean isClosed = false;
	
	int     countOfNegativeInfluenceLinks = 0;
	int     id;
	
	String representation = null;
	String shortRep = null;
	
	/**
	 * Creates an empty sequence
	 */
	public Sequence() {
		init();
		id = idCounter++;	
	}
	
	/**
	 * Creates a sequence with a single link
	 * @param l
	 */
	public Sequence(Link l) {
		addLink(l);
		init(); // Redundant, but good practice
		id = idCounter++;	
	}

	/**
	 * Copy constructor
	 * @param toCopy
	 */
	public Sequence(Sequence toCopy) {
		for(Link l: toCopy.links) {
			Link newLink = new Link(l);
			links.add(newLink); // Deep copy
		}
		init();
		id = idCounter++;
	}
	
	/**
	 * Appends a link to the end of this sequence.
	 * A closed Sequence (either closed or a loop)
	 * cannot have more links appended.
	 * @param l
	 * @return
	 */
	public boolean addLink(Link l) {
		if(isClosed) return false;
		links.add(l);
		init();
		return true;
	}
	
	/**
	 * Determines the count of negative influence links
	 */
	private void setCountOfNegativeInfluenceLinks() {
		countOfNegativeInfluenceLinks = 0;
		for(Link l: links) if(l.influence == Influence.DECREASES) countOfNegativeInfluenceLinks++;
	}
	
	/**
	 * Sets the flags indicating whether this sequence is
	 * closed, and if so is it a loop
	 */
	private void detectClosedLoop() {
		int idx = (links.size() == 0) ? -1 : indexOfSourceWithConcept(links.peekLast().target); 
		isClosed = (idx != -1);
		isLoop   = (idx ==  0);		
	}
	
	/**
	 * Sets the count of negative influence links,
	 * detects whether the loop is closed or not,
	 * and re-sets the list of values to null
	 * (to be re-initialized if needed).
	 */
	private void init() {
		setCountOfNegativeInfluenceLinks();
		detectClosedLoop();
		values = null;
		shortRep = createShortRep();
	}
	
	/**
	 * Returns true if any of the links in this sequence
	 * have the specified concept as their source, false otherwise
	 * @param c
	 * @return
	 */
	public boolean hasSource(Concept c) {
		return (indexOfSourceWithConcept(c) != -1);
	}
	
	/**
	 * Returns the index of the link with the source
	 * as the specified concept; returns -1 if the concept
	 * is not present as a source.
	 * @param c
	 * @return
	 */
	private int indexOfSourceWithConcept(Concept c) {
		int i = 0;
		for(Link l: links) {
			if(l.source.compareTo(c) == 0) return i;
			else i++;
		}
		return -1;
	}
	
	/**
	 * Remove the current first link and place it in the last
	 * position.
	 */
	public void rotate() {
		if(!isLoop) return; // Rotation only applies to loops
		if(links.size() > 0) links.add(links.remove(0));
	}
	
	/**
	 * Perform rotations until the specified concept is in the first
	 * position. If the specified concept is not a source in this
	 * sequence, the sequence is unchanged.
	 * @param c
	 */
	public void rotate(Concept c) {
		if(!isLoop)                           return; // Rotation only applies to loops
		if(indexOfSourceWithConcept(c) == -1) return; // Cannot rotate to X if X is not present
		if(links.size() == 0)                 return; // Cannot do next step if size is zero
		while(links.get(0).source.compareTo(c) != 0) rotate();
		// Do not need to call init- Does not change any of the other state variables
	}

	/**
	 * If this is a loop, rotates it so that the lowest-ID
	 * concept is in the first position
	 */
	public void rotateToStandard() {
		if(!isLoop) return;           // Rotation only applies to loops
		if(links.size() == 0) return; // Meaningless
		Concept first = links.get(0).source;
		for(Link l: links) if(l.source.compareTo(first) < 0) first = l.source;
		rotate(first);
	}
	
	/**
	 * Returns the number of links in this sequence.
	 * If the sequence is open (not closed), the number
	 * of concepts will be equal to the number of links -1
	 * @return
	 */
    public int getSize() {
    	return links.size();
    }
    
    /**
     * Removes the head from this sequence.
     * If the sequence is a loop, the target of the
     * tail is re-set to point to the new head.
     * The influence is also re-set to reflect the
     * way the original tail influenced the new head.
     * If the original influence through the two links
     * was positive - positive or negative - negative, 
     * the new link is positive; otherwise it is negative.
     */
    public void trimHead() {
    	if(links.size() == 0) return;
    	Influence origInf = links.peekFirst().influence;
    	links.remove(0);
    	if(links.size() > 0) { // If there are any left
    		if(isLoop) {
    			links.peekLast().target = links.get(0).source; // Connect tail to head
    			// Also need to set the last influence.
    			links.peekLast().influence = links.peekLast().influence == origInf ? Influence.INCREASES : Influence.DECREASES;
    		}
    	}
    	init();
    }
    
    /**
     * Removes the last link from this sequence.
     * If the sequence was closed, the target of the
     * new trailing link is re-set to the previous
     * old target. The influence is also re-set to reflect
     * the way that the previous penultimate concept impacted
     * the target of the old last link.
     * If the original influence through the two links
     * was positive - positive or negative - negative, 
     * the new link is positive; otherwise it is negative.
     */
    public void trimTail() {
    	if(links.size() == 0) return;
    	if(!isClosed) links.remove(links.size() - 1); // This is simple if not closed...
    	else {
    		Concept oldTarget = links.peekLast().target;
    		Influence oldInfluence = links.peekLast().influence;
    		links.remove(links.size() - 1); // Take the last element off
    		if(links.size() > 0) { // If there are any left
    			links.peekLast().target = oldTarget; // Point the last element to the old target
    			links.peekLast().influence = links.peekLast().influence == oldInfluence ? Influence.INCREASES : Influence.DECREASES;
	    	}
    	}
    	init();
    }

    /**
     * Gets the concept at the head of this Sequence
     * @return
     */
    public Concept head() {
    	return links.size() > 0 ? links.get(0).source : null;
    }

    /**
     * Gets the concept that is the source of the last link in this sequence
     * @return
     */
    public Concept lastSource() {
    	return links.size() > 0 ? links.peekLast().source : null;
    }
    
    /**
     * Gets the concept that is the target of the last link in this
     * sequence; if this is a loop, this will be the head; if this is 
     * a closed sequence (but not a loop), it will be a concept that
     * is found somewhere else in the sequence
     * @return
     */
    public Concept tail() {
    	return links.size() > 0 ? links.peekLast().target : null;
    }

    // These elements are shortcuts that allow for faster
    // processing
    
    /**
     * Gets the list of concept IDs of SOURCES;
     * if this is not a loop, the last link target will
     * be omitted.
     * 
     * This value is memo-ized as it is used repeatedly;
     * changes to the sequence should null the memoized
     * copy out and allow it to be regenerated when needed.
     * @return
     */
    private Vector<Integer> getListOfValues() {
    	if(values != null) return values;
    	Vector<Integer> ret = new Vector<Integer>();
    	for(Link L: links) ret.add(L.source.id);
    	values = ret;
    	return ret;
    }
    
    
    
    
    /**
     * Calculates a distance value between this sequence and another.
     * Assumes both are loops.
     * @param other
     * @param reference
     * @return
     */
    public double distance(Sequence other, Concept reference) {
    	return distance(other, reference, true);
    }

    private static int TIME_LIMIT  = 200;
    private static int COUNT_LIMIT = 200;
    
    /**
     * Calculates a distance value between this sequence and another.
     * Assumes both are loops.
     * @param other
     * @param reference
     * @param doChecksFlag - If true, gives verbose output
     * @return
     */
    public float distance(Sequence other, Concept reference, boolean doChecksFlag) {
    	long t = System.currentTimeMillis();
		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) System.out.println("FAILED TIME AT CHECKPOINT 1");

    	int marker = 1;
    	boolean gc = false;
    	boolean doChecks = false; // Can set to flag
    	if(doChecks) Utilities.doGC(marker++, gc);

    	if(reference == null) return Float.POSITIVE_INFINITY;
    	
    	if(doChecks) Utilities.doGC(marker++, gc);

    	if(doChecks) {
	    	if(    isLoop                                     == false || 
		    	   other.isLoop                               == false ||
		    	   links.size()                               ==  0    ||
		    	   other.links.size()                         ==  0    ||
		    	   indexOfSourceWithConcept(reference)        == -1    ||
		    	   other.indexOfSourceWithConcept (reference) == -1) {
	    	  System.out.println("Not valid- aborting... " + System.lineSeparator());
	    	  return Float.POSITIVE_INFINITY; // Can only compare two valid loops
	    	}
    	}
		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) System.out.println("FAILED TIME AT CHECKPOINT 3");

    	if(doChecks) Utilities.doGC(marker++, gc);

    	l1.clear();
    	l1.addAll(getListOfValues());
    	l2.clear();
    	l2.addAll(other.getListOfValues());
    	
    	if(doChecks) Utilities.doGC(marker++, gc);

    	Integer ref = reference.id;
    	int count = 0;
    	while(l1.firstElement() != ref) {
    		l1.add(l1.remove(0));
    		if(count++ > l1.size()) {
    			System.out.println("ERROR! L1");
    			break;
    		}
    	}
    	count = 0;
    	while(l2.firstElement() != ref) {
    		l2.add(l2.remove(0));
    		if(count++ > l2.size()) {
    			System.out.println("ERROR! L2");
    			break;
    		}
    		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) {
    			System.out.println("FAILED TIME AT CHECKPOINT 4 L1 size = " + l1.size() + " L2 size = " + l2.size() + " count = " + count);
//    			System.gc();
    			System.out.println("MILLIS GC: " + (System.currentTimeMillis() - t));		    			
    		}

    	}
    	
    	int size      = l1.size() + l2.size() - 2;
    	int deletions = 0;
    	
    	if(doChecks) Utilities.doGC(marker++, gc);

    	count = 0;
    	int l1Size = l1.size();
    	int l2Size = l2.size();
    	while((l1.size() != 0) && (l2.size() != 0)) {
    		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) {
    			System.out.println("FAILED TIME AT CHECKPOINT 5 L1 size = " + l1.size() + " L2 size = " + l2.size());
//    			System.gc();
    			System.out.println("MILLIS GC: " + (System.currentTimeMillis() - t));		    			
    		}

    		Vector<Integer> longer  = l1;
    		Vector<Integer> shorter = l2;
    		if(l1.size() < l2.size()) {
    			longer  = l2;
    			shorter = l1;
    		}
    		int sizeMin = shorter.size();
        	
    		while(sizeMin > 0 && (l1.firstElement() == l2.firstElement())) {
//    			if(count++ > COUNT_LIMIT) System.out.println("FAILED COUNT LIMIT AT CHECKPOINT 6");
        		longer.remove(0);
        		shorter.remove(0);
        		sizeMin--;
        	}

    		
        	// Can do the same with the tail
    		while((sizeMin > 0) && (longer.lastElement() == shorter.lastElement())){
//    			if(count++ > COUNT_LIMIT) System.out.println("FAILED COUNT LIMIT AT CHECKPOINT 7");
        		longer.removeElementAt(longer.size() - 1);;
        		shorter.removeElementAt(shorter.size() - 1);
        		sizeMin--;
        	}
    		
    		Integer c = longer.lastElement();
    		count = 0;
    		if(shorter.contains(c)) {
				shorter.remove(c);
            	deletions++;
            	sizeMin--;
			}
			else {
   				longer.removeElementAt(longer.size() - 1);
				deletions++;
			}
//			if(count++ > COUNT_LIMIT) System.out.println("FAILED COUNT LIMIT AT CHECKPOINT 8");
//			if(l1.size() == l1Size && l2.size() == l2Size) System.out.println("FAILED SIZE LIMIT AT CHECKPOINT 9");
			l1Size = l1.size();
			l2Size = l2.size();

    	}
    	if(doChecks) Utilities.doGC(marker++, gc);

    	deletions += l1.size() + l2.size(); // Anything left must be deleted
    	Float ret = new Float((float)deletions/(float)size);
    	return ret;
    }
        
    
    /**
     * A simple way to determine if two sequences are identical.
     * Note only compares their string representations.
     * @param other
     * @return
     */
    public boolean equals(Sequence other) {
    	if(getSize() != other.getSize()) return false;
    	return (this.toString().compareTo(other.toString()) == 0);
    }

    /**
     * Gets the type of sequence (open, closed, balancing loop
     * or reinforcing loop)
     * @return
     */
    public SequenceType getType() {
    	if     (!isClosed)                                return SequenceType.OPEN;
    	else if(!isLoop)                                  return SequenceType.CLOSED;
    	else if((countOfNegativeInfluenceLinks % 2) == 1) return SequenceType.BALANCING_LOOP;
    	else                                              return SequenceType.REINFORCING_LOOP;
    }
    
    /**
     * Gets a String representation
     * @return
     */
    public String toString(){
    	return shortRep;
    }
    
    private String createRepresentation(){
    	String ret = isLoop ? "LOOP: " : isClosed ? "CLOSED: " : "SEQUENCE: ";
    	if(links.size() == 0) ret += "<EMPTY>";
    	else {
    		ret += links.get(0).source.getName();
    		for(int i = 0; i < links.size(); i++) {
    			Link l = links.get(i);
    			ret += (l.influence == Influence.INCREASES ? " + " : " - ") + 
    					((isClosed == true) && (i == (links.size() - 1)) ? "{" + l.target.getName() + "}" : l.target.getName());
    		}
    	}
//    	ret.intern();
    	return ret;
    }
    
    private String createShortRep(){
    	String ret = isLoop ? "LOOP: " : isClosed ? "CLOSED: " : "SEQUENCE: ";
    	if(links.size() == 0) ret += "<EMPTY>";
    	else {
    		ret += links.get(0).source.id;
    		for(int i = 0; i < links.size(); i++) {
    			Link l = links.get(i);
    			ret += (l.influence == Influence.INCREASES ? "+" :  "-") + 
    					((isClosed == true) && (i == (links.size() - 1)) ? "{" + l.target.id + "}" : l.target.id);
    		}
    	}
    	return ret;
    }
    
    
    
    
    
    /**
     * Gets a String ID
     * @return
     */
    public String getID() {
    	return "SEQ_" + id;
    }
    
    /**
     * Gets the set of all concepts in this sequence
     * @return
     */
    public Set<Concept> getAllConcepts(){
    	Set<Concept> ret = new HashSet<Concept>();
    	for(Link l: links) ret.add(l.source);
    	ret.add(links.peekLast().target);
    	return ret;
    }
    
    /**
     * Determines if this sequence contains any link
     * from source to target (influence is NOT considered)
     * @param source
     * @param target
     * @return
     */
    public boolean containsLink(Concept source, Concept target) {
    	for(Link l: links) if((l.source.compareTo(source) == 0) && (l.target.compareTo(target) == 0)) return true;
    	return false;
    }
    
}
