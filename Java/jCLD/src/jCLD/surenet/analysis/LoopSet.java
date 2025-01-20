package jCLD.surenet.analysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

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
	

	public static class SeqScorePair{
		public Sequence seq;
		public double score;
		
		public SeqScorePair(Sequence sequence, double s) {
			seq   = sequence;
			score = s;
		}
	}
	
	
	public static void moveFromPrecalc(String path, String inputFileName, int startLine, int countLines, String outputFilename, String continueFlagFileName) {
		try {
			File continueFlagFile = new File(path + continueFlagFileName);
			FileWriter writer = new FileWriter(path + outputFilename);
	        Scanner reader = new Scanner(new File(path + inputFileName));
	    	
	        int lineCount = 0;
	        int readLines = 0;
	        while(reader.hasNextLine() && readLines < countLines) {
	        	Utilities.waitAndSee(continueFlagFile, 2);
	        	String line = reader.nextLine();
	        	lineCount++;
	        	if(lineCount > startLine) {
	        		if(line.endsWith("]")) writer.write(line + System.lineSeparator()); // Just copy the pre-calculated
	        		else {
	        			String[] elements = line.split("\\|");
	        			String[] s1 = elements[2].split(",");
	        			int[] seq1 = new int[s1.length];
	        			for(int i = 0; i < s1.length; i++) seq1[i] = Integer.parseInt(s1[i]);
	        			String[] s2 = elements[3].split(",");
	        			int[] seq2 = new int[s2.length];
	        			for(int i = 0; i < s2.length; i++) seq2[i] = Integer.parseInt(s2[i]);
	        			
	        			double dist = Utilities.distLevenshteinWithRotation(seq1, seq2);
	        			writer.write(elements[0] + "|" + elements[1] + "|[" + dist + "]" + System.lineSeparator());
	        		}
	        		readLines++;
	        	}
	        	if(readLines > 0) {
		        	if(lineCount % 1000 == 0) {
		        		System.out.print(".");
		        		if(lineCount % 10000 == 0) {
		        			System.out.println((new Date()).toString() + " " + readLines + " lines processed");
		        		}
		        	}
	        	}
	        }
	        reader.close();
			writer.close();
			System.out.println();
			System.out.println("Done; read " + readLines + " lines");
		} 
		catch (IOException E) {
	        System.out.println("Error: " + E.getMessage());
	        E.printStackTrace();
	    }
		
	}
	
	
	
	
	
	

	private Set<Sequence> loops = new TreeSet<Sequence>();
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
		for(Sequence l: loops) {
			System.out.println(l.id + ": " + l.shortRep);
		}
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
	
	private static String addSuffixAndLCFExtension(String filename, int index) {
		return filename + "_" + ("0000" + index).substring(("0000" + index).length() - 3) + ".lcf";
	}
	
	// Writes a file with all sequences, their IDs,
	// and the version that needs to be compared
	// Omits initial elements if they are identical
	public void writeOnlyComparisonsToFile(String filename, long maxLinesPerFile, boolean pauseBetweenFiles) {
		Vector<Sequence> sequencesInOrder = new Vector<Sequence>();
		for(Sequence loop: loops) {
			loop.getSequenceAsInts(); // This will initialize all of these
			sequencesInOrder.add(loop);
		}
		int size = sequencesInOrder.size();
		int comps = (size * (size -1 )/ 2);
		System.out.println(size + " loops; " + comps  + " possible comparisons.");
		String sep = "";
		int count = 0;
		int precalc = 0;
		int linesInCurrentFile = 0;
		int currentFile = 0;
		
		try {
			String currentFilename = addSuffixAndLCFExtension(filename, currentFile);
			FileWriter writer = new FileWriter(currentFilename);
			for(int i = 0; i < sequencesInOrder.size() - 1; i++) {
				Sequence seq1 = sequencesInOrder.get(i);
				for(int j = i + 1; j < sequencesInOrder.size(); j++) {
					Sequence seq2 = sequencesInOrder.get(j);
					
					StringBuilder s = new StringBuilder();
					s.append(seq1.id + "," + seq1.getSize());
					s.append("|");
					s.append(seq2.id + "," + seq2.getSize());
					s.append("|");
	
					int start = 0;
					int minLength = Math.min(seq1.sequenceAsInts.length, seq2.sequenceAsInts.length);
					while((start < minLength) && (seq1.sequenceAsInts[start] == seq2.sequenceAsInts[start])) start++;
					
					// In this case, the beginning of one of the sequences is the entirety of another,
					// e.g.:
					//     A  B  C  D
					//     A  B  C
					// The Levenshtein distance will be the difference in their lengths
					if(start == minLength) {
						s.append("[" + Math.abs(seq1.sequenceAsInts.length - seq2.sequenceAsInts.length) + "]");
						precalc++;
					}
					else if(seq1.sequenceAsInts.length == start + 1 && seq2.sequenceAsInts.length == start +1) {
						// This is the case where both sequences start the same but have one ending element different
						s.append("[1]");
						precalc++;
					}
					else {
						
						sep = "";
						for(int x = start; x < seq1.sequenceAsInts.length; x++) {
							s.append(sep);
							s.append(seq1.sequenceAsInts[x]);
							sep = ",";
						}
						s.append("|");
						sep = "";
						for(int x = start; x < seq2.sequenceAsInts.length; x++) {
							s.append(sep);
							s.append(seq2.sequenceAsInts[x]);
							sep = ",";
						}
					}
					count++;
					if(count % 10000 == 0) {
						System.out.print(".");
						if(count % 500000 == 0) {
							System.out.println(" " + ((new Date()).toString()) + " Completed " + count + " of " + comps + " comparisons with " + precalc + " precalculated");
						}
					}
					writer.write(s.toString() + System.lineSeparator());
					linesInCurrentFile++;
					if(linesInCurrentFile == maxLinesPerFile) {
						writer.close();
						if(pauseBetweenFiles == true) {
							System.out.println();
							System.out.println("Paused: Move the previous file (" + currentFilename + ") to continue");
							File f = new File(currentFilename);
							while(f.exists()) {
								try{
									TimeUnit.SECONDS.sleep(10);
								}
								catch(Exception E) {}
							}
						}
						else {
							System.out.println();
							System.out.println("File " + currentFilename + " complete");
						}
						currentFile++;
						currentFilename = addSuffixAndLCFExtension(filename, currentFile);
						writer = new FileWriter(currentFilename);
						linesInCurrentFile = 0;
					}
				}
				
			}
		    writer.close();
		} 
		catch (IOException E) {
	        System.out.println("Error: " + E.getMessage());
	        E.printStackTrace();
	    }

	}
	
	
	public void readPrescores(String path, String[] filenames) {
		try {
			for(String filename: filenames) {
				System.out.println("Scanning " + filename);
				Scanner reader = new Scanner(new File(path + filename));
				int lineCount       = 0;
				int skippedLines    = 0;
				int readLines       = 0;
				int countNewValue   = 0;
				int countAlreadySet = 0;
				int countMisMatch   = 0;
				while(reader.hasNextLine()) {
					String line = reader.nextLine();
					lineCount++;
					if((line.endsWith("]"))) {
						readLines++;
						String[] elements = line.split("\\|");
						String[] element1 = elements[0].split(",");
						int id1 = Integer.parseInt(element1[0]);
						int len1 = Integer.parseInt(element1[1]);
						
						String[] element2 = elements[1].split(",");
						int id2 = Integer.parseInt(element2[0]);
						int len2 = Integer.parseInt(element2[1]);
						
						double score = Double.parseDouble(elements[2].substring(1).replace("]","")) / (double)(len1 + len2);
						float prevScore = distances.push(id1,  id2, (float)score);
						if(prevScore == -1f) countNewValue++;
						else {
							if(prevScore == (float)score) countAlreadySet++;
							else {
								System.err.println();
								System.err.println("Mismatch! Seq 1 = " + id1 + " Seq 2 " + id2 + " Previous: " + prevScore + " New: " + (float)score);
								countMisMatch++;
							}
						}						
					}
					else skippedLines++;
					
					if(lineCount % 100000 == 0) {
						System.out.print(".");
						if(lineCount % 1000000 == 0) {
							System.out.println(" processed " + lineCount + " lines");
						}
					}
					
				}				
				reader.close();
				System.out.println();
				System.out.println("Done reading prescores from " + filename + " " + lineCount + " lines, " + readLines + " read, " + skippedLines + " skipped, " + countAlreadySet + " already set, " + countNewValue + " new values, " + countMisMatch + " mismatches");
			}
		}
		catch(Exception E) {
			System.out.println(E.getMessage());
			E.printStackTrace();
		}
		int[] unassigned = distances.countAssigned(loops.size(), -1f);
		for(int i = 0; i < unassigned.length; i++) if(unassigned[i] > 0 ) System.out.println("Loop " + i + " has " + unassigned[i] + " unassigned values");
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
	
	private static String suffix(int val) {
		return suffix(val, 5);
	}
	
	private static String suffix(int val, int digits) {
		String sfx = "";
		while(sfx.length() < digits) sfx += "0";
		sfx += "" + val;
		sfx = sfx.substring(sfx.length() - digits);
		return sfx;
		
	}
	
	public static void main(String[] args) {
		String path           ="/Volumes/General/SUReNet/Data_And_Runs/SUReNet_1_Links_WithUrbanAndRuralFlooding/";

		int startLine = 19000000;
		int lineCount =  1000001;

		int BASE_ITEM = 3;
		
		for(int item = BASE_ITEM; item < 41; item += 50) {
			String whichfile = suffix(item,3);
			String comparisonFile = "pairs_" + whichfile + ".lcf";
			LoopSet.moveFromPrecalc(path, comparisonFile, startLine, lineCount, "fileOut_"+ whichfile + "_003.txt", "continue_003_" + BASE_ITEM + ".txt");			
		}		
	}
	
}
