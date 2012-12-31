package fr.pludov.utils;

public enum ChannelMode {
	Red,
	Green,
	Blue,
	Bayer;
	
	// R G1
	// G2 B
	public static int getRGBBayerId(int x, int y)
	{
		return (x & 1) + (y & 1);
	}
	
	
}
