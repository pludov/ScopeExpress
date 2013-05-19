package fr.pludov.cadrage.catalogs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.collections.primitives.DoubleList;

public class StarCollection {

	final DoubleList xyMag;
	final List<String> references;
	
	public StarCollection() {
		this.xyMag = new ArrayDoubleList(10000);
		this.references = new ArrayList<String>(3000);
	}

	public int getStarLength()
	{
		return this.references.size();
	}
	
	public double getX(int star)
	{
		return this.xyMag.get(3 * star);
	}
	
	public double getY(int star)
	{
		return this.xyMag.get(1 + 3 * star);
	}

	public double getMag(int star)
	{
		return this.xyMag.get(2 + 3 * star);
	}

	public String getReference(int star)
	{
		return this.references.get(star);
	}
	
	public void addStar(double x, double y, double mag, String reference)
	{
		this.xyMag.add(x);
		this.xyMag.add(y);
		this.xyMag.add(mag);
		this.references.add(reference);
	}

}
