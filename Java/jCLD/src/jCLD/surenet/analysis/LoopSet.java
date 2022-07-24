package jCLD.surenet.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import jCLD.surenet.tests.LoadNetwork.SeqScorePair;
import jCLD.surenet.utils.HalfFloatMatrix;
import jCLD.surenet.utils.Utilities;

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
//	float[][] distances = null;
	HalfFloatMatrix distances = null;
	long halfMatrixHits = 0;
	
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
	
	public void finalize() {
		int id = 0;
		for(Sequence l: loops) l.id = id++;
		distances = new HalfFloatMatrix(id, -1f);
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
	
	public double getDistance(Sequence a, Sequence b) {
		float d = distances.get(a.id,b.id);
		if(d == -1f) {
			d = a.distance(b, false);
			distances.set(a.id,b.id, d);
		}
		else halfMatrixHits++;
		return d;
	}
	
	public float getStoredDistance(Sequence a, Sequence b) {
		float d = distances.get(a.id,b.id);
		return (d == -1)  ? Float.POSITIVE_INFINITY : d;
	}
	

	
	private Set<Concept> getAllConcepts(){
		Set<Concept> concepts = new HashSet<Concept>();
	    for(Sequence s: loops) {
	    	for(Concept c: s.getAllConcepts()) {
	    		if(!concepts.contains(c)) concepts.add(c); // Isn't this automatic- it's a set?
	    	}
	    }
	    return concepts;
	}
	
	public Map<Concept, Double> getConceptsAndScores(boolean verbose){
		Map<Concept, Double> ret = new HashMap<Concept, Double>();
		
		Vector<Sequence> ls       = loopsSortedBySize();	        
		Set<Concept>     concepts = getAllConcepts();
		    
	    if(verbose) System.out.println("Entering scoring...");
	    
	    
	    
	   // writeForRBulkProcessing("/Users/murphy/work/SUReNet/R_Tests/BulkProcess");
	    
	    
	    
		    
	    // Loop through all the concepts and get relevance scores
	    int conceptCount = 0;
	    for(Concept c: concepts) {
	    	conceptCount++;
	    	if(verbose) System.out.println("Scoring concept: " + c.getName() + " (" + conceptCount + "/" + concepts.size() + ")");
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
	    	File continueFlagFile = new File("/Users/murphy/work/SUReNet/continue.txt");
	    	Date dt = new Date();
	    	while(sourceLoops.size() > 0) {	    	    		
	    		System.out.print(dt + " Looping through sourceloops, size = " + sourceLoops.size());	    		
	    		Utilities.waitAndSee(continueFlagFile, 120);
	    		halfMatrixHits = 0;
	    		for(SeqScorePair source: sourceLoops) {
	    			double d = getDistance(source.seq, lastAdded.seq);
	    			if(d < source.score) source.score = d;
	    		}
	    		System.out.print(" hits = " + halfMatrixHits + "(" + (int)((((double)halfMatrixHits)/sourceLoops.size() * 100d)) + "%)");
	    			    		
	    		int indexOfMin = 0;
	    		double min = Float.POSITIVE_INFINITY;
	    		int indx = 0;
	    		for(SeqScorePair source: sourceLoops) {
	    			if(source.score < min) {
	    				min = source.score;
	    				indexOfMin = indx; 
	    			}
	    			indx++;
	    		}
	    		lastAdded = sourceLoops.remove(indexOfMin);
	    		finalScore += lastAdded.seq.getSize() * lastAdded.score;
    			scoredLoops.add(lastAdded);
    			System.out.println(" time = " + ((double)((dt.getTime() - ((dt = (new Date())).getTime()))/(-1000d))));

	    	}
	    	if(verbose) System.out.println("FINALSCORE," + c.getName().replaceAll(" ", "_") + "," + numberOfLoops + "," + finalScore);
	    	ret.put(c, finalScore);
	    }
		return ret;
	}
	
	
	
	
	
	
	
//	private void writeForRBulkProcessing(String filename) {
//	  String header = "seq1ID,seq2ID,seq1,seq2,minimumVal" + System.lineSeparator();
//	  System.out.println("Starting write for bulk file...");
//	  try {
//        FileWriter writer = new FileWriter(filename + ".csv");
//        writer.write(header);
//        
//        
//        Vector<Sequence> ls       = loopsSortedBySize();	        
//        System.out.println("Looping; total possible = " + (ls.size() * (ls.size() - 1) / 2));
//        int count = 1;
//        int countWritten = 0;
//        int tenMillions  = 0;
//		for(int i = 0; i < ls.size() - 1; i++) {
//			Sequence s1 = ls.elementAt(i);
//			for(int j = 1; j < ls.size(); j++) {
//				Sequence s2 = ls.elementAt(j);
//				int numNotInCommon = s1.numberOfElementsNotFoundInAnotherSequence(s2) + s2.numberOfElementsNotFoundInAnotherSequence(s1);
//				if(numNotInCommon != s1.getSize() + s2.getSize()) {
//					if(!distances.check(s1.id,  s2.id, -2f)){
//						// Write to the file
//						writer.write(s1.id + "," + 
//						             s2.id + "," + 
//								     Utilities.writeIntArray(s1.getSequenceAsInts(), "|") + "," + 
//						             Utilities.writeIntArray(s2.getSequenceAsInts(), "|") + "," +
//								     ((numNotInCommon)) +
//								     System.lineSeparator());
//						countWritten++;
//					}
//				}
//
//				if(count % 100000 == 0) System.out.print(".");
//				if(count % 1000000 == 0) System.out.print(" ");
//				if(count % 1000000 == 0) {
//					System.out.println(tenMillions++);
//					writer.close();
//					writer = new FileWriter(filename + "_" + tenMillions + ".csv");
//			        writer.write(header);
//				}
//				count++;
//			}
//		}
//       
//        writer.close();
//        System.out.println();
//        System.out.println("Successfully wrote " + countWritten + " lines to file.");
//      } catch (IOException e) {
//        System.out.println("An error occurred.");
//        e.printStackTrace();
//      }
//	  System.out.println("Done with write for bulk file.");
//		
//		
//		
//	}
//	
//	private void readFromRBulkProcessing(String filename) {
//		
//	}
}
