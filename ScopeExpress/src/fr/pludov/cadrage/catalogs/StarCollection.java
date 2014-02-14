package fr.pludov.cadrage.catalogs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.collections.primitives.DoubleList;

public class StarCollection {

	final DoubleList xyzMag;
	final List<String> references;
	
	public StarCollection() {
		this.xyzMag = new ArrayDoubleList(10000);
		this.references = new ArrayList<String>(3000);
	}

	public int getStarLength()
	{
		return this.references.size();
	}
	
	public void loadStarSky3dPos(int star, double [] sky3dPos)
	{
		int id = 4 * star;
		for(int i = 0; i < 3; ++i) {
			sky3dPos[i] = xyzMag.get(id + i);
		}
	}

	public double getMag(int star)
	{
		return this.xyzMag.get(3 + 4 * star);
	}

	public String getReference(int star)
	{
		return this.references.get(star);
	}
	
	public void addStar(double [] sky3d, double mag, String reference)
	{
		this.xyzMag.add(sky3d[0]);
		this.xyzMag.add(sky3d[1]);
		this.xyzMag.add(sky3d[2]);
		this.xyzMag.add(mag);
		this.references.add(reference);
	}

}
