package jCLD.surenet.analysis;

/**
 * A Link is a source, a target, and an influence type
 */
public class Link{

	public  Concept   source;
	public  Influence influence;
	public  Concept   target;
	
	private String    representation;
	
	/**
	 * Basic constructor; also initializes the representation
	 * @param src
	 * @param infl
	 * @param tgt
	 */
	public Link(Concept src, Influence infl, Concept tgt) {
		source         = src;
		influence      = infl;
		target         = tgt;
		initRepresentation();
	}

	private void initRepresentation() {
		representation = "__" + source.toString() + "_" + influence + "_" + target.toString() + "__";
	}
	
	/**
	 * Copy constructor creates another link
	 * using the same source, target, and influence
	 * @param other
	 */
	public Link(Link other) {
		source    = other.source;
		influence = other.influence;
		target    = other.target;
	}
	
	public String toString() {
		return representation;
	}
	
	/**
	 * Reverses the influence of this link.
	 * Note that this does NOT reverse the polarity of either
	 * the source or the target, although it might seem like it should
	 * do so. The reason is that the polarity of the source and/or target
	 * should be reversed independently, in case it participates in multiple
	 * links. Re-initializes the representation to reflect the new influence.
	 * @return
	 */
	public Influence reverseInfluence() {
		influence = (influence == Influence.INCREASES ? Influence.DECREASES: Influence.INCREASES);
		initRepresentation();
		return influence;
	}
	
}
