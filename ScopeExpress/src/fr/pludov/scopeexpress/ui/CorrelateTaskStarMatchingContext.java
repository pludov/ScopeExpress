package fr.pludov.scopeexpress.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.pludov.scopeexpress.catalogs.StarCollection;
import fr.pludov.scopeexpress.catalogs.StarProvider;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarCorrelationPosition;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.ui.CorrelateTask.ImageStar;
import fr.pludov.scopeexpress.utils.PointMatchAlgorithm;
import fr.pludov.scopeexpress.utils.PointMatchAlgorithm.Correlation;

/**
 * Encapsule les étoiles de réference lors d'une correlation
 */
public class CorrelateTaskStarMatchingContext {
	final SkyProjection skyProjection;
	final int imgWidth, imgHeight;
	final Mosaic mosaic;
	final double pixelTolerance;
	StarCollection collection;
	// Identifiant => ReferenceCatalogStar
	Map<String, ReferenceStar> refStars = new HashMap<String, ReferenceStar>();
	List<ReferenceStar> referenceStars = new ArrayList<ReferenceStar>();
	
	
	Map<Image, Double> magnitudeEstimation = new HashMap<Image, Double>();;
	private List<Correlation> correlations;
	List<ImageStar> destStars;
	
	CorrelateTaskStarMatchingContext(Mosaic mosaic, SkyProjection skyProj, int imgWidth, int imgHeight, double pixelTolerance)
	{
		this.mosaic = mosaic;
		this.skyProjection = skyProj;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.pixelTolerance = pixelTolerance;
	}
		

	// Charge depuis le catalogue
	public void searchStars()
	{
		// FIXME: magnitude et rayon sont en dûr.
		this.collection = StarProvider.getStarAroundNorth(skyProjection, 3, 12);

		double [] refImgPos = new double[3];
		double [] tmp2d = new double[2];
		
		for(int i = 0; i < collection.getStarLength(); ++i)
		{
			collection.loadStarSky3dPos(i, refImgPos);
			double x, y;
			if (skyProjection.sky3dToImage2d(refImgPos, tmp2d)) {
				x = tmp2d[0];
				y = tmp2d[1];
			} else {
				continue;
			}

			ReferenceStar refStar = new ReferenceStar(i, x, y);
			referenceStars.add(refStar);
			refStars.put(refStar.getReference(), refStar);
		}
	}
	
	/**
	 * Retourne la valeur de magnitude à ajouter à so.getUnscaledMagByChannel(-1)
	 * pour avoir des magnitude réelle
	 * 
	 */
	Double getMagnitudeEstimation(Image image)
	{
		if (magnitudeEstimation.containsKey(image))  {
			return magnitudeEstimation.get(image);
		}
		
		double deltaSum = 0;
		int count = 0;
		for(StarOccurence so : mosaic.getStarOccurences(image))
		{
			if (so.getStar().getPositionStatus() == StarCorrelationPosition.Reference)
			{
				// On a une 
				double magFound = so.getUnscaledMagByChannel(-1);
				double magCatalog = so.getStar().getMagnitude();
				
				deltaSum += (magCatalog - magFound);
				count++;
			}
		}
		Double result;
		if (count == 0) {
			result = null;
		} else {
			result = deltaSum / count;
		}
		
		magnitudeEstimation.put(image, result);
		
		return result;
		
	}
	
	double estimateMag(Star s)
	{
		double magEstimate = 0;
		int magEstimateCount = 0;
		for(StarOccurence so : this.mosaic.getStarOccurences(s))
		{
			Double magForImg = getMagnitudeEstimation(so.getImage());
			if (magForImg == null) continue;
			
			double mag = so.getUnscaledMagByChannel(-1) + magForImg;
			magEstimate += mag;
			magEstimateCount ++;
		}
		
		if (magEstimateCount == 0) return Double.NaN;
		return magEstimate / magEstimateCount;
	}

	// Charge depuis l'image
	public void loadMosaicStars()
	{
		double [] tmp2d = new double[2];
		
		// Projetter l'étoile sur la mosaic
		for(Star s : mosaic.getStars())
		{
			if (s.getReference() != null) {
				ReferenceStar rc = refStars.get(s.getReference());
				if (rc != null) {
					rc.star = s;
					continue;
				}
			}
			
			double [] spos = s.getPossiblePosition();
			if (spos == null) continue;
			
			if (this.skyProjection.sky3dToImage2d(spos, tmp2d)) {
				// Trouver une estimation de la magnitude
				
				// Trouver la magnitude
				double mag = estimateMag(s);
				if (Double.isNaN(mag)) continue;
				
				// FIXME: tester qu'on n'est pas à l'autre bout du monde... 
				ReferenceStar refStar = new ReferenceStar(s, tmp2d[0], tmp2d[1], mag);
				referenceStars.add(refStar);
			}
		}
	}
	
	public class ReferenceStar {
		final double imgx, imgy;
		final double adu;
		Star star;

		final int id;
		
		ReferenceStar(int id, double x, double y)
		{
			this.id = id;
			this.adu = Math.pow(2.512, -collection.getMag(id));
			
			this.imgx = x;
			this.imgy = y;
			this.star = null;
		}
		
		ReferenceStar(Star star, double x, double y, double magEstimate)
		{
			this.id = -1;
			this.adu = Math.pow(2.512, -magEstimate);
			
			this.imgx = x;
			this.imgy = y;
			this.star = star;
		}
		
		public double getAduLevel() {
			return adu;
		}

		public String getReference() {
			return collection.getReference(id);
		}

		public double[] getSky3dPos() {
			double [] result = new double[3];
			collection.loadStarSky3dPos(id, result);
			return result;
		}

		public double getMagnitude() {
			return collection.getMag(id);
		}

	}

	public void correlate(List<ImageStar> destStars) {
		this.destStars = destStars;
		double [] otherX = new double[destStars.size()];
		double [] otherY = new double[destStars.size()];

		// Calculer la projection de toutes les étoiles de l'image sur l'image
		for(int i = 0; i < destStars.size(); ++i)
		{
			ImageStar otherSo = destStars.get(i);
			otherX[i] = otherSo.getX();
			otherY[i] = otherSo.getY();
		}
		
		double [] referenceX = new double[referenceStars.size()];
		double [] referenceY = new double[referenceStars.size()];
		for(int i = 0; i < referenceStars.size(); ++i)
		{
			referenceX[i] = referenceStars.get(i).imgx;
			referenceY[i] = referenceStars.get(i).imgy;
		}

		// FIXME : le nombre de pixel correspondant à la taille des étoile
		PointMatchAlgorithm algo = new PointMatchAlgorithm(referenceX, referenceY, otherX, otherY, pixelTolerance);
		
		correlations = algo.proceed();
	}
	
	public void mergeStars()
	{
		for(Correlation c : correlations)
		{
			int refStarId = c.getP1();
			int otherStarId = c.getP2();

			ReferenceStar ref = referenceStars.get(refStarId);
			ImageStar iStar = destStars.get(otherStarId);

			StarOccurence otherSo = iStar.so;
			
			if (!mosaic.exists(otherSo)) {
				continue;
			}
			
			// ref est soit une image du catalogue, soit une image qui n'est pas dans le catalogue
			if (ref.star == null) {
				// otherSo devient liée à une image du catalogue
				otherSo.getStar().setCorrelatedPos(ref.getSky3dPos());
				otherSo.getStar().setReference(ref.getReference());
				otherSo.getStar().setMagnitude(ref.getMagnitude());
				otherSo.getStar().setPositionStatus(StarCorrelationPosition.Reference);
			} else {
				// On merge juste les stars
				mosaic.mergeStarOccurence(ref.star, otherSo);
			}
		}
	}

}
