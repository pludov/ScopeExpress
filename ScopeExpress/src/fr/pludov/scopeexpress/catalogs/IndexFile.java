package fr.pludov.scopeexpress.catalogs;

public class IndexFile extends TableFile {

	public IndexFile() {
		// On fait rentrer l'index en mémoire
		super(42, 10000);
	}

	public long getRecT2()
	{
		return getInt(1, 7);
	}
	
	public long getRecSuppl1()
	{
		return getInt(9, 14);
	}
	
	public double getMinRa()
	{
		return getDouble(16, 21);
	}
	
	public double getMaxRa()
	{
		return getDouble(23, 28);
	}
	
	public double getMinDec()
	{
		return getDouble(30, 35);
	}
	
	public double getMaxDec()
	{
		return getDouble(37, 42);
	}
}
