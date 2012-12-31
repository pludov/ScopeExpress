package fr.pludov.io;

public class FitsPlane {
	float [] value;
	int sx, sy;
	
	public FitsPlane(int sx, int sy)
	{
		this.sx = sx;
		this.sy = sy;
		this.value = new float[sx * sy];
	}

	public float[] getValue() {
		return value;
	}

	public int getSx() {
		return sx;
	}

	public int getSy() {
		return sy;
	}
	
}
