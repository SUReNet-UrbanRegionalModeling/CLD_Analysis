package jCLD.surenet.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jCLD.surenet.tests.LoadNetwork.SeqScorePair;

/**
 * Maintains a collection of loops, ensuring
 * no duplicates. All loops contained
 * are rotated to the standard position;
 * loop stored is a copy of the loop passed.
 * Loop returned is the copy that is stored,
 * or the extant duplicate loop if it already 
 * existed.
 */
public class LoopSet{

	private Set<Sequence> loops = new HashSet<Sequence>();
	
	/**
	 * Add a loop to this loopset.
	 * Compares this loop to all the previously added loops; if
	 * it is a duplicate, it will not be added.
	 * If it is a non-loop sequence it will not be added, and the
	 * return value will be null.
	 * Note that what is actually added is a COPY of the original loop,
	 * rotated into standard position (with the concept with the
	 * lowest ID value in the first position)
	 * @param toAdd
	 * @return null if the sequence to be added is not a loop,
	 * and the newly added sequence if it is added successfully. 
	 */
	public Sequence addLoop(Sequence toAdd) {
		if(!toAdd.isLoop) return null; // Only add loops
		Sequence loop = new Sequence(toAdd);
		loop.rotateToStandard();
		String rep = loop.toString();
		for(Sequence l: loops) if(rep.compareTo(l.toString()) == 0) return l;
		loops.add(loop);
		return loop;
	}
	
	public void report() {
		int[] counts = new int[Sequence.SequenceType.values().length];
		for(Sequence s: loops) counts[s.getType().ordinal()]++;
		for(Sequence.SequenceType s: Sequence.SequenceType.values()) System.out.println(s.name() + " " + counts[s.ordinal()]);
		
		int longest = 0;
		for(Sequence s: loops) longest = Math.max(longest, s.getSize());
		
		longest++;
		int[] countsBySize = new int[longest]; // Auto init to zero
		for(Sequence s: loops) countsBySize[s.getSize()]++;
		for(int i = 0; i < longest; i++) System.out.println("LOOPS OF SIZE: " + i + " = " + countsBySize[i]);
		
	}
	
	public Vector<Sequence> loopsSortedBySize(){
		Vector<Sequence> ret = new Vector<Sequence>();
		for(Sequence loop: loops) {
			int s = loop.getSize(); // Memoize
			for(int i = 0; i <= ret.size(); i++) {
				if((i == ret.size()) || (s >= ret.get(i).getSize())) {
					ret.insertElementAt(loop, i);
					break;
				}
			}
		}
		return ret;
	}
	
	public long loopsContainingLink(Concept source, Concept target) {
		int count = 0;
		for(Sequence s: loops) if(s.containsLink(source, target)) count++;
		return count;
	}
	
	public long loopsContainingConcept(Concept concept) {
		int count = 0;
		for(Sequence s: loops) if(s.hasSource(concept)) count++;
		return count;
	}
	
	public int getSize() {
		return loops.size();
	}
	
	public Map<Concept, Double> getConceptsAndScores(){
		return getConceptsAndScores(false);
	}
	
	
	private static int LOOP_REPORT_PERIOD = 100;
	private static int TIME_LIMIT         = 200;
	
	public Map<Concept, Double> getConceptsAndScores(boolean verbose){
		Map<Concept, Double> ret = new HashMap<Concept, Double>();
		
		Vector<Sequence> ls = loopsSortedBySize();
	        
		Set<Concept> concepts = new HashSet<Concept>();
	    for(Sequence s: ls) for(Concept c: s.getAllConcepts()) concepts.add(c);
		    
	    if(verbose) System.out.println("Entering scoring...");
		    
	    // Loop through all the concepts and get relevance scores
	    for(Concept c: concepts) {
	    	if(verbose) System.out.println("Scoring concept: " + c.getName());
	    	LinkedList<SeqScorePair> sourceLoops = new LinkedList<SeqScorePair>(); // Score is lowest distance to all current Scored Loops
	    	Vector<SeqScorePair>     scoredLoops = new Vector<SeqScorePair>(); // Score is lowest distance to all previously entered Source Loops
		    	
	    	for(Sequence s: ls) if(s.hasSource(c)) sourceLoops.add(new SeqScorePair(s, 1.0)); // Preserves order
	    	int numberOfLoops = sourceLoops.size();
	    	if(numberOfLoops <= 1) {
	    		if(verbose) System.out.println(c.getName() + " is in less than two loops- skipping.");
	    		continue; // Just move along...
	    	}

	    	SeqScorePair lastAdded = sourceLoops.remove();
	    	scoredLoops.add(lastAdded);
	    	double finalScore = lastAdded.seq.getSize();
	    	int mainLoopPasses = 0;
	    	while(sourceLoops.size() > 0) {
	    		long t = System.currentTimeMillis();
	    		boolean doChecks = (sourceLoops.size() % LOOP_REPORT_PERIOD == 0);
	    		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) System.out.println("FAILED MAIN TIME 1");
	    		int innerLoopPasses = 0;
	    		for(SeqScorePair source: sourceLoops) {
	    			if(verbose && doChecks) System.out.println("LOAD TIMING BEFORE DISTANCE" + ((-1 * t) + (t = System.currentTimeMillis())) + " " + Runtime.getRuntime().freeMemory());
	    			double d = source.seq.distance(lastAdded.seq, c, false);
	    			if(verbose && doChecks) System.out.println("LOAD TIMING DISTANCE " + ((-1 * t) + (t = System.currentTimeMillis())) + " " + Runtime.getRuntime().freeMemory());
	    			if(d < source.score) source.score = d;
	    			innerLoopPasses++;
		    		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) System.out.println("FAILED MAIN TIME 2");
	    		}
	    		if(verbose && doChecks) System.out.println("LOAD TIMING A " + ((-1 * t) + (t = System.currentTimeMillis())) + " " + Runtime.getRuntime().freeMemory());
	    			    		
	    		int indexOfMin = 0;
	    		double min = Double.POSITIVE_INFINITY;
	    		int indx = 0;
	    		if(verbose && doChecks) System.out.println("LOAD TIMING B " + ((-1 * t) + (t = System.currentTimeMillis())) + " " + Runtime.getRuntime().freeMemory());
	    		
	    		for(SeqScorePair source: sourceLoops) {
	    			if(source.score < min) {
	    				min = source.score;
	    				indexOfMin = indx; 
	    			}
	    			indx++;
		    		if(((-1 * t) + (t = System.currentTimeMillis())) > TIME_LIMIT) System.out.println("FAILED MAIN TIME 3");

	    		}
	    		if(verbose && doChecks) System.out.println("LOAD TIMING C " + ((-1 * t) + (t = System.currentTimeMillis())) + " " + Runtime.getRuntime().freeMemory());
	    			    		
	    		lastAdded = sourceLoops.remove(indexOfMin);
	    		finalScore += lastAdded.seq.getSize() * lastAdded.score;
    			scoredLoops.add(lastAdded);
    			if(verbose && doChecks) System.out.println("LOAD TIMING D " + ((-1 * t) + (t = System.currentTimeMillis())) + " " + Runtime.getRuntime().freeMemory());
	    		
	    		mainLoopPasses++;
	    		if(doChecks) {
	    			if(verbose)System.out.println("Main Loop Passes: " + mainLoopPasses + " Inner Loop Passes: " + innerLoopPasses);
	    			mainLoopPasses  = 0;
	    		}
	    	}
	    	if(verbose) System.out.println("FINALSCORE," + c.getName().replaceAll(" ", "_") + "," + numberOfLoops + "," + finalScore);
	    	ret.put(c, finalScore);
	    }
		return ret;
	}
}
