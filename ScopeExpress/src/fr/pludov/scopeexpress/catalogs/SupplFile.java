package fr.pludov.scopeexpress.catalogs;

public class SupplFile extends TableFile {

	public SupplFile() {
		super(122, 128);
		
	}

	public long getTyc1()
	{
		return getInt(1, 4);
	}

	public long getTyc2()
	{
		return getInt(6, 10);
	}

	public long getTyc3()
	{
		return getInt(12, 12);
	}
	
	public double getRa()
	{
		return getDouble(16, 27);
	}
	
	public double getDec()
	{
		return getDouble(29, 40);
	}


	double getVtMag()
	{
		return getDouble(97,102);
	}
	
	double getBtMag()
	{
		return getDouble(84, 89);
	}
	
	double getMagnitude()
	{
		double rslt = getVtMag();
		if (Double.isNaN(rslt)) {
			rslt = getBtMag();
		}
		return rslt;
	}
}
