package jCLD.surenet.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;


public class LoopSetLoader{
	
	private DiagramNetwork       network;
	private Set<Link>            allLinks;
	private LoopSet              loopSet;
	private Map<Concept, Double> scores;
	
	public void loadLoopSet(String path, String filename) {
		System.out.println("Starting load network and load allLinks");
		network  = new DiagramNetwork();	
		allLinks = new HashSet<Link>();
	    try {	        
	    	// This ensures that the same network file will return the same concepts and be assigned the same IDs
	    	System.out.println("Loading concepts via concept factory");
	        ConceptFactory.loadConceptSetFromNetworkFile(path + filename);
	        System.out.println("Done with concept load.");
	        
	        Scanner myReader = new Scanner(new File(path + filename));
	    	
	        System.out.println("Beginning read of links...");
	        String data = myReader.nextLine(); // Skip the first line
	        while (myReader.hasNextLine()) {
	          data = myReader.nextLine();  
	          
	          String[]  info      = data.split(",");
	          Concept   concept1  = ConceptFactory.getConcept(info[0]);
	          Concept   concept2  = ConceptFactory.getConcept(info[1]);
	          Influence influence = info[2].compareTo("Positive") == 0 ? Influence.INCREASES : Influence.DECREASES;
	          Link      toAdd     = new Link(concept1, influence, concept2);
	          network.addLink(toAdd);
	          allLinks.add(toAdd);
	        }
	        System.out.println("Done with link read.");
	        myReader.close();
	      } catch (FileNotFoundException e) {
	        System.out.println("An error occurred.");
	        e.printStackTrace();
	      }
	    
	    System.out.println(network.nodes.size() + " nodes in network map...");

	    loopSet = network.getLoops();
        for(Link l: allLinks) {
        	System.out.println("Loops containing:" + l.toString() + ":" + loopSet.loopsContainingLink(l.source,l.target));
        }

	    return;
	}
	    
	public void writeLoopSetComparisonsToFile(String path, String filename, long maxLines, boolean pause) {		
		loopSet.writeOnlyComparisonsToFile(path + filename, maxLines, pause);
	}
	    
	public void readLoopSetComparisonsFromFiles(String path, String[] filenames) {
		loopSet.readPrescores(path, filenames);
	}
	
	public void getScores() {
		if(scores == null) scores = loopSet.getConceptsAndScores(true);
	}
	
	public void writeConceptNodeFile(String path, String outputFilename) {
		try {
			FileWriter writer = new FileWriter(path + outputFilename);
			writer.write("id,numberOfLoops,relevanceScore" + System.lineSeparator());
			for(Concept c: ConceptFactory.getAll()) {
				double score = scores.containsKey(c) ? scores.get(c) : 0.0;
				long loopsTraversing = loopSet.loopsContainingConcept(c);
				writer.write(c.getRepresentation() + "," + loopsTraversing + "," + score + System.lineSeparator());
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	
    public void writeConceptLinkFile(String path, String outputFilename) {
    	try {
			FileWriter writer = new FileWriter(path + outputFilename);
		    writer.write("CONCEPTLINKFILE|source,target,linkInfluence,loopsTraversing" + System.lineSeparator());
		    for(Link l: allLinks) {
		    	long loopCount = loopSet.loopsContainingLink(l.source, l.target);
		    	if(loopCount > 0) writer.write(l.source.getRepresentation() + "," + l.target.getRepresentation() + "," + l.influence.toString() + "," + loopCount + System.lineSeparator());
		    }
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
    public void writeLoopNodeFile(String path, String outputFilename) {
    	try {
			FileWriter writer = new FileWriter(path + outputFilename);
			writer.write("id,size" + System.lineSeparator());
		    for(Sequence s: loopSet.loopsSortedBySize()) {
		    	writer.write(s.getID() + "," + s.getSize() + System.lineSeparator());
		    } 		    
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
    }

    // This version only writes the loops as links; it does not retrieve the score
    // However, to avoid accidentally writing a gigantic file, you _must_
    // pass a line limit
    public void writeLoopLinkFile(String path, String outputFilename, long limitLines) {
    	try {
			FileWriter writer = new FileWriter(path + outputFilename);
			writer.write("source,target" + System.lineSeparator());
		    Vector<Sequence> allLoops = loopSet.loopsSortedBySize();
		    int countOfAllLoops = allLoops.size();
		    int count = 0;
		    mainLoop:
		    for(int i = 0; i < countOfAllLoops -1; i++) {
		    	Sequence s1 = allLoops.get(i);
		    	for(int j = i; j < countOfAllLoops; j++) {
		    		if(i != j) {
			    		Sequence s2 = allLoops.get(j);
		    			writer.write(i + "|" + j + "|" + countOfAllLoops +"|" + (count++) + "):" + s1.getID() + "," + s2.getID() + System.lineSeparator());
		    			count++;
		    			if(count > limitLines) break mainLoop;
		    		}
		    	}
		    }
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
    }
    
    public void writeLoopLinkFile(String path, String outputFilename, double threshold) {
    	try {
			FileWriter writer = new FileWriter(path + outputFilename);
			writer.write("source,target,distance" + System.lineSeparator());
		    Vector<Sequence> allLoops = loopSet.loopsSortedBySize();
		    int countOfAllLoops = allLoops.size();
		    int count = 0;
		    double THRESHOLD = .151;
		    for(int i = 0; i < countOfAllLoops -1; i++) {
		    	Sequence s1 = allLoops.get(i);
		    	for(int j = i; j < countOfAllLoops; j++) {
		    		if(i != j) {
			    		Sequence s2 = allLoops.get(j);
		    			double dist = loopSet.getStoredDistance(s1, s2);
		    			if(dist < THRESHOLD) {
		    				writer.write(i + "|" + j + "|" + countOfAllLoops +"|" + (count++) + "):" + s1.getID() + "," + s2.getID() + "," + dist + System.lineSeparator());
		    			}
		    		}
		    	}
		    }
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
    }

    	
	public void reportFileScoreSet(String path, String outputFilename) {   
    	try {
			FileWriter writer = new FileWriter(path + outputFilename);
		    for(Concept c: scores.keySet()) writer.write(c.getRepresentation() + " = " + scores.get(c) + System.lineSeparator());			
		    writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		

	}

}
