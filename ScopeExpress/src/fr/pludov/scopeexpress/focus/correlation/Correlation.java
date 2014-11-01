//package fr.pludov.scopeexpress.focus.correlation;
//
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.IdentityHashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//import javax.imageio.IIOImage;
//import javax.imageio.ImageIO;
//import javax.imageio.ImageWriteParam;
//import javax.imageio.ImageWriter;
//import javax.imageio.stream.ImageOutputStream;
//
//import org.apache.log4j.Logger;
//
//import fr.pludov.scopeexpress.focus.AffineTransform3D;
//import fr.pludov.scopeexpress.focus.SkyProjection;
//import fr.pludov.scopeexpress.utils.DynamicGrid;
//import fr.pludov.scopeexpress.utils.DynamicGridPoint;
//import fr.pludov.scopeexpress.utils.DynamicGridPointWithAdu;
//import fr.pludov.scopeexpress.utils.PointMatchAlgorithm;
//import fr.pludov.scopeexpress.utils.Ransac;
//import fr.pludov.scopeexpress.utils.PointMatchAlgorithm.DynamicGridPointWithIndex;
//
///**
// * Correlation est une collection d'images corrélée
// * 
// * Chaque image a un status et une position relative
// * 
// * Une liste des étoiles identifiées est gardée
// * 
// * 
// * @author Ludovic POLLET
// *
// */
//public class Correlation {
//	private static final Logger logger = Logger.getLogger(Correlation.class);
//	
//	SkyProjection skyProjection;
//	boolean found;
//	
//	public Correlation()
//	{
//		this.found = false;
//	}
//	
//	private static class CorrelationStatus
//	{
//		List<PointMatchAlgorithm.Correlation> correlation;
//		RansacPoint rp;
//		
//		int count;
//		double dstsum;
//		
//		// Quaternions
//		double qx, qy, qz, qw, qsum;
//		
//		SkyProjection skyProjection;
//	}
//
//	private static int incChannel(int ch, double value)
//	{
//		ch = 255 - ch;
//		ch *= value;
//		ch = 255 - ch;
//		return ch;
//	}
//	
//	private static int incRgb(int rgb)
//	{
//		int r = rgb & 255;
//		int g = (rgb >> 8) & 255;
//		int b = (rgb >> 16) & 255;
//		
//		r = incChannel(rgb, 0.5);
//		g = incChannel(rgb, 0.25);
//		b = incChannel(rgb, 0.125);
//		
//		rgb = r | (g << 8) | (b << 16);
//		return rgb;
//	}
//	
//	private static void saveDGP(List<? extends DynamicGridPoint> triangles, File pathName)
//	{
//		BufferedImage bi;
//		int maxWidth = 512;
//		bi = new BufferedImage(maxWidth, maxWidth, BufferedImage.TYPE_INT_RGB);
//		
//		double x0, y0, x1, y1;
//		x0 = 0;
//		y0 = 0;
//		x1 = 1;
//		y1 = 1;
//	
//		if (!triangles.isEmpty()) {
//			
//			for(DynamicGridPoint point : triangles)
//			{
//				double x = (double)point.getX();
//				double y = (double)point.getY();
//				if (x > x1) x1 = x;
//				if (y > y1) y1 = y;
//				if (x < x0) x0 = x;
//				if (y < y0) y0 = y;
//			}
//			
//			x0 -= 0.01;
//			y0 -= 0.01;
//			x1 += 0.01;
//			y1 += 0.01;
//			
//			double scale = maxWidth * 1.0 / Math.max(x1 - x0, y1 - y0);
//			for(DynamicGridPoint point : triangles)
//			{
//				int scx = (int)Math.round((point.getX() - x0) * scale);
//				int scy = (int)Math.round((point.getY() - y0) * scale);
//				
//				
//				int rgb = bi.getRGB(scx, scy);
//				rgb = incRgb(rgb);
//				bi.setRGB(scx, scy, rgb);
//			}
//			
//		}
//		try {
//			ImageIO.write(bi, "bmp", pathName);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	/**
//	 * Enregistre une image de debug présentant les étoiles et les triangles
//	 */
//	private static void saveTriangles(List<? extends DynamicGridPointWithAdu> stars, List<Triangle> triangles, File pathName)
//	{
//		BufferedImage bi;
//		int maxWidth = 2048;
//		bi = new BufferedImage(maxWidth, maxWidth, BufferedImage.TYPE_INT_RGB);
//		logger.debug("drawing " + stars.size() + " stars, " + triangles.size() + " triangles");
//		if (!stars.isEmpty()) {
//			int x0, y0, x1, y1;
//			double maxAdu;
//			x0 = (int)stars.get(0).getX();
//			y0 = (int)stars.get(0).getY();
//			maxAdu = stars.get(0).getAduLevel();
//			x1 = x0;
//			y1 = y0;
//			
//			for(DynamicGridPointWithAdu point : stars)
//			{
//				int x = (int)point.getX();
//				int y = (int)point.getY();
//				if (x > x1) x1 = x;
//				if (y > y1) y1 = y;
//				if (x < x0) x0 = x;
//				if (y < y0) y0 = y;
//				if (point.getAduLevel() > maxAdu) maxAdu = point.getAduLevel();
//			}
//			
//			
//			double scale = maxWidth * 1.0 / Math.max(x1 - x0, y1 - y0);
//			
//			Graphics2D g2d = bi.createGraphics();
//			g2d.setColor(Color.white);
//			for(DynamicGridPointWithAdu point : stars)
//			{
//				double scx = (point.getX() - x0) * scale;
//				double scy = (point.getY() - y0) * scale;
//				
//				double mag = 6 - (Math.log(maxAdu) - Math.log(point.getAduLevel()));
//				if (mag < 1) mag = 1;
//				g2d.drawOval((int)Math.round(scx - mag), (int)Math.round(scy - mag), (int)Math.round(2 * mag + 1), (int)Math.round(2 * mag + 1));
//			}
//			
//			int cid = 0;
//			Color [] colors = {Color.red, Color.blue, Color.green, Color.cyan, Color.magenta, Color.orange, Color.pink, Color.yellow}; 
//			for(Triangle t : triangles)
//			{
//				g2d.setColor(colors[cid]);
//				cid++;
//				if (cid == colors.length) cid = 0;
//				
//				DynamicGridPoint point = t.s1;
//				double scx0 = (point.getX() - x0) * scale;
//				double scy0 = (point.getY() - y0) * scale;
//				
//				point = t.s2;
//				double scx1 = (point.getX() - x0) * scale;
//				double scy1 = (point.getY() - y0) * scale;
//				
//				point = t.s3;
//				double scx2 = (point.getX() - x0) * scale;
//				double scy2 = (point.getY() - y0) * scale;
//				
//				g2d.drawLine((int)Math.round(scx0), (int)Math.round(scy0), (int)Math.round(scx1), (int)Math.round(scy1));
//				g2d.drawLine((int)Math.round(scx0), (int)Math.round(scy0), (int)Math.round(scx2), (int)Math.round(scy2));
//				g2d.drawLine((int)Math.round(scx2), (int)Math.round(scy2), (int)Math.round(scx1), (int)Math.round(scy1));
//			
//			}
//			
//		}
//		logger.debug("saving stars");
//		try {
//			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("BMP");
//			
//			if (!writers.hasNext()) {
//				throw new Exception("bmp not supported");
//			}
//			ImageWriter iw = writers.next();
//			try {
//				ImageWriteParam params = iw.getDefaultWriteParam();
//				ImageOutputStream stream = ImageIO.createImageOutputStream(pathName);
//				try {
//					iw.setOutput(stream);
//					
//					iw.write(null, new IIOImage(bi, null, null), params);
//				} finally {
//					stream.close();
//				}
//			} finally {
//				iw.dispose();
//			}
//			
////			ImageIO.write(bi, "png", pathName);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void identity()
//	{
//		// logger.info("Found best translation with " + best.correlation.size() + " star correlations (found by " + best.count + " ransac points)");
//		found = true;
//		this.skyProjection = null;
//	}
//
//	/**
//	 * Trouve la distance qui permet de garder au moins 90% des étoiles de starsFromImage
//	 * dmax sert à limiter la recherche
//	 */
//	public double getOtpimalRay(
//							List<? extends DynamicGridPointWithAdu> starsFromImage,
//							DynamicGrid<? extends DynamicGridPointWithAdu> starGrid,
//							double dmax,
//							int keepCount,
//							double brightestStarRatio)
//	{
//		// Distance mini au carré
//		double [] d2min = new double[starsFromImage.size()];
//		int i = 0;
//		for(DynamicGridPointWithAdu star : starsFromImage)
//		{
//			double mind2 = dmax * dmax;
//			double minAduLevel = star.getAduLevel() * brightestStarRatio;
//			for(DynamicGridPointWithAdu otherStar : starGrid.getNearObject(star.getX(), star.getY(), dmax))
//			{
//				if (otherStar.getAduLevel() <= minAduLevel) {
//					continue;
//				}
//				if (otherStar == star) {
//					continue;
//				}
//				
//				double d2 = (star.getX() - otherStar.getX()) * (star.getX() - otherStar.getX())
//							+ (star.getY() - otherStar.getY()) * (star.getY() - otherStar.getY());
//				if (d2 < mind2) {
//					mind2 = d2;
//				}
//			}
//			d2min[i++] = mind2;
//		}
//		
//		Arrays.sort(d2min);
//		int idToTake;
//		
//		if (keepCount >= d2min.length) {
//			idToTake = 0;
//		} else {
//			idToTake = d2min.length - keepCount - 1;
//		}
//		return Math.sqrt(d2min[idToTake]);
//	}
//	
//	/**
//	 * Retire de la liste toutes les étoiles qui ont un voisin plus brillant dans le rayon indiqué
//	 * Un voisin est considéré brillant si voisin.adu > this.adu*brightestStarRatio
//	 * 
//	 * un ratio > 1 permet d'ignorer les voisin légèrement plus brillants
//	 *  
//	 * @param starsFromImage
//	 * @param rayWithNoBrighterStar
//	 * @param brightestStarRatio
//	 * @return
//	 */
//	public ArrayList<DynamicGridPointWithAdu> filterStarWithBrightNeighboors(
//							List<? extends DynamicGridPointWithAdu> stars, 
//							DynamicGrid<? extends DynamicGridPointWithAdu> starGrid,
//							double rayWithNoBrighterStar, double brightestStarRatio)
//	{
//		ArrayList<DynamicGridPointWithAdu> result = new ArrayList<DynamicGridPointWithAdu>(stars.size());
//		if (starGrid == null) {
//			starGrid = new DynamicGrid<DynamicGridPointWithAdu>((List<DynamicGridPointWithAdu>)stars);
//		}
//		
//	StarLoop:
//		for(DynamicGridPointWithAdu star : stars)
//		{
//			double minAduLevel = star.getAduLevel() * brightestStarRatio;
//			for(DynamicGridPointWithAdu otherStar : starGrid.getNearObject(star.getX(), star.getY(), rayWithNoBrighterStar))
//			{
//				if (otherStar.getAduLevel() <= minAduLevel) {
//					continue;
//				}
//				if (otherStar == star) {
//					continue;
//				}
//				continue StarLoop;
//			}
//			
//			result.add(star);
//		}
//		return result;
//	}
//	
//	public void correlate(
//			List<? extends DynamicGridPointWithAdu> starsFromImage, SkyProjection imageAffineSkyProjection,
//			List<? extends DynamicGridPointWithAdu> starsFromRef, SkyProjection refSkyProjection,
//			boolean refIsExhaustive)
//	{
//		int maxTriangle = 900; // FIXME : c'est ad hoc...
//		
//		// Controle les paramètres liés à la taille (valable pour une image 8MP) 
//		double imageScaleRatio = 1.0;
//		
//		double starRay = refIsExhaustive ? 700 * imageScaleRatio : 3000 * imageScaleRatio; 	// Prendre en compte des triangles de au plus cette taille
//		double starMinRay = (refIsExhaustive ? 600 * imageScaleRatio : 300 * imageScaleRatio);	// Elimine les petits triangles
//		double maxBrightnessRatio = 5.5;	// On considère des triplet d'étoiles avec ce rapport maxi d'éclat 
//		double minGeoRatio = 0.25;	// Facteur par rapport au triangle "8-9-10" (à 1 on ne retiendra que lui) 
//		
//		// FIXME: pas sûr du sens
//		double refToImageRatio = refSkyProjection.getPixelRad() / imageAffineSkyProjection.getPixelRad();
//		
//		
//		List<Triangle> trianglesFromImage;
//		
//		List<Triangle> trianglesFromRef;
//		
//		if (refIsExhaustive || starsFromRef.size() > 200) {
//			
//			// Exclu les étoiles qui ont une voisine au moins x fois plus lumineuse
//			double brightestStarRatio = 1.5;
//	
//			// FIXME: c'est en dûr ! Ce paramètre sert de born min à la distance (dans le cas où il y a trés peu d'image) 
//			double dmax = 600 * imageScaleRatio;
//			
//			int keepCount, allowRemoval;
//			if (starsFromImage.size() < 40) {
//				allowRemoval = 5;
//			} else {
//				allowRemoval = starsFromImage.size() / 8;
//			}
//			keepCount = starsFromImage.size() - allowRemoval;
//			if (keepCount > 200) {
//				keepCount = 200;
//			}
//			
//			DynamicGrid<DynamicGridPointWithAdu> starGrid = new DynamicGrid<DynamicGridPointWithAdu>((List<DynamicGridPointWithAdu>)starsFromImage);
//			double rayWithNoBrighterStar = getOtpimalRay(starsFromImage, starGrid, dmax, keepCount, brightestStarRatio);
//			
//			
//			logger.info("Restricting to stars that has no brighter star within " + rayWithNoBrighterStar + " pixels in image");
//			int orginalStarFromImageCount = starsFromImage.size();
//			starsFromImage = filterStarWithBrightNeighboors(starsFromImage, starGrid, rayWithNoBrighterStar, brightestStarRatio);
//			logger.info("Star from image filtered from " + orginalStarFromImageCount + " down to " + starsFromImage.size() + " stars");
//			int orginalStarFromRefCount = starsFromRef.size();
//			starsFromRef = filterStarWithBrightNeighboors(starsFromRef, null, rayWithNoBrighterStar / refToImageRatio, brightestStarRatio);
//			logger.info("Star from reference filtered from " + orginalStarFromRefCount + " down to " + starsFromRef.size() + " stars");
//		}
//		
//		Collections.sort(starsFromImage, new Comparator<DynamicGridPointWithAdu>() {
//			@Override
//			public int compare(DynamicGridPointWithAdu o1, DynamicGridPointWithAdu o2) {
//				
//				double thisVal = o1.getAduLevel();
//				double anotherVal = o2.getAduLevel();
//				return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
//			}
//		});
//		
//		Collections.sort(starsFromRef, new Comparator<DynamicGridPointWithAdu>() {
//			@Override
//			public int compare(DynamicGridPointWithAdu o1, DynamicGridPointWithAdu o2) {
//				
//				double thisVal = o1.getAduLevel();
//				double anotherVal = o2.getAduLevel();
//				return (thisVal<anotherVal ? 1 : (thisVal==anotherVal ? 0 : -1));
//			}
//		});
//		
//
//		int lengthToTest = starsFromImage.size();
//		int lengthForRef = starsFromRef.size();
//		do {
//			// FIXME : on voudrait que les triangles ne se recouvrent pas trop !
//			logger.debug("Looking for image triangles, max size = " + starRay + " brightness ratio = " + maxBrightnessRatio);
//			trianglesFromImage = getTriangleList(starsFromImage.subList(0, lengthToTest), imageAffineSkyProjection, starMinRay, starRay, maxTriangle, maxBrightnessRatio, minGeoRatio);
//			if (trianglesFromImage == null) {
//				
////				if (lengthToTest > 10) {
////					logger.warn("Too many triangles found, restrict to brightest stars");
////					lengthToTest = (int)Math.round(0.75 * lengthToTest);
////				} else {
//					logger.warn("Too many triangles found, restrict morphology/brightness");
////					maxBrightnessRatio = 1 + (maxBrightnessRatio - 1) * 0.95;
//					minGeoRatio = Math.pow(minGeoRatio, 0.7);
////				}
//				continue;
//			}
//			if (logger.isDebugEnabled()) {
//				IdentityHashMap<DynamicGridPoint, Boolean> map = new IdentityHashMap<DynamicGridPoint, Boolean>();
//				for(Triangle t : trianglesFromImage) {
//					map.put(t.s1, Boolean.TRUE);
//					map.put(t.s2, Boolean.TRUE);
//					map.put(t.s3, Boolean.TRUE);
//				}
//				logger.debug("Triangle selection covers " + map.size() + "/" + lengthToTest + " stars ");
//			}
//			if (trianglesFromImage.size() == 0) {
//				logger.warn("Not enough triangle in image");
//				found = false;
//				return;
//			}
//			logger.debug("Looking for references triangles, max size = " + starRay);
//			trianglesFromRef = getTriangleList(starsFromRef.subList(0, lengthForRef), refSkyProjection, starMinRay / refToImageRatio, starRay / refToImageRatio, 4000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
//			if (trianglesFromRef == null) {
//				
////				if (lengthForRef > 10) {
////					logger.warn("Too many triangles found, restrict to brightest stars");
////					lengthForRef = (int)Math.round(0.75 * lengthForRef);
////				} else {
//					logger.warn("Too many triangles found, restrict morphology/brightness");
////					maxBrightnessRatio = 1 + (maxBrightnessRatio - 1) * 0.95;
//					minGeoRatio = Math.pow(minGeoRatio, 0.7);
////				}
//				continue;
//			}
//			
//			break;
//		} while(true);
//		
//		if (logger.isDebugEnabled()) {
//			logger.debug("Saving images");
//			saveTriangles(starsFromRef/*.subList(0, lengthForRef)*/, trianglesFromRef, new File("c:\\triangles-ref.bmp"));
//			saveDGP(trianglesFromRef, new File("c:\\triangles-ref-ratio.bmp"));
//			saveTriangles(starsFromImage/*.subList(0, lengthToTest)*/, trianglesFromImage, new File("c:\\triangles-image.bmp"));
//			saveDGP(trianglesFromImage, new File("c:\\triangles-image-ratio.bmp"));
//		}
//		
//		logger.debug("Creating dynamic grid table");
//		DynamicGrid<Triangle> refTriangleGrid = new DynamicGrid<Triangle>(trianglesFromRef);
//		logger.debug("Dynamic grid table created");
//		
//		// Cette tolerance est absolue. Elle a été ajustée pour ne pas rejetter 
//		// d'attracteurs sur une image prise près de la polaire
//		double tolerance = 0.002;
//		int maxNbRansacPoints = 200000;
//		
//		List<RansacPoint> ransacPoints;
//		
//		double sizeRatio = 1.0;
//		if (logger.isDebugEnabled()) {
//			double imageMoy = 0;
//			double refMoy = 0;
//			for(Triangle t : trianglesFromImage) {
//				imageMoy += t.dst12 + t.dst13 + t.dst23;
//			}
//			
//			for(Triangle t : trianglesFromRef) {
//				refMoy +=  t.dst12 + t.dst13 + t.dst23;
//			}
//			logger.debug("Triangle mean size : img:" + (imageMoy / trianglesFromImage.size()) + " ref:" + (refMoy/trianglesFromRef.size()));
//		}
//
//		// Les triangles sont à la même echelle (en radian) => le ratio ideal est 1
//		while((ransacPoints = getRansacPoints(trianglesFromImage, imageAffineSkyProjection, 
//												refTriangleGrid, refSkyProjection,
//												maxNbRansacPoints, tolerance, (sizeRatio) * 0.95, (sizeRatio) * 1.05)) == null)
//		{
//			logger.warn("Too many possible translations. Filter wiht more aggressive values...");
//			tolerance *= 0.6;
//		}
//		
//		logger.info("Found " + ransacPoints.size() + " translations");
//		
//		double [] srcX = new double[starsFromImage.size()];
//		double [] srcY = new double[starsFromImage.size()];
//		
//		
//		List<PointMatchAlgorithm.DynamicGridPointWithIndex> refStarsWithIndex = new ArrayList<PointMatchAlgorithm.DynamicGridPointWithIndex>();
//		for(int i = 0; i < starsFromRef.size(); ++i)
//		{
//			DynamicGridPoint dgp = starsFromRef.get(i);
//			double x = dgp.getX();
//			double y = dgp.getY();
//
//			PointMatchAlgorithm.DynamicGridPointWithIndex refStarWithIndex;
//			refStarWithIndex = new PointMatchAlgorithm.DynamicGridPointWithIndex(x, y, i);
//			refStarsWithIndex.add(refStarWithIndex);
//		}
//		
//		DynamicGrid<PointMatchAlgorithm.DynamicGridPointWithIndex> refGrid = new DynamicGrid<PointMatchAlgorithm.DynamicGridPointWithIndex>(refStarsWithIndex);
//		
//		double [] starPos2d = new double[2];
//		double [] starPos3d = new double[3];
//		
//		Map<List<PointMatchAlgorithm.Correlation>, CorrelationStatus> correlations = new HashMap<List<PointMatchAlgorithm.Correlation>, Correlation.CorrelationStatus>(ransacPoints.size());
//		for(RansacPoint rp : ransacPoints)
//		{
//			double[] tmp = new double[2];
//			for(int i = 0; i < starsFromImage.size(); ++i)
//			{
//				DynamicGridPoint dgp = starsFromImage.get(i);
//				starPos2d[0] = dgp.getX();
//				starPos2d[1] = dgp.getY();
//				rp.projection.image2dToSky3d(starPos2d, starPos3d);
//				refSkyProjection.image3dToImage2d(starPos3d, starPos2d);
//				srcX[i] = starPos2d[0];
//				srcY[i] = starPos2d[1];
//			}
//
//			// FIXME : introduire un paramètre "distance maxi en pixel"
//			// FIXME : sur quelle base on évalue les pixels ici ? l'image
//			PointMatchAlgorithm pma = new PointMatchAlgorithm(refGrid, srcX, srcY, 6.0 / refToImageRatio);
//			List<PointMatchAlgorithm.Correlation> correlation = pma.proceed();
//			
//			if (correlation.size() < 4) continue;
//			
//			Collections.sort(correlation, new Comparator<PointMatchAlgorithm.Correlation>() {
//				@Override
//				public int compare(PointMatchAlgorithm.Correlation o1, PointMatchAlgorithm.Correlation o2) {
//					if (o1.getP1() == o2.getP1()) {
//						return o1.getP2() - o2.getP2();
//					} else {
//						return o1.getP1() - o2.getP1();
//					}
//				}
//			});
//
//			CorrelationStatus status = correlations.get(correlation);
//			if (status != null) {
//				// Ajouter simplement à celui-ci
//				status.count++;
//				double [] quaternion = rp.projection.getTransform().getQuaternion();
//				status.qx += quaternion[0];
//				status.qy += quaternion[1];
//				status.qz += quaternion[2];
//				status.qw += quaternion[3];
//				status.qsum ++;
//				
//				if (status.dstsum > pma.getDstSum()) {
//					status.dstsum = pma.getDstSum();
//					status.skyProjection = rp.projection;
//					status.rp = rp;
//				}
//			} else {
//				status = new CorrelationStatus();
//				status.correlation = correlation;
//				status.rp = rp;
//				double [] quaternion = rp.projection.getTransform().getQuaternion();
//				status.qx += quaternion[0];
//				status.qy += quaternion[1];
//				status.qz += quaternion[2];
//				status.qw += quaternion[3];
//				status.qsum ++;
//				
//				status.skyProjection = rp.projection;
//				status.dstsum = pma.getDstSum();
////				status.cs = rp.cs;
////				status.sn = rp.sn;
////				status.tx = rp.tx;
////				status.ty = rp.ty;
//				status.count = 1;
//				correlations.put(correlation, status);
//			}
//		}
//
//		logger.info("Found " + correlations.values().size() + " different correlations");
//		
//		// Trouver la meilleure correlation
//		CorrelationStatus best = null;
//		CorrelationStatus plebBest = null;
//		int maxPleb = 0;
//		for(CorrelationStatus cs : correlations.values())
//		{
//			if (cs.count > maxPleb) {
//				maxPleb = cs.count;
//				plebBest = cs;
//			}
//			if (best == null || cs.correlation.size() > best.correlation.size())
////			if (best == null || cs.count > best.count)
//			{
//				best = cs;
//			} else if (best != null && cs.count == best.count && cs.correlation.size() > best.correlation.size())
//			{
//				best = cs;
//			}
//		}
//		
//		
//		logger.info("Biggest attractor = " + maxPleb);
//		if (best == null) {
//			found = false;
//		} else {
//			found = true;
//			logger.info("Found best translation with " + best.correlation.size() + " star correlations (found by " + best.count + " ransac points)");
//			logger.info("Matching avg delta: " + ((Math.sqrt(best.dstsum / best.correlation.size()) * refToImageRatio)) + " px" );
//			double [] quaternion = new double[]{best.qx, best.qy, best.qz, best.qw};
//			for(int i = 0; i < 4; ++i) {
//				quaternion[i] /= best.qsum;
//			}
//			double norm = AffineTransform3D.norm(quaternion);
//			for(int i = 0; i < 4; ++i) {
//				quaternion[i] /= norm;
//			}
//			AffineTransform3D fromQuaternion = AffineTransform3D.fromQuaternion(quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
//			this.skyProjection = best.skyProjection;
//			
////			List<CorrelationStatus> correlationsOrderedByMatching = new ArrayList<CorrelationStatus>(correlations.values());
////			
////			Collections.sort(correlationsOrderedByMatching, new Comparator<CorrelationStatus>() {
////				@Override
////				public int compare(CorrelationStatus o1, CorrelationStatus o2) {
////					return o1.correlation.size() - o2.correlation.size();
////				}
////			});
////			// Une fois qu'on la transfo, on pourra essayer de retrouver les triangles de la correlation pour comprendre où ils ont été supprimés
////			
////			int triangleFromRefNotDetectedInImage = 0;
////			int triangleFromImageNotDetectedInRef = 0;
////			int missingRansacPoint = 0;
////			
////			for(int i = 0; i < best.correlation.size(); ++i)
////			{
////				PointMatchAlgorithm.Correlation corrI = best.correlation.get(i);
////				DynamicGridPointWithAdu imagePi = starsFromImage.get(corrI.getP2());
////				DynamicGridPointWithAdu refPi = starsFromRef.get(corrI.getP1());
////				
////				for(int j = i + 1; j < best.correlation.size(); ++j)
////				{
////					PointMatchAlgorithm.Correlation corrJ = best.correlation.get(j);
////					DynamicGridPointWithAdu imagePj = starsFromImage.get(corrJ.getP2());
////					DynamicGridPointWithAdu refPj = starsFromRef.get(corrJ.getP1());
////					
////					for(int k = j + 1; k < best.correlation.size(); ++k)
////					{
////						PointMatchAlgorithm.Correlation corrK = best.correlation.get(k);
////						DynamicGridPointWithAdu imagePk = starsFromImage.get(corrK.getP2());
////						DynamicGridPointWithAdu refPk = starsFromRef.get(corrK.getP1());
////						Triangle imageTriangle = findTriangle(trianglesFromImage, imagePi, imagePj, imagePk);
////						
////						Triangle refTriangle = findTriangle(trianglesFromRef, refPi, refPj, refPk);
////			
////						if (imageTriangle != null || refTriangle != null) {
////							if (imageTriangle == null) {
////								triangleFromRefNotDetectedInImage++;
////								ArrayList<DynamicGridPointWithAdu> miniList;
////								List<Triangle> verifiedTriangleList ;
////								
////								miniList = new ArrayList<DynamicGridPointWithAdu>();
////								miniList.add(imagePi);
////								miniList.add(imagePj);
////								miniList.add(imagePk);
////								verifiedTriangleList = getTriangleList(miniList, starMinRay, starRay, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
////								if (verifiedTriangleList != null && !verifiedTriangleList.isEmpty()) {
////									logger.warn("Triangle is missing but was found");
////									List<Triangle> reverifiedTriangleList;
////									reverifiedTriangleList = getTriangleList(starsFromImage.subList(0, lengthToTest), starMinRay, starRay, maxTriangle, maxBrightnessRatio, minGeoRatio);
////									imageTriangle = findTriangle(trianglesFromImage, imagePi, imagePj, imagePk);
////									if (imageTriangle != null) {
////										logger.warn("Triangle reapparition");
////									}
////								}
////								
//////								miniList = new ArrayList<DynamicGridPointWithAdu>();
//////								miniList.add(imagePi);
//////								miniList.add(imagePk);
//////								miniList.add(imagePj);
//////								verifiedTriangleList = getTriangleList(miniList, starMinRay, starRay, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
//////								if (verifiedTriangleList != null && !verifiedTriangleList.isEmpty()) {
//////									logger.warn("Triangle is missing but was found");
//////								}
//////
//////								miniList = new ArrayList<DynamicGridPointWithAdu>();
//////								miniList.add(imagePj);
//////								miniList.add(imagePi);
//////								miniList.add(imagePk);
//////								verifiedTriangleList = getTriangleList(miniList, starMinRay, starRay, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
//////								if (verifiedTriangleList != null && !verifiedTriangleList.isEmpty()) {
//////									logger.warn("Triangle is missing but was found");
//////								}
//////
//////								miniList = new ArrayList<DynamicGridPointWithAdu>();
//////								miniList.add(imagePj);
//////								miniList.add(imagePk);
//////								miniList.add(imagePi);
//////								verifiedTriangleList = getTriangleList(miniList, starMinRay, starRay, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
//////								if (verifiedTriangleList != null && !verifiedTriangleList.isEmpty()) {
//////									logger.warn("Triangle is missing but was found");
//////								}
//////
//////								miniList = new ArrayList<DynamicGridPointWithAdu>();
//////								miniList.add(imagePk);
//////								miniList.add(imagePi);
//////								miniList.add(imagePj);
//////								verifiedTriangleList = getTriangleList(miniList, starMinRay, starRay, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
//////								if (verifiedTriangleList != null && !verifiedTriangleList.isEmpty()) {
//////									logger.warn("Triangle is missing but was found");
//////								}
//////
//////								miniList = new ArrayList<DynamicGridPointWithAdu>();
//////								miniList.add(imagePk);
//////								miniList.add(imagePj);
//////								miniList.add(imagePi);
//////								verifiedTriangleList = getTriangleList(miniList, starMinRay, starRay, 1000 * maxTriangle, maxBrightnessRatio, minGeoRatio);
//////								if (verifiedTriangleList != null && !verifiedTriangleList.isEmpty()) {
//////									logger.warn("Triangle is missing but was found");
//////								}
////
////							}
////							if (refTriangle == null) {
////								triangleFromImageNotDetectedInRef++;
////							}
////						}
////						
////						if (imageTriangle != null && refTriangle != null) {
////							List<Triangle> miniListFromRef = Collections.singletonList(refTriangle);
////							DynamicGrid<Triangle> miniGridForImage = new DynamicGrid<Triangle>(Collections.singletonList(imageTriangle));
////							
////							List<RansacPoint> miniRansacPoints = getRansacPoints(
////									miniListFromRef, 
////									miniGridForImage, 
////									maxNbRansacPoints, 
////									tolerance, (refToImageRatio) * 0.95, (refToImageRatio) * 1.05);
////							if (miniRansacPoints == null || miniRansacPoints.isEmpty()) {
////								missingRansacPoint++;
////								miniRansacPoints = getRansacPoints(
////										miniListFromRef, 
////										miniGridForImage, 
////										maxNbRansacPoints, 
////										tolerance, (refToImageRatio) * 0.95, (refToImageRatio) * 1.05);
////							}
////						}
////						
////					}
////				}
////			}
////			
////			logger.info("Post analysis result: " + missingRansacPoint + " missing ransac points");
////			logger.info("Post analysis result: " + triangleFromRefNotDetectedInImage + " triangle From Ref NotDetected In Image (possible cause: near of size limit, high variation in relative brightness)");
////			logger.info("Post analysis result: " + triangleFromImageNotDetectedInRef + " triangle From Image Not Detected In Ref (possible cause: near of size limit, high variation in relative brightness)");
//			
//		}
//		
////		
////		logger.info("Performing RANSAC with " + ransacPoints.size());
////		
////		CorrelationAlgo ransac = new Ransac();
////		
////		ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
////			@Override
////			public double getEvaluator(Ransac.RansacPoint p) {
////				return Math.sqrt(p.getRansacParameter(2) * p.getRansacParameter(2) + p.getRansacParameter(3) * p.getRansacParameter(3));
////			}
////		});
////
////		
////		ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
////			@Override
////			public double getEvaluator(Ransac.RansacPoint p) {
////				return Math.sqrt(p.getRansacParameter(0) * p.getRansacParameter(0) + p.getRansacParameter(1) * p.getRansacParameter(1));
////			}
////		});
////		
////		
//////		double [] bestParameter = ransac.proceed(ransacPoints, 4, 
//////				new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
//////				0.1, 0.5);
////		double [] bestParameter = ransac.proceed(ransacPoints, 4, 
////				new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
////				1 / Math.sqrt(ransacPoints.size()), 0.1);
////		
////		if (bestParameter == null) {
////			throw new RuntimeException("Pas de corrélation trouvée");
////		}
////		
////		
////		logger.debug("Transformation is : translate:" + bestParameter[0]+"," + bestParameter[1]+
////				" rotate=" + 180 * Math.atan2(bestParameter[3], bestParameter[2])/Math.PI +
////				" scale=" + Math.sqrt(bestParameter[2] * bestParameter[2] + bestParameter[3] * bestParameter[3]));
////		this.tx = bestParameter[0];
////		this.ty = bestParameter[1];
////		this.cs = bestParameter[2];
////		this.sn = bestParameter[3];
////		
////		found = true;
//	}
//
//	private Triangle findTriangle(List<Triangle> tList, DynamicGridPoint p1, DynamicGridPoint p2, DynamicGridPoint p3)
//	{
//		for(Triangle t : tList)
//		{
//			if (t.s1 == p1) {
//				if (t.s2 == p2) {
//					if (t.s3 == p3) return t;
//				} else if (t.s2 == p3) {
//					if (t.s3 == p2) return t;
//				}
//			} else if (t.s1 == p2) {
//				if (t.s2 == p1) {
//					if (t.s3 == p3) return t;
//				} else if (t.s2 == p3) {
//					if (t.s3 == p1) return t;
//				}
//			} else if (t.s1 == p3) {
//				if (t.s2 == p1) {
//					if (t.s3 == p2) return t;
//				} else if (t.s2 == p2) {
//					if (t.s3 == p1) return t;
//				}
//			}
//		}
//		return null;
//	}
//	
//	private static double[][] newMatrix(int d1, int d2)
//	{
//		double[][] result = new double[d1][];
//		for(int i = 0; i < d1; ++i)
//		{
//			result[i] = new double[d2];
//		}
//		return result;
//	}
//	
//	private List<RansacPoint> getRansacPoints(
//			List<Triangle> imageTriangle,
//			SkyProjection imageSkyProjection,
//			DynamicGrid<Triangle> referenceTriangleGrid,
//			SkyProjection referenceSkyProjection,
//			int maxCount, double distanceMax,
//			double sizeRatioMin, double sizeRatioMax)
//	{
//		List<RansacPoint> ransacPoints = new ArrayList<RansacPoint>();
//		double [] cssn1 = new double[3];
//		double [] cssn2 = new double[3];
//		double [] cssn3 = new double[3];
//		
//		double [] [] image2DPoints = newMatrix(3, 2);
//		double [] [] image3DPoints = newMatrix(3, 3);
//		double [] [] ref2DPoints = newMatrix(3, 2);
//		double [] [] ref3DPoints = newMatrix(3, 3);		
//		
//		int rpDbgId = 0;
//		
//		for(Triangle t : imageTriangle)
//		{
//			// FIXME : ce radius devrait être vajusté en fonction du nombre de triangle, pour sortir suffisement de candidat
//			// En même temps, le matching est absolu (on compare la précision des triangles)
//			List<Triangle> correspondant = referenceTriangleGrid.getNearObject(t.x, t.y, distanceMax);
//			
//			image2DPoints[0][0] = t.getPointX(1);
//			image2DPoints[0][1] = t.getPointY(1);
//			image2DPoints[1][0] = t.getPointX(2);
//			image2DPoints[1][1] = t.getPointY(2);
//			image2DPoints[2][0] = t.getPointX(3);
//			image2DPoints[2][1] = t.getPointY(3);
//			
//			double tlength = t.dst12 + t.dst13 + t.dst23;
//			
//			int id = 1;
//			for(Triangle c : correspondant)
//			{
//				ref2DPoints[0][0] = c.getPointX(1);
//				ref2DPoints[0][1] = c.getPointY(1);
//				ref2DPoints[1][0] = c.getPointX(2);
//				ref2DPoints[1][1] = c.getPointY(2);
//				ref2DPoints[2][0] = c.getPointX(3);
//				ref2DPoints[2][1] = c.getPointY(3);
//				
//
//				// Calculer le rapport de taille entre t et c
//				// Recalculer les coordonnées de t après mutliplication par le rapport de taille (FIXME: ce n'est pas parfait parfait.)
//				// Calculer la transformation affine
//
//				double clength = c.dst12 + c.dst13 + c.dst23;
//				
//				double ratio = tlength / clength;
//				
//				if (ratio < sizeRatioMin || ratio > sizeRatioMax) continue;
//				
//				double oldPixelRad = imageSkyProjection.getPixelRad();
//				// FIXME: ça ne suffit pas vraiment à avoir un accord parfait! Le ratio dépend aussi de la distance au centre de l'image
//				double newPixelRad = oldPixelRad / ratio; 
//				
//				imageSkyProjection.setPixelRad(newPixelRad);
//				try {
//					// Calculer l'emplacement des étoiles en 3D
//					for(int imagePt = 0; imagePt < 3; ++imagePt) {
//						imageSkyProjection.image2dToImage3d(image2DPoints[imagePt], image3DPoints[imagePt]);
//					}
//
//					// Calculer la nouvelle taille en pixel
//					double tlengthAdjusted = 
//							Math.sqrt(d2(image3DPoints[0], image3DPoints[1])) + 
//							Math.sqrt(d2(image3DPoints[1], image3DPoints[2])) + 
//							Math.sqrt(d2(image3DPoints[2], image3DPoints[0]));
//					
//					for(int imagePt = 0; imagePt < 3; ++imagePt) {
//						referenceSkyProjection.image2dToImage3d(ref2DPoints[imagePt], ref3DPoints[imagePt]);
//					}
//						
//					RansacPoint rp = new RansacPoint();
//					rp.dbgId = rpDbgId++;
//					rp.image = t;
//					rp.original = c;
//					rp.projection = new SkyProjection(imageSkyProjection, AffineTransform3D.getRotationMatrix(image3DPoints, ref3DPoints));
//					
//					for(int imagePt = 0; imagePt < 3; ++imagePt) {
//						rp.projection.getTransform().convert(image3DPoints[imagePt]);
//					}
//					
//					ransacPoints.add(rp);
//
//					if (ransacPoints.size() > maxCount) {
//						logger.warn("Too many translation founds. Retry with stricter filter");
//						return null;
//					}
//					
//					id++;
//
//				} finally {
//					imageSkyProjection.setPixelRad(oldPixelRad);
//				}
//				
////				c.calcRotation(t, 1, 2, cssn1);
////				c.calcRotation(t, 1, 3, cssn2);
////				c.calcRotation(t, 2, 3, cssn3);
////				
////				// Faire la moyenne des cos/sin, les rendre normé
////				// Faire la moyenne des ratios.
////				double cs = cssn1[0] + cssn2[0] + cssn3[0];
////				double sn = cssn1[1] + cssn2[1] + cssn3[1];
////				
////				double angle = 180 * Math.atan2(sn, cs) / Math.PI;
////				
////				double div = Math.sqrt(cs * cs + sn * sn);
////				// Ici, on s'attend à avoir une norme trés proche de 3 (atteignable uniquement si les 3 rotations sont identiques)
////				if (div < 2.9 || div > 3.1) continue;
////				div = 1.0 / div;
////				cs *= div;
////				sn *= div;
////				
////				
////				
////				double ratio = (cssn1[2] + cssn2[2] + cssn3[2]) / 3;
////				cs *= ratio;
////				sn *= ratio;
////				
////				double dlt = (cssn1[2] - ratio) * (cssn1[2] - ratio)  + (cssn2[2] - ratio) * (cssn2[2] - ratio) + + (cssn3[2] - ratio) * (cssn3[2] - ratio);
////				
////				if (dlt > 0.001) {
////					// si les cos/sin ne sont pas orthogonaux, la translation déforme....
////					continue;
////				}
////				
////				// On  met un filtre sur le grossissement également
////				if (ratio < sizeRatioMin || ratio > sizeRatioMax) continue;
////				
////				double tx = 0, ty = 0;
////				
////				// Calcul de la translation
////				for(int i = 1; i <= 3; ++i)
////				{
////					double xRef = t.getPointX(i);
////					double yRef = t.getPointY(i);
////					
////					double xRotateScale = xRef * cs + yRef * sn; 
////					double yRotateScale = yRef * cs - xRef * sn; 
////					
////					double dltx = c.getPointX(i) - xRotateScale;
////					double dlty = c.getPointY(i) - yRotateScale;
////					
////					tx += dltx;
////					ty += dlty;
////				}
////				
////				tx /= 3;
////				ty /= 3;
////				double delta = Math.sqrt((t.x - c.x)*(t.x - c.x) + (t.y - c.y) * (t.y - c.y));
////				
////				
////				RansacPoint rp = new RansacPoint();
////				rp.image = t;
////				rp.original = c;
////				rp.tx = tx;
////				rp.ty = ty;
////				rp.cs = cs;
////				rp.sn = sn;
////				
////				ransacPoints.add(rp);
////				
////				// logger.debug("Found possible translation (" + id+" ) " + tx +" - " + ty + " scale=" + ratio + ", angle="+angle+" with delta=" + delta);
////				
////				if (ransacPoints.size() > maxCount) {
////					logger.warn("Too many translation founds. Retry with stricter filter");
////					return null;
////				}
////				
////				id++;
//			}
//		}
//		return ransacPoints;
//	}
//	
//    public static boolean pntInTriangle(double px, double py, double x1, double y1, double x2, double y2, double x3, double y3) {
//	
//	    double o1 = getOrientationResult(x1, y1, x2, y2, px, py);
//	    double o2 = getOrientationResult(x2, y2, x3, y3, px, py);
//	    double o3 = getOrientationResult(x3, y3, x1, y1, px, py);
//	
//	    return (o1 == o2) && (o2 == o3);
//	}
//	
//	private static int getOrientationResult(double x1, double y1, double x2, double y2, double px, double py) {
//	    double orientation = ((x2 - x1) * (py - y1)) - ((px - x1) * (y2 - y1));
//	    if (orientation > 0) {
//	        return 1;
//	    }
//	    else if (orientation < 0) {
//	        return -1;
//	    }
//	    else {
//	        return 0;
//	    }
//	}
//	
//	static int filtered = 0;
//	static int retained = 0;
//
//	/**
//	 * @param referenceStars étoiles, triées par ordre de luminosité décroissante
//	 * @param minTriangleSize
//	 * @param triangleSearchRadius
//	 * @param maxTriangle
//	 * @param maxRatio : rapport de luminosité max entre la plus faible et la plus lumineuse étoile d'un triangle
//	 * @param geoRatio : filtre (de 0 à 1) sur la forme des triangle. 1 = uniquement des equilateral, 0 = n'importe
//	 * @return
//	 */
//	private List<Triangle> getTriangleList(List<? extends DynamicGridPointWithAdu> referenceStars, 
//			SkyProjection skyProjection,
//			double minTriangleSize, double triangleSearchRadius, int maxTriangle, double maxRatio, double geoRatio)
//	{
//		// Trouver les points à moins de 50 pixels
//		DynamicGrid<DynamicGridPointWithAdu> reference = new DynamicGrid<DynamicGridPointWithAdu>((List<DynamicGridPointWithAdu>)referenceStars);
//		List<Triangle> result = new ArrayList<Triangle>();
//		
//		double [] i_d = new double[3];
//		double [] r_d = new double[3];
//
//		double minTriangleSizeInRad2 = Math.pow(minTriangleSize * skyProjection.getPixelRad(), 2);
//		
//		minTriangleSize = minTriangleSize * minTriangleSize;
//		
//		double maxTriangleSizeInRad2 = Math.pow(triangleSearchRadius * skyProjection.getPixelRad(), 2); 
//				
//		double maxTriangleSize2 = triangleSearchRadius * triangleSearchRadius;
//		
//		
//
//		double [] rst1pos2d = new double[2];
//		double [] rst2pos2d = new double[2];
//		double [] rst3pos2d = new double[2];
//		
//		double [] rst1pos3d = new double[3];
//		double [] rst2pos3d = new double[3];
//		double [] rst3pos3d = new double[3];
//		
//		logger.info("Searching for triangles in " + referenceStars.size() + " stars - minsize=" + Math.sqrt(minTriangleSize) + ", maxsize=" + triangleSearchRadius + ", brightness ratio=" + maxRatio + ", geo ratio=" + geoRatio);
//		for(DynamicGridPointWithAdu rst1 : referenceStars)
//		{
//			// Fixme: ajuster le search radius en fonction de la distance de rst1 (l'echelle est connue)
//			List<DynamicGridPointWithAdu> referencePeerList = reference.getNearObject(rst1.getX(), rst1.getY(), 1.5 * triangleSearchRadius);
//			
//			if (referencePeerList.isEmpty()) continue;
//			
//			rst1pos2d[0] = rst1.getX();
//			rst1pos2d[1] = rst1.getY();
//			
//			skyProjection.image2dToImage3d(rst1pos2d, rst1pos3d);
//			
//			for(int a = 0; a < referencePeerList.size(); ++a)
//			{
//				DynamicGridPointWithAdu rst2 = referencePeerList.get(a);
//				if (rst2 == rst1) continue;
//				if (compareTo(rst1, rst2) >= 0) continue;
//			
//				
//				double minAdu = rst1.getAduLevel();
//				double maxAdu = rst2.getAduLevel();
//				
//				if (minAdu > maxAdu) {
//					double tmp = minAdu;
//					minAdu = maxAdu;
//					maxAdu = tmp;
//				}
//				if (maxAdu > minAdu * maxRatio) continue;
//
//				rst2pos2d[0] = rst2.getX();
//				rst2pos2d[1] = rst2.getY();
//				skyProjection.image2dToImage3d(rst2pos2d, rst2pos3d);
//
//				// On a une distance en radian (en quelque sorte)
//				// Pour la remettre en pixel, il faut la mettre en degres
//				// puis la mettre en pixel
//				double r_d1_3d = d2(rst1pos3d, rst2pos3d);
//				
//				
////				double r_d1 = d2(rst1, rst2);
//				// if (r_d1 < minTriangleSize) continue;
//				if (r_d1_3d > maxTriangleSizeInRad2) continue;
//				
//			PeerLoop:
//				for(int b = 0; b < referencePeerList.size(); ++b)
//				{
//					DynamicGridPointWithAdu rst3 = referencePeerList.get(b);
//					if (rst3 == rst1) continue;
//					if (rst3 == rst2) continue;
//					if (compareTo(rst2, rst3) >= 0) continue;
//				
//					double newAdu = rst3.getAduLevel();
//					if (newAdu < minAdu && newAdu * maxRatio < maxAdu) {
//						// Trop petit
//						continue;
//					}
//					if (newAdu > maxAdu && minAdu * maxRatio < newAdu) {
//						// Trop gran
//						continue;
//					}
//					
//					rst3pos2d[0] = rst3.getX();
//					rst3pos2d[1] = rst3.getY();
//					skyProjection.image2dToImage3d(rst3pos2d, rst3pos3d);
//					
//					r_d[0] = r_d1_3d;
//					r_d[1] = d2(rst1pos3d, rst3pos3d);
//					r_d[2] = d2(rst2pos3d, rst3pos3d);
//
//					
//					double maxSize = Math.max(Math.max(r_d[0], r_d[1]), r_d[2]);
//					if (maxSize < minTriangleSizeInRad2) continue;
//					
//					Triangle t = new Triangle(rst1, rst2, rst3, 
//							r_d[0],
//							r_d[1],
//							r_d[2]);
//					// Exclure les triangles trop symétriques
//					if (((filtered + retained) & 0xfff) == 0)
//					{
//					//	System.out.println("filtered : " + filtered + "  retained : " + retained);
//					}
//					
//					if (t.getX() > 0.90 || t.getY() > 0.9) {
//						filtered ++;
//						continue;
//					} else {
//						retained++;
//					}
//					
//					// Avec ces critère, le geoRation maxi est 0.8 * 0.6
//					if (geoRatio > 0 && t.getX() * t.getY() < geoRatio * 0.9 * 0.9) {
//						continue;
//					}
//					
//					// Vérifier que l'on n'ait pas d'étoile plus lumineuse à l'interieur du triangle
//					double aduMinOfTriangle = rst1.getAduLevel();
//					aduMinOfTriangle = Math.min(aduMinOfTriangle, rst2.getAduLevel());
//					aduMinOfTriangle = Math.min(aduMinOfTriangle, rst3.getAduLevel());
//					
//					double triangle_x0,triangle_y0,triangle_x1, triangle_y1;
//					triangle_x0 = Math.min(rst1.getX(), Math.min(rst2.getX(), rst3.getX()));
//					triangle_y0 = Math.min(rst1.getY(), Math.min(rst2.getY(), rst3.getY()));
//					triangle_x1 = Math.max(rst1.getX(), Math.max(rst2.getX(), rst3.getX()));
//					triangle_y1 = Math.max(rst1.getY(), Math.max(rst2.getY(), rst3.getY()));
//					
////					for(DynamicGridPointWithAdu obstacle : referenceStars)
////					{
////						if (obstacle.getAduLevel() < aduMinOfTriangle) break;
////						if (obstacle.getX() < triangle_x0) continue;
////						if (obstacle.getY() < triangle_y0) continue;
////						if (obstacle.getX() > triangle_x1) continue;
////						if (obstacle.getY() > triangle_y1) continue;
////						
////						if (obstacle == rst1) continue;
////						if (obstacle == rst2) continue;
////						if (obstacle == rst3) continue;
////						
////						if (pntInTriangle(
////								obstacle.getX(), obstacle.getY(),
////								rst1.getX(), rst1.getY(),
////								rst2.getX(), rst2.getY(),
////								rst3.getX(), rst3.getY())) {
////					
////							continue PeerLoop;
////						}
////								
////								
////					}
//					if ((result.size() & 0xffff) == 0) {
//						logger.info("searching... now at " + result.size() + " triangles");
//					}
//					result.add(t);
//					if (result.size() >  maxTriangle) {
//						logger.warn("Found too many triangles... retry with stricter filter");
//						return null;
//					}
////					if (logger.isDebugEnabled()) {
////						logger.debug("Found triangle : " + Math.sqrt(r_d[0]) + " - " + Math.sqrt(r_d[1]) + " - " + Math.sqrt(r_d[2]));
////					}
//				}
//			}
//		}
//		
//		logger.info("Found " + result.size());
//		
//		return result;
//	}
//
//	// Permet d'avoir un ordre arbitraire sur les points pour éviter de traiter tous les triangles en x3.
//	private static int compareTo(DynamicGridPoint a, DynamicGridPoint b)
//	{
//		double ax, bx;
//		ax = a.getX();
//		bx = b.getX();
//		if (ax <  bx) {
//			return 1;
//		}
//		if (ax > bx) {
//			return -1;
//		}
//		double ay, by;
//		ay = a.getY();
//		by = b.getY();
//		if (ay <  by) {
//			return 1;
//		}
//		if (ay > by) {
//			return -1;
//		}
//		
//		return 0;
//	}
//
//	private static class Triangle implements DynamicGridPoint
//	{
//		// Plus grande distance : s2-s3
//		// Plus petite distance : s1-s2
//		DynamicGridPoint s1, s2, s3;
//		
//		double dst12, dst13, dst23;
//		
//		// Les rapports : moyen / grand et petit/moyen
//		double x, y;
//		
//		Triangle(DynamicGridPoint s1, DynamicGridPoint s2, DynamicGridPoint s3, double dst12, double dst13, double dst23)
//		{
//			this.s1 = s1;
//			this.s2 = s2;
//			this.s3 = s3;
//			
//			if (dst12 >= dst13 && dst12 >= dst23)
//			{
//				// Le plus grand segment est 1-2. On echange 1 et 3
//				DynamicGridPoint tmpStar = this.s1;
//				this.s1 = this.s3;
//				this.s3 = tmpStar;
//				
//				// Echange dst12 avec dst23. dst13 est inchangé
//				double tmp = dst12;
//				dst12 = dst23;
//				dst23 = tmp;
//			} else if (dst13 >= dst12 && dst13 >= dst23) {
//				// Le plus grand segment est 1-3. On echange 1 et 2.
//				DynamicGridPoint tmpStar = this.s1;
//				this.s1 = this.s2;
//				this.s2 = tmpStar;
//				
//				// Echange dst13 avec dst23. dst12 est inchangé
//				double tmp = dst13;
//				dst13 = dst23;
//				dst23 = tmp;
//			}
//			
//			//dst23 est maintenant le plus grand
//			if (dst23 < dst12 || dst23 < dst13) {
//				throw new RuntimeException("Ca marche pas ton truc !");
//			}
//				
//			// On veut maintenant que le plus petit soit dst12.
//			if (dst12 > dst13) {
//				// Echanger 2 et 3. dst23 reste inchangé
//				DynamicGridPoint tmpStar = this.s2;
//				this.s2 = this.s3;
//				this.s3 = tmpStar;
//				
//				double tmp = dst13;
//				dst13 = dst12;
//				dst12 = tmp;
//			}
//			
//
//			//dst12 est maintenant le plus petit
//			if (dst12 > dst23 || dst12 > dst13 || dst13 > dst23) {
//				throw new RuntimeException("Ca marche pas ton truc ! (2)");
//			}
//			
//			this.dst12 = Math.sqrt(dst12);
//			this.dst13 = Math.sqrt(dst13);
//			this.dst23 = Math.sqrt(dst23);
//			
//			x = this.dst13 / this.dst23;
//			y = this.dst12 / this.dst13;
//			
//		}
//		
//		@Override
//		public double getX() {
//			return x;
//		}
//		
//		@Override
//		public double getY() {
//			return y;
//		}
//		
//		private double getPointX(int p)
//		{
//			switch(p - 1) {
//			case 0:
//				return s1.getX();
//			case 1:
//				return s2.getX();
//			case 2:
//				return s3.getX();
//			}
//			return 0;
//		}
//		
//		private double getPointY(int p)
//		{
//			switch(p - 1) {
//			case 0:
//				return s1.getY();
//			case 1:
//				return s2.getY();
//			case 2:
//				return s3.getY();
//			}
//			return 0;			
//		}
//		
//		private double getDistance(int p1, int p2)
//		{
//			if (p1 > p2) {
//				int tmp = p1;
//				p1 = p2;
//				p2 = tmp;
//			}
//			if (p1 == 1) {
//				if (p2 == 2) {
//					return dst12;
//				}
//				return dst13;
//			}
//			return dst23;
//		}
//		
//		// Fourni cos et sin et ratio pour passer de this à other
//		public double [] calcRotation(Triangle other, int p1, int p2, double [] calc)
//		{
//			if (calc == null || calc.length != 3) {
//				calc = new double[3];
//			}
//			
//			// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
//			double Xa = other.getPointX(p2) - other.getPointX(p1);
//			double Ya = other.getPointY(p2) - other.getPointY(p1);
//			double Xb = this.getPointX(p2) - this.getPointX(p1);
//			double Yb = this.getPointY(p2) - this.getPointY(p1);
//			
//			calc[0] = (Xa*Xb+Ya*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
//			calc[1] = (Ya*Xb-Xa*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
//			calc[2] = this.getDistance(p1, p2) / other.getDistance(p1, p2);
//			
//			// On doit avoir :
//			
//			double XbVerif = (Xa * calc[0] + Ya * calc[1]) * calc[2];
//			double YbVerif = (Ya * calc[0] - Xa * calc[1]) * calc[2];
//			
//			return calc;
//		}
//	}
//
//	private static class RansacPoint {
//		Triangle original;
//		Triangle image;
//		
//		SkyProjection projection;
//		
//		int dbgId;
//		
//		@Override
//		public String toString() {
//			return projection.toString();
////			double angle = Math.atan2(cs, sn) * 180 / Math.PI;
////			double scale = Math.sqrt(cs * cs + sn * sn);
////			return String.format("translation=(%.2f,%.2f) rotation=%.2f scale=%.3f", tx, ty, angle, scale); 
//		}
//		
////		public void project(double x, double y, double [] result)
////		{
////			result[0] = this.tx + x * this.cs + y * this.sn;
////			result[1] = this.ty + y * this.cs - x * this.sn;
////		}
////		
////		public void unproject(double nvx, double nvy, double [] result)
////		{
////			result[0] = ((-1.0)*((cs*(tx - nvx)) + (sn*(nvy - ty)))/((sn*sn) + (cs*cs)));
////			result[1] = (((sn*(nvx - tx)) + (cs*(nvy - ty)))/((sn*sn) + (cs*cs)));
////		}
//		
////		@Override
////		public double getRansacParameter(int order) {
////			switch(order) {
////			case 0:
////				return tx;
////			case 1:
////				return ty;
////			case 2:
////				return cs;
////			case 3:
////				return sn;
////			default:
////				return 0;
////			}
////		}
//		
//	}
//
//	private static double d2(DynamicGridPoint d1, DynamicGridPoint d2)
//	{
//		return (d1.getX() - d2.getX()) * (d1.getX() - d2.getX()) + (d1.getY() - d2.getY()) * (d1.getY() - d2.getY());
//	}
//
//	private static double d2(double [] i3d, double [] j3d)
//	{
//		double d0 = i3d[0] - j3d[0];
//		d0 = d0 * d0;
//		double d1 = i3d[1] - j3d[1];
//		d1 = d1 * d1;
//		double d2 = i3d[2] - j3d[2];
//		d2 = d2 * d2;
//		return d0 + d1 + d2;
//	}
//	
//	public SkyProjection getSkyProjection()
//	{
//		return skyProjection;
//	}
//	
//	public boolean isFound() {
//		return found;
//	}
//
//}
