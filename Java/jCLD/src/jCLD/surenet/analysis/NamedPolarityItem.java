package jCLD.surenet.analysis;

/**
 * Items that are named and have a polarity
 * must implement this interface in order to be
 * compared in an appropriate way, such that
 * either the original item or 
 */
public interface NamedPolarityItem{

	public int      getId();
	public String   getName();
	public Polarity getPolarity();
	
}
