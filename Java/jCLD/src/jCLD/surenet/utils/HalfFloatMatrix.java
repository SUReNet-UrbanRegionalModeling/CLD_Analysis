package jCLD.surenet.utils;

public class HalfFloatMatrix{
	
	private float[] data = null;
	
	
	public HalfFloatMatrix(int size) {
		data = new float[((size * (size - 1)) / 2) + 1];
	}
	
	public void set(int x, int y, float val) {
		data[getIndex(x,y)] = val;
	}
	
	public float get(int x, int y) {
		return data[getIndex(x, y)];
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
	
	
}
