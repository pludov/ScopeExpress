package fr.pludov.cadrage.catalogs;

public class StarFile extends TableFile {

	public StarFile() {
		super(206, 4096);
	}
	
	long getTyc1()
	{
		return getInt(1, 4);
	}
	
	long getTyc2()
	{
		return getInt(6, 10);
	}

	double getMeanRa()
	{
		return getDouble(16, 27);
	}
	
	double getMeanDec()
	{
		return getDouble(29, 40);
	}
	
	double getTyc2Ra()
	{
		return getDouble(153, 164);
	}
	
	double getTyc2Dec()
	{
		return getDouble(166, 177);
	}
	
	double getRaCorrected(int year)
	{
		double ra = getRa();
		double raMotion = getDouble(42, 48);
		if (!Double.isNaN(raMotion))
		{
			double corr = (year - 2000) * raMotion / 1000.0;
			double dec = getDec();
			double raDecFactor = 1.0 / Math.cos(dec * Math.PI / 180);
			corr *= raDecFactor;
			ra += corr / (3600);
		} 
		return ra;
		
	}
	
	double getDecCorrected(int year)
	{
		double dec = getDec();
		double decMotion = getDouble(50, 56);
		if (!Double.isNaN(decMotion))
		{
			double corr = (year - 2000) * decMotion / 1000.0;
			dec += corr / 3600;
		}
		
		return dec;
	}
	
	double getRa()
	{
		double rslt = getMeanRa();
		if (Double.isNaN(rslt)) {
			rslt = getTyc2Ra();
		}
		return rslt;
	}
	
	double getDec()
	{
		double rslt = getMeanDec();
		if (Double.isNaN(rslt)) {
			rslt = getTyc2Dec();
		}
		return rslt;
	}
	
	double getVtMag()
	{
		return getDouble(124,129);
	}
	
	double getBtMag()
	{
		return getDouble(111, 116);
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
