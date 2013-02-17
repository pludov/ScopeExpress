package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.MosaicImageParameter;

/**
 * Reçoit une liste de transformation et crée un POI tel que sur chaque image,
 * le poi se projet au même endroit sur chaque image.
 * Le pole est cet endroit de la mosaique
 * 
 * Entre deux transformation A et B, on veut: A.mosaicToImage(polex, poley) = B.mosaicToImage(polex, poley)
 * 
 * 
 * C.a.D:
 *  btx + x * bcs + y * bsn = atx + x * acs + y * asn
 * 	bty + y * bcs - x * bsn = aty + y * acs - x * asn
 * 
 *    
 *  
 *  
 */
public class PoleFindAlgorithm {
	final List<MosaicImageParameter> mipList;
	
	double x, y;
	
	// Listes des matchings individuels
	List<Double> points;
	
	double xsum, ysum, divsum;
	boolean found;
	
	public PoleFindAlgorithm() {
		this.mipList = new ArrayList<MosaicImageParameter>();
		this.points = new ArrayList<Double>();
	}

	public void addMosaicImageParameter(MosaicImageParameter mip)
	{
		this.mipList.add(mip);
	}
	
	public void perform()
	{
		xsum = 0;
		ysum = 0;
		divsum = 0;
		for(int i = 0; i < this.mipList.size(); ++i)
		{
			MosaicImageParameter mipi = this.mipList.get(i);
			if (!mipi.isCorrelated()) continue;
			for(int j = i + 1; j < this.mipList.size(); ++j)
			{
				MosaicImageParameter mipj = this.mipList.get(j);
				if (!mipj.isCorrelated()) continue;
				findPoint(mipi, mipj);
			}
		}
		
		if (divsum > 0) {
 			this.x = this.xsum / this.divsum;
			this.y = this.ysum / this.divsum;
			this.found = true;
		}
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
	
	void findPoint(MosaicImageParameter a, MosaicImageParameter b)
	{
		double atx = a.getTx();
		double aty = a.getTy();
		double acs = a.getCs();
		double asn = a.getSn();
		
		double btx = b.getTx();
		double bty = b.getTy();
		double bcs = b.getCs();
		double bsn = b.getSn();
		
	
		double denominateur = (bcs * bcs + acs * acs + bsn * bsn + asn * asn - (2*((bcs*acs) + (bsn*asn))) );
		// FIXME: le denominateur est une mesure de l'angle en fait
		if (denominateur < 2E-4) {
			return;
		}
		double xnum = (((bty - aty)*(bsn - asn)) + ((btx - atx)*(acs - bcs)));
		double ynum = (((atx - btx)*(bsn - asn)) + ((aty - bty)*(bcs - acs)));

		xsum += xnum;
		ysum += ynum;
		divsum += denominateur;
		points.add(xnum / denominateur);
		points.add(ynum / denominateur);
	}

	public List<Double> getPoints() {
		return points;
	}

	public boolean isFound() {
		return found;
	}
	
}
