package jCLD.surenet.utils;

public class HalfFloatMatrix{
	
	private float[] data = null;
	
	
	public HalfFloatMatrix(int size) {
		data = new float[((size * (size - 1)) / 2) + 1];
	}
	
	public HalfFloatMatrix(int size, float initialVal) {
		int total = ((size * (size - 1)) / 2) + 1;
		data = new float[total];
		for(int i = 0; i < total; i++) data[i] = initialVal;
		System.out.println("HFMatrix has " + total + " elements");
	}
	
	
	public void set(int x, int y, float val) {
		data[getIndex(x,y)] = val;
	}
	
	public float get(int x, int y) {
		return data[getIndex(x, y)];
	}
	
	// Checks to see if the value at this index is
	// equal to the value passed; returns true if it
	// is, false if it is not, AND sets it to the specified
	// value
	public boolean check(int x, int y, float val) {
		int indx = getIndex(x,y);
		if(data[indx] == val) return true;
		data[indx] = val;
		return false;
	}
	
	// Pushes a new value into the specified position,
	// while returning the original value. Allows
	// calling routine to determine if value changed.
	public float push(int x, int y, float val) {
		int indx = getIndex(x,y);
		float ret = data[indx];
		data[indx] = val;
		return ret;
	}
	
	public int getIndex(int x, int y) {
		int low = x;
		int high = y;
		if(low > high) {
			low = y;
			high = x;
		}
		return ((high * (high - 1))/2) + low;
	}
	
	public int[] countAssigned(int highestValue, float comparison) {
		int[] ret = new int[highestValue];
		for(int i = 0; i < highestValue - 1; i++) {
			for(int j = i+1; j < highestValue; j++) {
				if(get(j, i) == comparison) ret[i]++;
			}
		}
		return ret;
	}
	
}
