package jCLD.surenet.analysis;

/**
 * Concepts can have a polarity. This is arbitrary,
 * but is defined with respect to the incoming links.
 * The default polarity is 'positive'; an incoming
 * link with a positive ('increasing') influence will cause
 * a positive polarity node to increase. In some cases
 * it is useful to reverse this, so that the node will
 * effectively decrease in response to a positive influence
 * and increase in response to a negative influence.
 * 
 * For example, consider:
 * 
 *   Climate Change - neg -> Complacency - neg -> Legislation
 *   
 * This sequence implies that an increase in climate change
 * causes a reduction in complacency, and that an increase in
 * complacency causes a reduction in legislation. A simpler
 * formulation is:
 * 
 *   Climate Change - pos -> [!Complacency] - pos -> Legislation
 *  
 *  In this case, the notion of 'Complacency' has been replaced
 *  with a concept of opposite polarity; an increase in climate
 *  change causes this to increase, which in turn increases legislation.
 *  If we assume that the '!Complacency' concept could be renamed
 *  to something like, "Activism" we get;
 *  
 *   Climate Change - pos -> Activism - pos -> Legislation
 *  
 *  In the positive formulation the notion is more intuitive, even if
 *  the negative formulation is what arose during the working session.
 *  
 */
public enum Polarity {
   POSITIVE,
   NEGATIVE
}
