package fr.pludov.cadrage.utils;

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.utils.EquationSolver;

/**
 * Reçoit une liste de transformation et crée un POI tel que sur chaque image,
 * le poi se projet au même endroit sur chaque image.
 * L'axe de la monture est cet endroit de la mosaique
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
public class AxeFindAlgorithm {
	final List<MosaicImageParameter> mipList;
	
	double x, y;
	
	// Listes des matchings individuels
	List<Double> points;
	
	double xsum, ysum, divsum;
	boolean found;
	
	public AxeFindAlgorithm() {
		this.mipList = new ArrayList<MosaicImageParameter>();
		this.points = new ArrayList<Double>();
	}

	public void addMosaicImageParameter(MosaicImageParameter mip)
	{
		this.mipList.add(mip);
	}
	
	public void perform() throws EndUserException
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
	
	void findPoint(MosaicImageParameter a, MosaicImageParameter b) throws EndUserException
	{
		// Premiere transfo mosaique vers image
		AffineTransform3D t1 = a.getProjection().getTransform();
		// Seconde transfo mosaique vers image
		AffineTransform3D t2 = b.getProjection().getTransform();
		AffineTransform3D rotation;
		try {
			t1 = t1.invert();
			t2 = t2.invert();
			
			// rotation = t1.invert().combine(t2);
	///		t2 = t2.invert();
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
//		
//		// On veut la rotation qui passe de t1 à t2
		rotation = AffineTransform3D.getRotationMatrix(
				t1.getAxis(),
				t2.getAxis());

		// FIXME: si pas de rotation, sortir
		
		// Vecteur dans le repère 3D de la mosaique
		double [] vector = rotation.getRotationAxis();
		
		// Dans l'hemisphere nord svp !
		if (vector[2] < 0) {
			vector[0] = (-vector[0]);
			vector[1] = (-vector[1]);
			vector[2] = (-vector[2]);
		}
		
		// FIXME: si pas de rotation, sortir
		
		// FIXME: ponderer par l'angle de rotation !
		
		// Normalement, on doit avoir le même résultat avec les deux transformation !
		double [] imagePos = b.mosaic3DToImage(Arrays.copyOf(vector, vector.length), null);
		if (imagePos == null) throw new EndUserException("Point invisible sur l'image");
		double [] imagePosA = a.mosaic3DToImage(Arrays.copyOf(vector, vector.length), null);
		if (imagePosA == null) throw new EndUserException("Point invisible sur l'image");
		double dst = Math.sqrt((imagePos[0] - imagePosA[0]) * (imagePos[0] - imagePosA[0]) +
				(imagePos[1] - imagePosA[1]) * (imagePos[1] - imagePosA[1]));
		
		// On pondère en fonction de la précision sur l'image
		double weight = 1.0;
		
		double [] verifAxeA = Arrays.copyOf(vector, vector.length);
		a.getProjection().getTransform().convert(verifAxeA);
		double [] verifAxeB = Arrays.copyOf(vector, vector.length);
		b.getProjection().getTransform().convert(verifAxeB);
		// Normalement, verifAxeA et verifAxeB sont rigoureusement identique
		double [] verifDst = new double[]{
			verifAxeA[0] - verifAxeB[0],
			verifAxeA[1] - verifAxeB[1],
			verifAxeA[2] - verifAxeB[2]
		};
		
		xsum += imagePos[0] * weight;
		ysum += imagePos[1] * weight;
		divsum += weight;
		
		points.add(imagePos[0]);
		points.add(imagePos[1]);
		
//		double atx = a.getTx();
//		double aty = a.getTy();
//		double acs = a.getCs();
//		double asn = a.getSn();
//		
//		double btx = b.getTx();
//		double bty = b.getTy();
//		double bcs = b.getCs();
//		double bsn = b.getSn();
//		
//	
//		double denominateur = (bcs * bcs + acs * acs + bsn * bsn + asn * asn - (2*((bcs*acs) + (bsn*asn))) );
//		// FIXME: le denominateur est une mesure de l'angle en fait
//		if (denominateur < 2E-4) {
//			return;
//		}
//		double xnum = (((bty - aty)*(bsn - asn)) + ((btx - atx)*(acs - bcs)));
//		double ynum = (((atx - btx)*(bsn - asn)) + ((aty - bty)*(bcs - acs)));
//
//		xsum += xnum;
//		ysum += ynum;
//		divsum += denominateur;
//		// points.add(xnum / denominateur);
//		// points.add(ynum / denominateur);
	}

	public List<Double> getPoints() {
		return points;
	}

	public boolean isFound() {
		return found;
	}
	
}
