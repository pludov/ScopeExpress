package fr.pludov.cadrage.focus.correlation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.utils.DynamicGrid;
import fr.pludov.cadrage.utils.DynamicGridPoint;
import fr.pludov.cadrage.utils.DynamicGridPointWithAdu;
import fr.pludov.cadrage.utils.PointMatchAlgorithm;
import fr.pludov.cadrage.utils.PointMatchAlgorithm.DynamicGridPointWithIndex;
import fr.pludov.cadrage.utils.Ransac;

/**
 * Correlation est une collection d'images corrélée
 * 
 * Chaque image a un status et une position relative
 * 
 * Une liste des étoiles identifiées est gardée
 * 
 * 
 * @author Ludovic POLLET
 *
 */
public class Correlation {
	private static final Logger logger = Logger.getLogger(Correlation.class);
	
	double tx, ty;
	double cs, sn;
	boolean found;
	
	public Correlation()
	{
		this.found = false;
	}
	
	private static class CorrelationStatus
	{
		List<PointMatchAlgorithm.Correlation> correlation;
		int count;
		double cs, sn, tx, ty;
	}

	private static int incChannel(int ch, double value)
	{
		ch = 255 - ch;
		ch *= value;
		ch = 255 - ch;
		return ch;
	}
	
	private static int incRgb(int rgb)
	{
		int r = rgb & 255;
		int g = (rgb >> 8) & 255;
		int b = (rgb >> 16) & 255;
		
		r = incChannel(rgb, 0.5);
		g = incChannel(rgb, 0.25);
		b = incChannel(rgb, 0.125);
		
		rgb = r | (g << 8) | (b << 16);
		return rgb;
	}
	
	private static void saveDGP(List<? extends DynamicGridPoint> triangles, File pathName)
	{
		BufferedImage bi;
		int maxWidth = 512;
		bi = new BufferedImage(maxWidth, maxWidth, BufferedImage.TYPE_INT_RGB);
		
		double x0, y0, x1, y1;
		x0 = 0;
		y0 = 0;
		x1 = 1;
		y1 = 1;
	
		if (!triangles.isEmpty()) {
			
			for(DynamicGridPoint point : triangles)
			{
				double x = (double)point.getX();
				double y = (double)point.getY();
				if (x > x1) x1 = x;
				if (y > y1) y1 = y;
				if (x < x0) x0 = x;
				if (y < y0) y0 = y;
			}
			
			x0 -= 0.01;
			y0 -= 0.01;
			x1 += 0.01;
			y1 += 0.01;
			
			double scale = maxWidth * 1.0 / Math.max(x1 - x0, y1 - y0);
			for(DynamicGridPoint point : triangles)
			{
				int scx = (int)Math.round((point.getX() - x0) * scale);
				int scy = (int)Math.round((point.getY() - y0) * scale);
				
				
				int rgb = bi.getRGB(scx, scy);
				rgb = incRgb(rgb);
				bi.setRGB(scx, scy, rgb);
			}
			
		}
		try {
			ImageIO.write(bi, "bmp", pathName);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Enregistre une image de debug présentant les étoiles et les triangles
	 */
	private static void saveTriangles(List<? extends DynamicGridPointWithAdu> stars, List<Triangle> triangles, File pathName)
	{
		BufferedImage bi;
		int maxWidth = 2048;
		bi = new BufferedImage(maxWidth, maxWidth, BufferedImage.TYPE_INT_RGB);
		logger.debug("drawing " + stars.size() + " stars, " + triangles.size() + " triangles");
		if (!stars.isEmpty()) {
			int x0, y0, x1, y1;
			double maxAdu;
			x0 = (int)stars.get(0).getX();
			y0 = (int)stars.get(0).getY();
			maxAdu = stars.get(0).getAduLevel();
			x1 = x0;
			y1 = y0;
			
			for(DynamicGridPointWithAdu point : stars)
			{
				int x = (int)point.getX();
				int y = (int)point.getY();
				if (x > x1) x1 = x;
				if (y > y1) y1 = y;
				if (x < x0) x0 = x;
				if (y < y0) y0 = y;
				if (point.getAduLevel() > maxAdu) maxAdu = point.getAduLevel();
			}
			
			
			double scale = maxWidth * 1.0 / Math.max(x1 - x0, y1 - y0);
			
			Graphics2D g2d = bi.createGraphics();
			g2d.setColor(Color.white);
			for(DynamicGridPointWithAdu point : stars)
			{
				double scx = (point.getX() - x0) * scale;
				double scy = (point.getY() - y0) * scale;
				
				double mag = 6 - (Math.log(maxAdu) - Math.log(point.getAduLevel()));
				if (mag < 1) mag = 1;
				g2d.drawOval((int)Math.round(scx - mag), (int)Math.round(scy - mag), (int)Math.round(2 * mag + 1), (int)Math.round(2 * mag + 1));
			}
			
			int cid = 0;
			Color [] colors = {Color.red, Color.blue, Color.green, Color.cyan, Color.magenta, Color.orange, Color.pink, Color.yellow}; 
			for(Triangle t : triangles)
			{
				g2d.setColor(colors[cid]);
				cid++;
				if (cid == colors.length) cid = 0;
				
				DynamicGridPoint point = t.s1;
				double scx0 = (point.getX() - x0) * scale;
				double scy0 = (point.getY() - y0) * scale;
				
				point = t.s2;
				double scx1 = (point.getX() - x0) * scale;
				double scy1 = (point.getY() - y0) * scale;
				
				point = t.s3;
				double scx2 = (point.getX() - x0) * scale;
				double scy2 = (point.getY() - y0) * scale;
				
				g2d.drawLine((int)Math.round(scx0), (int)Math.round(scy0), (int)Math.round(scx1), (int)Math.round(scy1));
				g2d.drawLine((int)Math.round(scx0), (int)Math.round(scy0), (int)Math.round(scx2), (int)Math.round(scy2));
				g2d.drawLine((int)Math.round(scx2), (int)Math.round(scy2), (int)Math.round(scx1), (int)Math.round(scy1));
			
			}
			
		}
		logger.debug("saving stars");
		try {
			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("BMP");
			
			if (!writers.hasNext()) {
				throw new Exception("bmp not supported");
			}
			ImageWriter iw = writers.next();
			try {
				ImageWriteParam params = iw.getDefaultWriteParam();
				ImageOutputStream stream = ImageIO.createImageOutputStream(pathName);
				try {
					iw.setOutput(stream);
					
					iw.write(null, new IIOImage(bi, null, null), params);
				} finally {
					stream.close();
				}
			} finally {
				iw.dispose();
			}
			
//			ImageIO.write(bi, "png", pathName);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void identity()
	{
		// logger.info("Found best translation with " + best.correlation.size() + " star correlations (found by " + best.count + " ransac points)");
		found = true;
		this.tx = 0.0;
		this.ty = 0.0;
		this.cs = 1.0;
		this.sn = 1.0;	
	}

	/**
	 * Trouve la distance qui permet de garder au moins 90% des étoiles de starsFromImage
	 * dmax sert à limiter la recherche
	 */
	public double getOtpimalRay(
							List<? extends DynamicGridPointWithAdu> starsFromImage,
							DynamicGrid<? extends DynamicGridPointWithAdu> starGrid,
							double dmax,
							int keepCount,
							double brightestStarRatio)
	{
		// Distance mini au carré
		double [] d2min = new double[starsFromImage.size()];
		int i = 0;
		for(DynamicGridPointWithAdu star : starsFromImage)
		{
			double mind2 = dmax * dmax;
			double minAduLevel = star.getAduLevel() * brightestStarRatio;
			for(DynamicGridPointWithAdu otherStar : starGrid.getNearObject(star.getX(), star.getY(), dmax))
			{
				if (otherStar.getAduLevel() <= minAduLevel) {
					continue;
				}
				if (otherStar == star) {
					continue;
				}
				
				double d2 = (star.getX() - otherStar.getX()) * (star.getX() - otherStar.getX())
							+ (star.getY() - otherStar.getY()) * (star.getY() - otherStar.getY());
				if (d2 < mind2) {
					mind2 = d2;
				}
			}
			d2min[i++] = mind2;
		}
		
		Arrays.sort(d2min);
		int idToTake;
		
		if (keepCount >= d2min.length) {
			idToTake = 0;
		} else {
			idToTake = d2min.length - keepCount - 1;
		}
		return Math.sqrt(d2min[idToTake]);
	}
	
	/**
	 * Retire de la liste toutes les étoiles qui ont un voisin plus brillant dans le rayon indiqué
	 * Un voisin est considéré brillant si voisin.adu > this.adu*brightestStarRatio
	 * 
	 * un ratio > 1 permet d'ignorer les voisin légèrement plus brillants
	 *  
	 * @param starsFromImage
	 * @param rayWithNoBrighterStar
	 * @param brightestStarRatio
	 * @return
	 */
	public ArrayList<DynamicGridPointWithAdu> filterStarWithBrightNeighboors(
							List<? extends DynamicGridPointWithAdu> stars, 
							DynamicGrid<? extends DynamicGridPointWithAdu> starGrid,
							double rayWithNoBrighterStar, double brightestStarRatio)
	{
		ArrayList<DynamicGridPointWithAdu> result = new ArrayList<DynamicGridPointWithAdu>(stars.size());
		if (starGrid == null) {
			starGrid = new DynamicGrid<DynamicGridPointWithAdu>((List<DynamicGridPointWithAdu>)stars);
		}
		
	StarLoop:
		for(DynamicGridPointWithAdu star : stars)
		{
			double minAduLevel = star.getAduLevel() * brightestStarRatio;
			for(DynamicGridPointWithAdu otherStar : starGrid.getNearObject(star.getX(), star.getY(), rayWithNoBrighterStar))
			{
				if (otherStar.getAduLevel() <= minAduLevel) {
					continue;
				}
				if (otherStar == star) {
					continue;
				}
				continue StarLoop;
			}
			
			result.add(star);
		}
		return result;
	}
	
	public void correlate(List<? extends DynamicGridPointWithAdu> starsFromImage, List<? extends DynamicGridPointWithAdu> starsFromRef, double refToImageRatio)
	{
		int maxTriangle = 900; // FIXME : c'est ad hoc...
		double starRay = 1200; 	// Prendre en compte des triangles de au plus cette taille
		double starMinRay = 900;	// Elimine les petits triangles
		double maxBrightnessRatio = 5.5;	// On considère des triplet d'étoiles avec ce rapport maxi d'éclat 
		double minGeoRatio = 0.15;	// Facteur par rapport au triangle "8-9-10" (à 1 on ne retiendra que lui) 
		
		List<Triangle> trianglesFromImage;
		
		List<Triangle> trianglesFromRef;
		
		// Exclu les étoiles qui ont une voisine au moins x fois plus lumineuse
		double brightestStarRatio = 2;

		// FIXME: c'est en dûr ! Ce paramètre sert de born min à la distance (dans le cas où il y a trés peu d'image) 
		double dmax = 600;
		
		int keepCount, allowRemoval;
		if (starsFromImage.size() < 40) {
			allowRemoval = 10;
		} else {
			allowRemoval = starsFromImage.size() / 4;
		}
		keepCount = starsFromImage.size() - allowRemoval;
		if (keepCount > 200) {
			keepCount = 200;
		}
		
		DynamicGrid<DynamicGridPointWithAdu> starGrid = new DynamicGrid<DynamicGridPointWithAdu>((List<DynamicGridPointWithAdu>)starsFromImage);
		double rayWithNoBrighterStar = getOtpimalRay(starsFromImage, starGrid, dmax, keepCount, brightestStarRatio);
		
		logger.info("Restricting to stars that has no brighter star within " + rayWithNoBrighterStar + " pixels in image");
		int orginalStarFromImageCount = starsFromImage.size();
		starsFromImage = filterStarWithBrightNeighboors(starsFromImage, starGrid, rayWithNoBrighterStar, brightestStarRatio);
		logger.info("Star from image filtered from " + orginalStarFromImageCount + " down to " + starsFromImage.size() + " stars");
		int orginalStarFromRefCount = starsFromRef.size();
		starsFromRef = filterStarWithBrightNeighboors(starsFromRef, null, rayWithNoBrighterStar / refToImageRatio, brightestStarRatio);
		logger.info("Star from reference filtered from " + orginalStarFromRefCount + " down to " + starsFromRef.size() + " stars");
		
		Collections.sort(starsFromImage, new Comparator<DynamicGridPointWithAdu>() {
			@Override
			public int compare(DynamicGridPointWithAdu o1, DynamicGridPointWithAdu o2) {
				
				double thisVal = o1.getAduLevel();
				double anotherVal = o2.getAduLevel();
				return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
			}
		});
		
		Collections.sort(starsFromRef, new Comparator<DynamicGridPointWithAdu>() {
			@Override
			public int compare(DynamicGridPointWithAdu o1, DynamicGridPointWithAdu o2) {
				
				double thisVal = o1.getAduLevel();
				double anotherVal = o2.getAduLevel();
				return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
			}
		});
		

		int lengthToTest = starsFromImage.size();
		int lengthForRef = starsFromRef.size();
		do {
			// FIXME : on voudrait que les triangles ne se recouvrent pas trop !
			logger.debug("Looking for image triangles, max size = " + starRay + " brightness ratio = " + maxBrightnessRatio);
			trianglesFromImage = getTriangleList(starsFromImage.subList(0, lengthToTest), starMinRay, starRay, maxTriangle, maxBrightnessRatio, minGeoRatio);
			if (trianglesFromImage == null) {
				
//				if (lengthToTest > 10) {
//					logger.warn("Too many triangles found, restrict to brightest stars");
//					lengthToTest = (int)Math.round(0.75 * lengthToTest);
//				} else {
					logger.warn("Too many triangles found, restrict morphology/brightness");
//					maxBrightnessRatio = 1 + (maxBrightnessRatio - 1) * 0.95;
					minGeoRatio = Math.pow(minGeoRatio, 0.8);
//				}
				continue;
			}

			if (trianglesFromImage.size() == 0) {
				logger.warn("Not enough triangle in image");
				found = false;
				return;
			}
			logger.debug("Looking for references triangles, max size = " + starRay);
			trianglesFromRef = getTriangleList(starsFromRef.subList(0, lengthForRef), starMinRay / refToImageRatio, starRay / refToImageRatio, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
			if (trianglesFromRef == null) {
				
//				if (lengthForRef > 10) {
//					logger.warn("Too many triangles found, restrict to brightest stars");
//					lengthForRef = (int)Math.round(0.75 * lengthForRef);
//				} else {
					logger.warn("Too many triangles found, restrict morphology/brightness");
//					maxBrightnessRatio = 1 + (maxBrightnessRatio - 1) * 0.95;
					minGeoRatio = Math.pow(minGeoRatio, 0.8);
//				}
				continue;
			}
			
			break;
		} while(true);
		
		if (logger.isDebugEnabled()) {
			logger.debug("Saving images");
			saveTriangles(starsFromRef/*.subList(0, lengthForRef)*/, trianglesFromRef, new File("c:\\triangles-ref.bmp"));
			saveDGP(trianglesFromRef, new File("c:\\triangles-ref-ratio.bmp"));
			saveTriangles(starsFromImage/*.subList(0, lengthToTest)*/, trianglesFromImage, new File("c:\\triangles-image.bmp"));
			saveDGP(trianglesFromImage, new File("c:\\triangles-image-ratio.bmp"));
		}
		
		logger.debug("Creating dynamic grid table");
		DynamicGrid<Triangle> referenceTriangleGrid = new DynamicGrid<Triangle>(trianglesFromImage);
		logger.debug("Dynamic grid table created");
		
		// Cette tolerance est absolue. Elle a été ajustée pour ne pas rejetter 
		// d'attracteurs sur une image prise près de la polaire
		double tolerance = 0.006;
		int maxNbRansacPoints = 200000;
		
		List<RansacPoint> ransacPoints;
		
		while((ransacPoints = getRansacPoints(trianglesFromRef, referenceTriangleGrid, maxNbRansacPoints, tolerance, (refToImageRatio) * 0.95, (refToImageRatio) * 1.05)) == null)
		{
			logger.warn("Too many possible translations. Filter wiht more aggressive values...");
			tolerance *= 0.6;
		}
		
		logger.info("Found " + ransacPoints.size() + " translations");
		
		double [] srcX = new double[starsFromImage.size()];
		double [] srcY = new double[starsFromImage.size()];
		
		
		List<PointMatchAlgorithm.DynamicGridPointWithIndex> refStarsWithIndex = new ArrayList<PointMatchAlgorithm.DynamicGridPointWithIndex>();
		for(int i = 0; i < starsFromRef.size(); ++i)
		{
			DynamicGridPoint dgp = starsFromRef.get(i);
			double x = dgp.getX();
			double y = dgp.getY();

			PointMatchAlgorithm.DynamicGridPointWithIndex refStarWithIndex;
			refStarWithIndex = new PointMatchAlgorithm.DynamicGridPointWithIndex(x, y, i);
			refStarsWithIndex.add(refStarWithIndex);
		}
		
		DynamicGrid<PointMatchAlgorithm.DynamicGridPointWithIndex> refGrid = new DynamicGrid<PointMatchAlgorithm.DynamicGridPointWithIndex>(refStarsWithIndex);
		
		Map<List<PointMatchAlgorithm.Correlation>, CorrelationStatus> correlations = new HashMap<List<PointMatchAlgorithm.Correlation>, Correlation.CorrelationStatus>(ransacPoints.size());
		for(RansacPoint rp : ransacPoints)
		{
			double[] tmp = new double[2];
			for(int i = 0; i < starsFromImage.size(); ++i)
			{
				DynamicGridPoint dgp = starsFromImage.get(i);
				double x = dgp.getX();
				double y = dgp.getY();
				rp.unproject(x, y, tmp);
				srcX[i] = tmp[0];
				srcY[i] = tmp[1];
			}

			// FIXME : introduire un paramètre "distance maxi en pixel"
			// FIXME : sur quelle base on évalue les pixels ici ? l'image
			PointMatchAlgorithm pma = new PointMatchAlgorithm(refGrid, srcX, srcY, 10.0 / refToImageRatio);
			List<PointMatchAlgorithm.Correlation> correlation = pma.proceed();
			
			if (correlation.size() < 4) continue;
			
			Collections.sort(correlation, new Comparator<PointMatchAlgorithm.Correlation>() {
				@Override
				public int compare(PointMatchAlgorithm.Correlation o1, PointMatchAlgorithm.Correlation o2) {
					if (o1.getP1() == o2.getP1()) {
						return o1.getP2() - o2.getP2();
					} else {
						return o1.getP1() - o2.getP1();
					}
				}
			});

			CorrelationStatus status = correlations.get(correlation);
			if (status != null) {
				// Ajouter simplement à celui-ci
				status.count++;
			} else {
				status = new CorrelationStatus();
				status.correlation = correlation;
				status.cs = rp.cs;
				status.sn = rp.sn;
				status.tx = rp.tx;
				status.ty = rp.ty;
				status.count = 1;
				correlations.put(correlation, status);
			}
		}

		logger.info("Found " + correlations.values().size() + " different correlations");
		// Trouver la meilleure correlation
		CorrelationStatus best = null;
		CorrelationStatus plebBest = null;
		int maxPleb = 0;
		for(CorrelationStatus cs : correlations.values())
		{
			if (cs.count > maxPleb) {
				maxPleb = cs.count;
				plebBest = cs;
			}
			if (best == null || cs.correlation.size() > best.correlation.size())
//			if (best == null || cs.count > best.count)
			{
				best = cs;
			} else if (best != null && cs.count == best.count && cs.correlation.size() > best.correlation.size())
			{
				best = cs;
			}
		}
		
		logger.info("Biggest attractor = " + maxPleb);
		if (best == null) {
			found = false;
		} else {
			found = true;
			logger.info("Found best translation with " + best.correlation.size() + " star correlations (found by " + best.count + " ransac points)");
			this.tx = best.tx;
			this.ty = best.ty;
			this.cs = best.cs;
			this.sn = best.sn;

		}
		
//		
//		logger.info("Performing RANSAC with " + ransacPoints.size());
//		
//		CorrelationAlgo ransac = new Ransac();
//		
//		ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
//			@Override
//			public double getEvaluator(Ransac.RansacPoint p) {
//				return Math.sqrt(p.getRansacParameter(2) * p.getRansacParameter(2) + p.getRansacParameter(3) * p.getRansacParameter(3));
//			}
//		});
//
//		
//		ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
//			@Override
//			public double getEvaluator(Ransac.RansacPoint p) {
//				return Math.sqrt(p.getRansacParameter(0) * p.getRansacParameter(0) + p.getRansacParameter(1) * p.getRansacParameter(1));
//			}
//		});
//		
//		
////		double [] bestParameter = ransac.proceed(ransacPoints, 4, 
////				new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
////				0.1, 0.5);
//		double [] bestParameter = ransac.proceed(ransacPoints, 4, 
//				new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
//				1 / Math.sqrt(ransacPoints.size()), 0.1);
//		
//		if (bestParameter == null) {
//			throw new RuntimeException("Pas de corrélation trouvée");
//		}
//		
//		
//		logger.debug("Transformation is : translate:" + bestParameter[0]+"," + bestParameter[1]+
//				" rotate=" + 180 * Math.atan2(bestParameter[3], bestParameter[2])/Math.PI +
//				" scale=" + Math.sqrt(bestParameter[2] * bestParameter[2] + bestParameter[3] * bestParameter[3]));
//		this.tx = bestParameter[0];
//		this.ty = bestParameter[1];
//		this.cs = bestParameter[2];
//		this.sn = bestParameter[3];
//		
//		found = true;
	}

	private List<RansacPoint> getRansacPoints(
			List<Triangle> imageTriangle,
			DynamicGrid<Triangle> referenceTriangleGrid, 
			int maxCount, double distanceMax,
			double sizeRatioMin, double sizeRatioMax)
	{
		List<RansacPoint> ransacPoints = new ArrayList<RansacPoint>();
		double [] cssn1 = new double[3];
		double [] cssn2 = new double[3];
		double [] cssn3 = new double[3];
		
		for(Triangle t : imageTriangle)
		{
			// FIXME : ce radius devrait être vajusté en fonction du nombre de triangle, pour sortir suffisement de candidat
			// En même temps, le matching est absolu (on compare la précision des triangles)
			List<Triangle> correspondant = referenceTriangleGrid.getNearObject(t.x, t.y, distanceMax);
			
			
			int id = 1;
			for(Triangle c : correspondant)
			{
				c.calcRotation(t, 1, 2, cssn1);
				c.calcRotation(t, 1, 3, cssn2);
				c.calcRotation(t, 2, 3, cssn3);
				
				// Faire la moyenne des cos/sin, les rendre normé
				// Faire la moyenne des ratios.
				double cs = cssn1[0] + cssn2[0] + cssn3[0];
				double sn = cssn1[1] + cssn2[1] + cssn3[1];
				
				double angle = 180 * Math.atan2(sn, cs) / Math.PI;
				
				double div = Math.sqrt(cs * cs + sn * sn);
				// Ici, on s'attend à avoir une norme trés proche de 3 (atteignable uniquement si les 3 rotations sont identiques)
				if (div < 2.9 || div > 3.1) continue;
				div = 1.0 / div;
				cs *= div;
				sn *= div;
				
				
				
				double ratio = (cssn1[2] + cssn2[2] + cssn3[2]) / 3;
				cs *= ratio;
				sn *= ratio;
				
				double dlt = (cssn1[2] - ratio) * (cssn1[2] - ratio)  + (cssn2[2] - ratio) * (cssn2[2] - ratio) + + (cssn3[2] - ratio) * (cssn3[2] - ratio);
				
				if (dlt > 0.001) {
					// si les cos/sin ne sont pas orthogonaux, la translation déforme....
					continue;
				}
				
				// On  met un filtre sur le grossissement également
				if (ratio < sizeRatioMin || ratio > sizeRatioMax) continue;
				
				double tx = 0, ty = 0;
				
				// Calcul de la translation
				for(int i = 1; i <= 3; ++i)
				{
					double xRef = t.getPointX(i);
					double yRef = t.getPointY(i);
					
					double xRotateScale = xRef * cs + yRef * sn; 
					double yRotateScale = yRef * cs - xRef * sn; 
					
					double dltx = c.getPointX(i) - xRotateScale;
					double dlty = c.getPointY(i) - yRotateScale;
					
					tx += dltx;
					ty += dlty;
				}
				
				tx /= 3;
				ty /= 3;
				
//				// Rotation
//				// On peut connaitre facilement le delta en divisant les distances
//				double ratio = (c.dst12 / t.dst12 + c.dst13 / t.dst13 + c.dst23 / t.dst23) / 3;
//				
//				// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
//				double Xa = c.s3.x - c.s2.x;
//				double Ya = c.s3.y - c.s2.y;
//				double Xb = t.s3.x - t.s2.x;
//				double Yb = t.s3.y - t.s2.y;
//				
//				double cos = (Xa*Xb+Ya*Yb)/(c.dst23*t.dst23);
//				double sin = (Xa*Yb-Ya*Xb)/(c.dst23*t.dst23);
//				
//				
//				
//				double tx = (c.s1.x + c.s2.x + c.s3.x - t.s1.x- t.s2.x- t.s3.x) / 3;
//				double ty = (c.s1.y - t.s1.y + c.s2.y - t.s2.y + c.s3.y - t.s3.y) / 3;
//			
				double delta = Math.sqrt((t.x - c.x)*(t.x - c.x) + (t.y - c.y) * (t.y - c.y));
				
				
				RansacPoint rp = new RansacPoint();
				rp.image = t;
				rp.original = c;
				rp.tx = tx;
				rp.ty = ty;
				rp.cs = cs;
				rp.sn = sn;
				
				ransacPoints.add(rp);
				
				// logger.debug("Found possible translation (" + id+" ) " + tx +" - " + ty + " scale=" + ratio + ", angle="+angle+" with delta=" + delta);
				
				if (ransacPoints.size() > maxCount) {
					logger.warn("Too many translation founds. Retry with stricter filter");
					return null;
				}
				
				id++;
			}
		}
		return ransacPoints;
	}
	
    public static boolean pntInTriangle(double px, double py, double x1, double y1, double x2, double y2, double x3, double y3) {
	
	    double o1 = getOrientationResult(x1, y1, x2, y2, px, py);
	    double o2 = getOrientationResult(x2, y2, x3, y3, px, py);
	    double o3 = getOrientationResult(x3, y3, x1, y1, px, py);
	
	    return (o1 == o2) && (o2 == o3);
	}
	
	private static int getOrientationResult(double x1, double y1, double x2, double y2, double px, double py) {
	    double orientation = ((x2 - x1) * (py - y1)) - ((px - x1) * (y2 - y1));
	    if (orientation > 0) {
	        return 1;
	    }
	    else if (orientation < 0) {
	        return -1;
	    }
	    else {
	        return 0;
	    }
	}
	
	static int filtered = 0;
	static int retained = 0;

	/**
	 * @param referenceStars étoiles, triées par ordre de luminosité décroissante
	 * @param minTriangleSize
	 * @param triangleSearchRadius
	 * @param maxTriangle
	 * @param maxRatio : rapport de luminosité max entre la plus faible et la plus lumineuse étoile d'un triangle
	 * @param geoRatio : filtre (de 0 à 1) sur la forme des triangle. 1 = uniquement des equilateral, 0 = n'importe
	 * @return
	 */
	private List<Triangle> getTriangleList(List<? extends DynamicGridPointWithAdu> referenceStars, double minTriangleSize, double triangleSearchRadius, int maxTriangle, double maxRatio, double geoRatio)
	{
		// Trouver les points à moins de 50 pixels
		DynamicGrid<DynamicGridPointWithAdu> reference = new DynamicGrid<DynamicGridPointWithAdu>((List<DynamicGridPointWithAdu>)referenceStars);
		List<Triangle> result = new ArrayList<Triangle>();
		
		double [] i_d = new double[3];
		double [] r_d = new double[3];

		minTriangleSize = minTriangleSize * minTriangleSize;
		double maxTriangleSize2 = triangleSearchRadius * triangleSearchRadius;
		
		logger.info("Searching for triangles in " + referenceStars.size() + " stars - minsize=" + Math.sqrt(minTriangleSize) + ", maxsize=" + triangleSearchRadius + ", brightness ratio=" + maxRatio + ", geo ratio=" + geoRatio);
		for(DynamicGridPointWithAdu rst1 : referenceStars)
		{
			List<DynamicGridPointWithAdu> referencePeerList = reference.getNearObject(rst1.getX(), rst1.getY(), triangleSearchRadius);
			
			for(int a = 0; a < referencePeerList.size(); ++a)
			{
				DynamicGridPointWithAdu rst2 = referencePeerList.get(a);
				if (rst2 == rst1) continue;
				if (compareTo(rst1, rst2) >= 0) continue;
			
				double r_d1 = d2(rst1, rst2);
				// if (r_d1 < minTriangleSize) continue;
				if (r_d1 > maxTriangleSize2) continue;
				
				double minAdu = rst1.getAduLevel();
				double maxAdu = rst2.getAduLevel();
				
				if (minAdu > maxAdu) {
					double tmp = minAdu;
					minAdu = maxAdu;
					maxAdu = tmp;
				}
				if (maxAdu > minAdu * maxRatio) continue;
				
			PeerLoop:
				for(int b = a + 1; b < referencePeerList.size(); ++b)
				{
					DynamicGridPointWithAdu rst3 = referencePeerList.get(b);
					if (rst3 == rst1) continue;
					if (compareTo(rst2, rst3) >= 0) continue;
				
					double newAdu = rst3.getAduLevel();
					if (newAdu < minAdu && newAdu * maxRatio < maxAdu) {
						// Trop petit
						continue;
					}
					if (newAdu > maxAdu && minAdu * maxRatio < newAdu) {
						// Trop gran
						continue;
					}
					
					r_d[0] = r_d1;
					r_d[1] = d2(rst1, rst3);
					r_d[2] = d2(rst2, rst3);

					// if (r_d[0] < minTriangleSize || r_d[1] < minTriangleSize || r_d[2] < minTriangleSize) continue;
					if (r_d[0] > maxTriangleSize2 || r_d[1] > maxTriangleSize2 || r_d[2] > maxTriangleSize2) continue;
					double maxSize = Math.max(Math.max(r_d[0], r_d[1]), r_d[2]);
					if (maxSize < minTriangleSize) continue;
					
					Triangle t = new Triangle(rst1, rst2, rst3, r_d[0], r_d[1], r_d[2]);
					// Exclure les triangles trop symétriques
					if (((filtered + retained) & 0xfff) == 0)
					{
					//	System.out.println("filtered : " + filtered + "  retained : " + retained);
					}
					
					if (t.getX() > 0.90 || t.getY() > 0.9) {
						filtered ++;
						continue;
					} else {
						retained++;
					}
					
					// Avec ces critère, le geoRation maxi est 0.8 * 0.6
					if (geoRatio > 0 && t.getX() * t.getY() < geoRatio * 0.9 * 0.9) {
						continue;
					}
					
					// Vérifier que l'on n'ait pas d'étoile plus lumineuse à l'interieur du triangle
					double aduMinOfTriangle = rst1.getAduLevel();
					aduMinOfTriangle = Math.min(aduMinOfTriangle, rst2.getAduLevel());
					aduMinOfTriangle = Math.min(aduMinOfTriangle, rst3.getAduLevel());
					
					double triangle_x0,triangle_y0,triangle_x1, triangle_y1;
					triangle_x0 = Math.min(rst1.getX(), Math.min(rst2.getX(), rst3.getX()));
					triangle_y0 = Math.min(rst1.getY(), Math.min(rst2.getY(), rst3.getY()));
					triangle_x1 = Math.max(rst1.getX(), Math.max(rst2.getX(), rst3.getX()));
					triangle_y1 = Math.max(rst1.getY(), Math.max(rst2.getY(), rst3.getY()));
					
//					for(DynamicGridPointWithAdu obstacle : referenceStars)
//					{
//						if (obstacle.getAduLevel() < aduMinOfTriangle) break;
//						if (obstacle.getX() < triangle_x0) continue;
//						if (obstacle.getY() < triangle_y0) continue;
//						if (obstacle.getX() > triangle_x1) continue;
//						if (obstacle.getY() > triangle_y1) continue;
//						
//						if (obstacle == rst1) continue;
//						if (obstacle == rst2) continue;
//						if (obstacle == rst3) continue;
//						
//						if (pntInTriangle(
//								obstacle.getX(), obstacle.getY(),
//								rst1.getX(), rst1.getY(),
//								rst2.getX(), rst2.getY(),
//								rst3.getX(), rst3.getY())) {
//					
//							continue PeerLoop;
//						}
//								
//								
//					}
					if ((result.size() & 0xffff) == 0) {
						logger.info("searching... now at " + result.size() + " triangles");
					}
					result.add(t);
					if (result.size() >  maxTriangle) {
						logger.warn("Found too many triangles... retry with stricter filter");
						return null;
					}
//					if (logger.isDebugEnabled()) {
//						logger.debug("Found triangle : " + Math.sqrt(r_d[0]) + " - " + Math.sqrt(r_d[1]) + " - " + Math.sqrt(r_d[2]));
//					}
				}
			}
		}
		
		logger.info("Found " + result.size());
		
		return result;
	}

	// Permet d'avoir un ordre arbitraire sur les points pour éviter de traiter tous les triangles en x3.
	private static int compareTo(DynamicGridPoint a, DynamicGridPoint b)
	{
		double ax, bx;
		ax = a.getX();
		bx = b.getX();
		if (ax <  bx) {
			return 1;
		}
		if (ax > bx) {
			return -1;
		}
		double ay, by;
		ay = a.getY();
		by = b.getY();
		if (ay <  by) {
			return 1;
		}
		if (ay > by) {
			return -1;
		}
		
		return 0;
	}

	private static class Triangle implements DynamicGridPoint
	{
		// Plus grande distance : s2-s3
		// Plus petite distance : s1-s2
		DynamicGridPoint s1, s2, s3;
		
		double dst12, dst13, dst23;
		
		// Les rapports : moyen / grand et petit/moyen
		double x, y;
		
		Triangle(DynamicGridPoint s1, DynamicGridPoint s2, DynamicGridPoint s3, double dst12, double dst13, double dst23)
		{
			this.s1 = s1;
			this.s2 = s2;
			this.s3 = s3;
			
			if (dst12 >= dst13 && dst12 >= dst23)
			{
				// Le plus grand segment est 1-2. On echange 1 et 3
				DynamicGridPoint tmpStar = this.s1;
				this.s1 = this.s3;
				this.s3 = tmpStar;
				
				// Echange dst12 avec dst23. dst13 est inchangé
				double tmp = dst12;
				dst12 = dst23;
				dst23 = tmp;
			} else if (dst13 >= dst12 && dst13 >= dst23) {
				// Le plus grand segment est 1-3. On echange 1 et 2.
				DynamicGridPoint tmpStar = this.s1;
				this.s1 = this.s2;
				this.s2 = tmpStar;
				
				// Echange dst13 avec dst23. dst12 est inchangé
				double tmp = dst13;
				dst13 = dst23;
				dst23 = tmp;
			}
			
			//dst23 est maintenant le plus grand
			if (dst23 < dst12 || dst23 < dst13) {
				throw new RuntimeException("Ca marche pas ton truc !");
			}
				
			// On veut maintenant que le plus petit soit dst12.
			if (dst12 > dst13) {
				// Echanger 2 et 3. dst23 reste inchangé
				DynamicGridPoint tmpStar = this.s2;
				this.s2 = this.s3;
				this.s3 = tmpStar;
				
				double tmp = dst13;
				dst13 = dst12;
				dst12 = tmp;
			}
			

			//dst12 est maintenant le plus petit
			if (dst12 > dst23 || dst12 > dst13 || dst13 > dst23) {
				throw new RuntimeException("Ca marche pas ton truc ! (2)");
			}
			
			this.dst12 = Math.sqrt(dst12);
			this.dst13 = Math.sqrt(dst13);
			this.dst23 = Math.sqrt(dst23);
			
			x = this.dst13 / this.dst23;
			y = this.dst12 / this.dst13;
			
		}
		
		@Override
		public double getX() {
			return x;
		}
		
		@Override
		public double getY() {
			return y;
		}
		
		private double getPointX(int p)
		{
			switch(p - 1) {
			case 0:
				return s1.getX();
			case 1:
				return s2.getX();
			case 2:
				return s3.getX();
			}
			return 0;
		}
		
		private double getPointY(int p)
		{
			switch(p - 1) {
			case 0:
				return s1.getY();
			case 1:
				return s2.getY();
			case 2:
				return s3.getY();
			}
			return 0;			
		}
		
		private double getDistance(int p1, int p2)
		{
			if (p1 > p2) {
				int tmp = p1;
				p1 = p2;
				p2 = tmp;
			}
			if (p1 == 1) {
				if (p2 == 2) {
					return dst12;
				}
				return dst13;
			}
			return dst23;
		}
		
		// Fourni cos et sin et ratio pour passer de this à other
		public double [] calcRotation(Triangle other, int p1, int p2, double [] calc)
		{
			if (calc == null || calc.length != 3) {
				calc = new double[3];
			}
			
			// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
			double Xa = other.getPointX(p2) - other.getPointX(p1);
			double Ya = other.getPointY(p2) - other.getPointY(p1);
			double Xb = this.getPointX(p2) - this.getPointX(p1);
			double Yb = this.getPointY(p2) - this.getPointY(p1);
			
			calc[0] = (Xa*Xb+Ya*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
			calc[1] = (Ya*Xb-Xa*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
			calc[2] = this.getDistance(p1, p2) / other.getDistance(p1, p2);
			
			// On doit avoir :
			
			double XbVerif = (Xa * calc[0] + Ya * calc[1]) * calc[2];
			double YbVerif = (Ya * calc[0] - Xa * calc[1]) * calc[2];
			
			return calc;
		}
	}

	private static class RansacPoint implements Ransac.RansacPoint{
		Triangle original;
		Triangle image;
		
		double tx, ty, cs, sn;
		
		@Override
		public String toString() {
			double angle = Math.atan2(cs, sn) * 180 / Math.PI;
			double scale = Math.sqrt(cs * cs + sn * sn);
			return String.format("translation=(%.2f,%.2f) rotation=%.2f scale=%.3f", tx, ty, angle, scale); 
		}
		
		public void project(double x, double y, double [] result)
		{
			result[0] = this.tx + x * this.cs + y * this.sn;
			result[1] = this.ty + y * this.cs - x * this.sn;
		}
		
		public void unproject(double nvx, double nvy, double [] result)
		{
			result[0] = ((-1.0)*((cs*(tx - nvx)) + (sn*(nvy - ty)))/((sn*sn) + (cs*cs)));
			result[1] = (((sn*(nvx - tx)) + (cs*(nvy - ty)))/((sn*sn) + (cs*cs)));
		}
		
		@Override
		public double getRansacParameter(int order) {
			switch(order) {
			case 0:
				return tx;
			case 1:
				return ty;
			case 2:
				return cs;
			case 3:
				return sn;
			default:
				return 0;
			}
		}
		
	}

	private static double d2(DynamicGridPoint d1, DynamicGridPoint d2)
	{
		return (d1.getX() - d2.getX()) * (d1.getX() - d2.getX()) + (d1.getY() - d2.getY()) * (d1.getY() - d2.getY());
	}

	public double getTx() {
		return tx;
	}

	public double getTy() {
		return ty;
	}

	public double getCs() {
		return cs;
	}

	public double getSn() {
		return sn;
	}

	public boolean isFound() {
		return found;
	}

}
