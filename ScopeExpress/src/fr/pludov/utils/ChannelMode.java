package fr.pludov.utils;

public enum ChannelMode {
	Red,
	Green,
	Blue,
	// En fait, ça veut dire : on ajoute tout !
	Bayer;
	
	// R G1
	// G2 B
	// Red => 0;
	// Green => 1
	// Blue => 2
	public static int getRGBBayerId(int x, int y)
	{
		return (x & 1) + (y & 1);
	}
	
	
}
