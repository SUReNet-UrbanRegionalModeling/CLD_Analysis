package jCLD.surenet.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

public class DiagramNetwork{
	
	public static class Node{
		Concept            concept;
		Map<Concept, Link> outwardLinks = new HashMap<Concept, Link>();
		Map<Concept, Link> inwardLinks  = new HashMap<Concept, Link>();

		public Node(Concept c) {
			concept = c;
		}
		
		public boolean addInwardLink(Link L) {
			if(inwardLinks.containsKey(L.source)) return false;
			inwardLinks.put(L.source, L);
			return true;
		}

		public boolean addOutwardLink(Link L) {
			if(outwardLinks.containsKey(L.target)) return false;
			outwardLinks.put(L.target, L);
			return true;
		}
		
		public boolean nodeIsSink() {
			return outwardLinks.size() == 0;
		}
		
		public boolean nodeIsSource() {
			return inwardLinks.size() == 0;
		}
				
		public boolean removeLinksTo(Concept C) {
			return( (outwardLinks.remove(C) != null) | (inwardLinks.remove(C) != null) ); // Note: Not short-circuit
		}
		
	}


	// Nodes contain the map of links
	public Map<Concept, Node> nodes = new TreeMap<Concept, Node>();
	
	public void addLink(Link L) {
		Concept source = L.source;
		Concept target = L.target;
		
		Node sourceNode = nodes.get(source);
		if(sourceNode == null) {
			sourceNode = new Node(source);
			nodes.put(source, sourceNode);
		}
		sourceNode.addOutwardLink(L);
		Node targetNode = nodes.get(target);
		if(targetNode == null) {
			targetNode = new Node(target);
			nodes.put(target, targetNode);
		}
		targetNode.addInwardLink(L);		
	}

	
	public void removeNode(Concept C) {
		for(Node n: nodes.values()) n.removeLinksTo(C);
		nodes.remove(C);
	}
	
	public int removeSourcesAndSinks() {
		int countOfRemoved = 0;
		Set<Concept> toRemove = new HashSet<Concept>();
		for(Node n: nodes.values()) if(n.nodeIsSource() || n.nodeIsSink()) toRemove.add(n.concept);
		for(Concept c: toRemove) {
			for(Node n: nodes.values()) n.removeLinksTo(c);
			nodes.remove(c);
			countOfRemoved++;
		}
		return countOfRemoved;
	}
	
	public LoopSet getLoops() {
		return getLoops(true);
	}
	
	public LoopSet getLoops(boolean verbose){
		while(removeSourcesAndSinks() > 0) { }
		LoopSet loopSet = new LoopSet();
		Vector<Node> allNodes = new Vector<Node>();
		allNodes.addAll(nodes.values());
		for(Node n: allNodes) {
			if(!n.nodeIsSink() && !n.nodeIsSource()) {
				getLoops(n, new Sequence(), loopSet, verbose);
				// Once we have found all the loops that pass through this node, we can remove it
				removeNode(n.concept);
				while(removeSourcesAndSinks() > 0) { }
			}
		}
		return loopSet;
	}
	
	public void getLoops(Node n, Sequence s, LoopSet loopSet, boolean verbose) {
		for(Map.Entry<Concept, Link> entry: n.outwardLinks.entrySet()) {
			Sequence nextSequence = new Sequence(s); // Copy original sequence
			nextSequence.addLink(entry.getValue());
			if     (nextSequence.isLoop) {
				
				if(verbose) System.out.println("FOUND LOOP " + (loopSet.getSize() + 1) + ": " + nextSequence.toString());
				String asAdded = loopSet.addLoop(nextSequence).toString();
				if(verbose) System.out.println("ADDED AS " + asAdded);
			}
			else if(nextSequence.isClosed) {
			} // End of recursion; a link has been added to a node that was already in the sequence but was not the initial node
			else   getLoops(nodes.get(entry.getKey()), nextSequence, loopSet, verbose); // Recurse downward
		}
	}
	
}
