package fr.pludov.scopeexpress;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import fr.pludov.io.FitsPlane;

public class StarDetection {
	private static final Logger logger = Logger.getLogger(StarDetection.class);
	
	StarDetectionParameters parameters;
	
	public StarDetection(StarDetectionParameters parameters)
	{
		this.parameters = new StarDetectionParameters(parameters);
	}
	
	private static class StarCandidate {
		int x, y;
		// En adu/s
		float energy;
		double fwhm;
	}
	
	private static Comparator<StarCandidate> pixelValueComparator()
	{
		return new Comparator<StarCandidate>(){
			@Override
			public int compare(StarCandidate o1, StarCandidate o2) {
				return Double.compare(o2.energy, o1.energy);
			}
		};
	}
	
	
	// Données dynamique (pour détection en cours)
	float [] image;
	int sx, sy;
	
	private void median3()
	{
		generic3(4);
	}
	
	private void erode()
	{
		generic3(0);
	}
	
	private void generic3(int slot)
	{
		float [] tmp = new float[image.length];
		int id = 0;
		
		float array[] = new float[9];
		
		for(int y = 1; y < sy - 1; ++y)
			for(int x = 1; x < sx - 1; ++x)
			{
				id = x + sx * y;
			
				array[0] = image[id - sx - 1];
				array[1] = image[id - sx ];
				array[2] = image[id - sx  + 1];
				array[3] = image[id - 1];
				array[4] = image[id ];
				array[5] = image[id + 1];
				array[6] = image[id + sx - 1];
				array[7] = image[id + sx ];
				array[8] = image[id + sx  + 1];
				
				// Trouver la valeur médiane
				Arrays.sort(array);
				
				tmp[id] = array[4];
			}
		
		image = tmp;
	}
	
	private void background()
	{
		int nbSquareX = (sx + parameters.getBackgroundSquare() - 1) / parameters.getBackgroundSquare();
		int nbSquareY = (sy + parameters.getBackgroundSquare() - 1) / parameters.getBackgroundSquare();
		
		if (nbSquareX == 0) nbSquareX = 1;
		if (nbSquareY == 0) nbSquareY = 1;
		
		
		int backgroundSquare = parameters.backgroundSquare;
		
		double [] backgroundLevel = new double[nbSquareX * nbSquareY];
		for(int squarey = 0; squarey < nbSquareY; ++squarey)
		{
			for(int squarex = 0; squarex < nbSquareX; ++squarex)
			{
				int x0 = squarex * backgroundSquare;
				int y0 = squarey * backgroundSquare;
				int x1 = x0 + backgroundSquare;
				if (x1 >= sx) x1 = sx - 1;
				int y1 = y0 + backgroundSquare;
				if (y1 >= sy) y1 = sy - 1;
				
				// Calculer la moyenne des x% les plus faibles et soustraire.
				double value = getBackground(x0, y0, x1, y1);
				
				backgroundLevel[squarex + nbSquareX * squarey] = value;
			}
		}
		
		BufferedImage mask = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
		
		for(int y = 0; y < sy; ++y)
			for(int x = 0; x < sx; ++x)
			{
				double fsqx = (x - backgroundSquare / 2) * 1.0 / backgroundSquare;
				double fsqy = (y - backgroundSquare / 2) * 1.0 / backgroundSquare;
				
				int squarex;
				double factNextX;// Ratio à prendre dans x+1
				
				int squarey;
				double factNextY;// Ratio à prendre dans x+1
				
				if (fsqx < 0) {
					squarex = 0;
					factNextX = 0;
				} else if (fsqx >= nbSquareX - 1) {
					squarex = nbSquareX - 1;
					factNextX = 0;
				} else {
					squarex = (int)Math.floor(fsqx);
					factNextX = (fsqx - squarex);
				}
				
				if (fsqy < 0) {
					squarey = 0;
					factNextY = 0;
				} else if (fsqy >= nbSquareY - 1) {
					squarey = nbSquareY - 1;
					factNextY = 0;
				} else {
					squarey = (int)Math.floor(fsqy);
					factNextY = (fsqy - squarey);
				}
				
				double dx0y0 = backgroundLevel[squarex + nbSquareX * squarey];
				double dx1y0 = factNextX > 0 ? backgroundLevel[squarex + 1 + nbSquareX * squarey] : dx0y0;
				double dx0y1 = factNextY > 0 ? backgroundLevel[squarex + nbSquareX * (squarey + 1)] : dx0y0;
				double dx1y1 = factNextX > 0 ? 
								(factNextY > 0 ? backgroundLevel[squarex + 1 + nbSquareX * (squarey + 1)] : dx1y0) :
								(factNextY > 0 ? dx0y1 : dx0y0);
				
				double val =  dx0y0 * (1 - factNextX) * (1 - factNextY)
							+ dx1y0 * (factNextX) * (1 - factNextY)
							+ dx0y1 * (1 - factNextX) * (factNextY)
							+ dx1y1 * (factNextX) * (factNextY);
				
				int rgb = (int)(255 * val);
				if (rgb < 0) rgb = 0;
				if (rgb > 255) rgb = 255;
				mask.setRGB(x, y, rgb | rgb << 8 | rgb << 16);
				
				int id = x + sx * y;
				float img = image[id];
				img -= val;
				if (img < 0) img = 0;
				image[id] = img;
			}
		try {
			ImageIO.write(mask, "png", new File("c:/background.png"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	List<StarCandidate> detectCandidates(int x0, int y0, int w, int h)
	{
		List<StarCandidate> result = new ArrayList<StarDetection.StarCandidate>();
		int candidateCount = 0;
		
//		int [] position  = new int[image.length];
//		for(int i = 0; i < position.length; ++i)
//		{
//			position[i] = i;
//		}
//		float[] imageCopy = Arrays.copyOf(image, image.length);
//		Arrays.sort(imageCopy);
//		
//		float seuil = imageCopy[(image.length * 99) / 100];
		
		float absoluteAduSeuil = (float)parameters.getAbsoluteAduSeuil();
		
		for(int y = 0; y < h; ++y)
		{
			for(int x = 0; x < w; ++x)
			{
				int id = (x + x0) + sx * (y + y0);
				
				float val = image[id];
				if (x + x0 > 0) {
					float valCheck = image[id - 1];
					if (valCheck > val) continue;
				}
				
				if (x + x0 + 1 < sx) {
					float valCheck = image[id + 1];
					if (valCheck > val) continue;
				}
				
				if (y + y0 > 0) {
					float valCheck = image[id - sx];
					if (valCheck > val) continue;
				}
				
				if (y + y0 + 1 < sy) {
					float valCheck = image[id + sx];
					if (valCheck > val) continue;
				}
				
//				if (binFactor * x < 528 - 70) continue;
//				if (binFactor * x > 528 + 70) continue;
//				if (binFactor * y < 430 - 70) continue;
//				if (binFactor * y > 430 + 70) continue;

				if (val > absoluteAduSeuil) {
					StarCandidate candidate = new StarCandidate();
					candidate.x = x0 + x;
					candidate.y = y0 + y;
					candidate.energy = val;
				
					result.add(candidate);
					
					candidateCount ++;
				}
			}
		}
		
		return result;
	}
	
	double gaussCenterX, gaussCenterY;
	double gaussIntensity;
	double gaussFwhm;
	double gaussEvaluation;
	
	double gaussFwhmMin;
	double gaussIntensityMin;
	
		
	private void substractGauss(double x, double y, double fwhm, double intensity)
	{
		int centerx = (int)Math.round(x);
		int centery = (int)Math.round(y);
		int width = (int)Math.floor(fwhm * 1.75);
		
		if ((centerx - width < 0) || (centerx + width >= sx)) {
			return ;
		}
		
		if ((centery - width < 0) || (centery + width >= sy)) {
			return ;
		}
		
		// Si dst2 = fwhm*fwhm , dst2 * mul = fact2 
		// mul = fact2 / fwhm²
		double fact2 = Math.log(2);
		double mul = fact2 / (fwhm * fwhm);
		
		for(int dy = -width; dy <= width; ++dy)
			for(int dx = -width; dx <= width; ++dx)
			{
				if (centerx+dx < 0) continue;
				if (centerx+dx >= sx) continue;
				if (centery+dy < 0) continue;
				if (centery+dy >= sy) continue;
				double dst2 = (centerx + dx - x) * (centerx + dx - x) + 
							  (centery + dy - y) * (centery + dy - y);
				double exp = dst2 * mul;
				// De 1 à ... 
				double normal = gaussIntensity * Math.exp(-dst2 * mul);

				int id = centerx + dx + sx * (centery + dy);
				this.image[id] -= normal;
				if (this.image[id] < 0) {
					this.image[id] = 0;
				}
			}
	}
	
	private double matchGauss(double x, double y, double fwhm, double intensity)
	{
		if (fwhm < gaussFwhmMin) {
			return Double.POSITIVE_INFINITY;
		}
		
		if (intensity < gaussIntensityMin) {
			return Double.POSITIVE_INFINITY;
		}
		
		int centerx = (int)Math.round(x);
		int centery = (int)Math.round(y);
		int width = (int)Math.floor(fwhm * 1.75);
		
		if ((centerx - width < 0) || (centerx + width >= sx)) {
			return Double.POSITIVE_INFINITY;
		}
		
		if ((centery - width < 0) || (centery + width >= sy)) {
			return Double.POSITIVE_INFINITY;
		}
		
		// Si dst2 = fwhm*fwhm , dst2 * mul = fact2 
		// mul = fact2 / fwhm²
		double fact2 = Math.log(2);
		double mul = fact2 / (fwhm * fwhm);
		
		double err = 0;
		double errWeight = 0;
		for(int dy = -width; dy <= width; ++dy)
			for(int dx = -width; dx <= width; ++dx)
			{
				double dst2 = (centerx + dx - x) * (centerx + dx - x) + 
							  (centery + dy - y) * (centery + dy - y);
				double exp = dst2 * mul;
				// De 1 à ... 
				double normal = gaussIntensity * Math.exp(-dst2 * mul);

				// Calcul le décalage 
				errWeight += normal;
				
				float pix = this.image[centerx + dx + sx * (centery + dy)];
				
				err += (pix - normal) * (pix - normal);
			}
		
		// Erreur par pixel, rapportée à l'énergie totale
		//  * (2 * width + 1)*(2 * width + 1)
		err = err / ((2 * width + 1)*(2 * width + 1));
		
		return err;
	}
	
	// Delta = magnitude : 
	// 	1 de 0.5 à 2 * fwhm
	//  2 de 0.25 à 4 * fwhm
	private void ajusteFwhm(double delta)
	{
		double reference = gaussEvaluation;
		double bestDelta = 1.0;
		
		int step = 10;
		for(int i = -step; i < step; ++i)
		{
			if (i == 0) continue;
			
			double d = Math.pow(2, i * delta / step);
			
			double value = matchGauss(gaussCenterX, gaussCenterY, gaussFwhm * d, gaussIntensity);
			if (value < reference) {
				reference = value;
				bestDelta = d;
			}
		}
		
		gaussFwhm *= bestDelta;
		gaussEvaluation = reference;
	}
	
	private void adjustGaussIntensity(double delta)
	{
		double reference = gaussEvaluation;
		double bestDelta = 1.0;
		for(int i = -5; i < 5; ++i)
		{
			if (i == 0) continue;
			
			double d = Math.pow(2, i * delta / 5);
			double value = matchGauss(gaussCenterX, gaussCenterY, gaussFwhm, gaussIntensity * d);
			if (value < reference) {
				reference = value;
				bestDelta = d;
			}
		}
		
		gaussIntensity *= bestDelta;
		gaussEvaluation = reference;
	}
	
	private void ajusteGaussCenterStep(double delta)
	{
		// On va faire un matching sur gaussCenterX, gaussCenterY
		double reference = gaussEvaluation;
		double dx = 0, dy = 0; 
		
		// Nombre de steps.
		int step = 4;
		for(int xd = -step; xd <= step; ++xd)
		{
			for(int yd = -step; yd <= step; ++yd)
			{
				if (xd == 0 || yd == 0) continue;
				double value = matchGauss(gaussCenterX + xd * delta / step, gaussCenterY + yd * delta / step, gaussFwhm, gaussIntensity);
				if (value < reference) {
					reference = value;
					dx = xd * delta / step;
					dy = yd * delta / step;
				}
			}
		}
		
		gaussCenterX += dx;
		gaussCenterY += dy;
		gaussEvaluation = reference;
	}
	
	
	/**
	 * Trouve la gaussienne et l'erreur minimale...
	 * 
	 * Il faudrait que l'erreur soit rapportée à la taille de l'étoile (car une étoile trés lumineuse va donner beaucoup d'erreur)
	 */
	private double evaluateStar(int x, int y, float factor)
	{
		gaussCenterX = x;
		gaussCenterY = y;
		gaussFwhm = 4.0;
		gaussIntensity = factor;
		gaussEvaluation = matchGauss(x, y, gaussFwhm, gaussIntensity);
		

		gaussFwhmMin = 0.8;
		gaussIntensityMin = Math.max(gaussIntensity * 0.5, this.parameters.getAbsoluteAduSeuil());
		
		for(int i = 0; i < 8; ++i)
		{
			logger.debug("Before step " + i + " : " + gaussCenterX+"," + gaussCenterY+"=>"+gaussIntensity+", fwhm=" + gaussFwhm + ", mean relative error=" + gaussEvaluation);

			// On divise par 4 la marge à chaque itération
			double fact = Math.pow(0.5, i);
			
			// Déplacer de quelques pixels -- fixme : mettre en conf le radius max.
			ajusteGaussCenterStep(8 * fact);
			
			// Ajuster la fwhm
			ajusteFwhm(2.5 * fact);
			
			adjustGaussIntensity(0.5 * fact);
		}
		
		return gaussEvaluation;
	}
	
	private boolean hasTopAdu(int x0, int y0, int x1, int y1, float limit)
	{
		for(int y = y0; y <= y1; ++y)
			for(int x = x0; x <= x1; ++x)
			{
				int id = x + sx * y;
				if (this.image[id] >= limit) {
					return true;
				}
			}
		return false;
	}
	
	private boolean clearTopAdu(int x0, int y0, int x1, int y1)
	{
		for(int y = y0; y <= y1; ++y)
			for(int x = x0; x <= x1; ++x)
			{
				int id = x + sx * y;
				if (this.image[id] >= this.parameters.getAbsoluteAduSeuil()) {
					this.image[id] = 0;
				}
			}
		return false;
	}
	

	/**
	 * Barycentre, somme de l'energie
	 */
	private double getBackground(int x0, int y0, int x1, int y1)
	{
		double rx = 0, ry = 0;
		double weightSum = 0;
		float [] array  = new float[(x1 - x0 + 1) * (y1 - y0 + 1)];
		int arrayId = 0;
		for(int y = y0; y <= y1; ++y)
			for(int x = x0; x <= x1; ++x)
			{
				int id = x + sx * y;
				float p = this.image[id];
				array[arrayId++] = p;
			}
		Arrays.sort(array);
		int nbPix = (int)(array.length * this.parameters.getBackgroundEvaluationPct());
		double sum = 0;
		for(int i = 0; i < nbPix; ++i)
		{
			sum += array[i];
		}
		return nbPix > 0 ? sum / nbPix : 0;
	}
	
	/**
	 * Barycentre, somme de l'energie
	 */
	private double [] getBarycentre(int x0, int y0, int x1, int y1)
	{
		double rx = 0, ry = 0;
		double weightSum = 0;
		for(int y = y0; y <= y1; ++y)
			for(int x = x0; x <= x1; ++x)
			{
				int id = x + sx * y;
				float p = this.image[id];
				if (p < 0) {
					p = 0;
				}
				rx += (x - x0) * p;
				ry += (y - y0) * p;
				weightSum += p;
			}
		double baryx = x0 + rx / weightSum;
		double baryy = y0 + ry / weightSum;
//		if (baryx < x0 || baryx > x1 || baryy < y0 || baryy > y1)
//		{
//			throw new RuntimeException("bary is out of bounds !");
//		}
		return new double[] {baryx, baryy, weightSum};
	}
	
	private double getFWHM(int x0, int y0, int x1, int y1, double centerx, double centery)
	{
		// On calcule l'écart type de la distance
		double sum = 0;
		double weight = 0;
		for(int y = y0; y <= y1; ++y)
			for(int x = x0; x <= x1; ++x)
			{
				int id = x + sx * y;
				float p = this.image[id];
				
				double d = ((x - centerx) * (x - centerx) + (y - centery) * (y - centery));
				
				sum += d * p;
				weight += p;
			}
		return 2.355 * Math.sqrt(sum/weight);
	}
	
	public class StarDetectionSector
	{
		int x, y, w, h;
		List<StarCandidate> candidates;
		int readPos;
		
		StarDetectionSector(int x, int y, int w, int h)
		{
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
		
		void detectStars()
		{
			this.candidates = detectCandidates(x, y, w, h);
			Collections.sort(this.candidates, pixelValueComparator());
			this.readPos = 0;
		}
		
		boolean hasNext()
		{
			return readPos < candidates.size();
		}
		
		StarCandidate next()
		{
			return candidates.get(readPos++);
		}
		
		double getLevelOfNextCandidate()
		{
			return candidates.isEmpty() ? -1 : candidates.get(0).energy;
		}
		
	}
	
	// Double adu255 : valeur d'adu pour 255.
	public List<ImageStar> proceed(FitsPlane img)
	{
		int binFactor = parameters.getBinFactor();
		// Construire un tableau avec les valeurs  
		sx = img.getSx() / binFactor;
		sy = img.getSy() / binFactor;
		
		if (binFactor != 1) throw new RuntimeException("unimplemented");
		image = Arrays.copyOf(img.getValue(),  img.getValue().length);
		for(int i = 0; i < image.length; ++i)
		{
			image[i] = img.getValue()[i];
		}

		
		background();
		median3();

		// pour DEBUG : enregistrer l'image sans le fond.
		BufferedImage mask = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
		for(int y = 0; y < sy; ++y)
		{
			for(int x = 0 ; x < sx; ++x)
			{
				int val = Math.round(255 * image[x + y * sx]);
				if (val < 0) val = 0;
				if (val > 255) val = 255;
				mask.setRGB(x, y, val);
			}
		}
		
		// On découpe l'image en secteur et on fait une recherche par secteur.
		// On trouve une étoile sur chaque secteur et on continue.
		// A chaque itération, on trouve la candidate la plus brillante sur chaque secteur...
		
		List<StarDetectionSector> sectors = new ArrayList<StarDetection.StarDetectionSector>();
		int xdiv = 6, ydiv = 6;
		
		if (sx < 500) xdiv = 4;
		if (sy < 500) ydiv = 4;
		
		if (sx < 128) xdiv = 2;
		if (sy < 128) ydiv = 2;
		
		int xmargin = (sx * 4) / (xdiv * 16);
		int ymargin = (sy * 4) / (xdiv * 16);
		
		
		
		for(int square_y = 0; square_y < ydiv; square_y++)
		{
			int y0 = (square_y * sy) / ydiv;
			int y1 = ((square_y + 1) * sy) / ydiv - 1;
		
			y0 -= ymargin;
			if (y0 < 0) y0 = 0;
			y1 += ymargin;
			if (y1 >= sy) y1 = sy - 1;
			
			for(int square_x = 0; square_x < xdiv; square_x++)
			{
				int x0 = (square_x * sx) / xdiv;
				int x1 = ((square_x + 1) * sx) / xdiv - 1;
			
				x0 -= xmargin;
				if (x0 < 0) x0 = 0;
				x1 += xmargin;
				if (x1 >= sx) x1 = sx - 1;
				
				StarDetectionSector sector = new StarDetectionSector(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
				sector.detectStars();
				sectors.add(sector);
			}
		}

		List<ImageStar> result = new ArrayList<ImageStar>();
		boolean doContinue = true;
		
		while(doContinue && result.size() < parameters.getNbStarMax())
		{
			doContinue = false;
			// Essayer sur chaque secteur, on choisissant d'abord les secteur brillants...
			for(StarDetectionSector sector : sectors)
			{
				while(sector.hasNext()) {
					
					doContinue = true;
					
					
					StarCandidate sc = sector.next();
	
					// Agrandir tant qu'on trouve un pct de pixel superieur à l'adu...
					int x0 = sc.x;
					int y0 = sc.y;
					int x1 = x0;
					int y1 = y0;
					
					// Si la valeur n'est plus la même, c'est qu'une autre étoile est dans le coin; on abandonne...
					if (image[x0 + y0 * sx] != sc.energy) {
						continue;
					}
					
					float limit = (float)(sc.energy * this.parameters.getStarGrowIntensityRatio());
					
					int maxSize = this.parameters.getStarMaxSize();
					boolean grow[] = {true,true, true, true};
					while((grow[0] || grow[1] || grow[2] || grow[3]) && (x1 - x0 + 1 < maxSize)&& (y1 - y0 + 1 < maxSize))
					{
						if (grow[0]) {
							if (x0 == 0) {
								grow[0] = false;
							} else {
								// on peut grandir à gauche
								if (hasTopAdu(x0-1, y0, x0-1, y1, limit)) {
									x0--;
								} else {
									grow[0] = false;
								}
							}
						}
						
						if (grow[1]) {
							if (x1 == sx - 1) {
								grow[1] = false;
							} else {
								if (hasTopAdu(x1 + 1, y0, x1+1, y1, limit)) {
									x1++;
								} else {
									grow[1] = false;
								}
							}
						}
					
						if (grow[2]) {
							if (y0 == 0) {
								grow[2] = false;
							} else {
								// on peut grandir à gauche
								if (hasTopAdu(x0, y0 - 1, x1, y0 - 1, limit)) {
									y0--;
								} else {
									grow[2] = false;
								}
							}
						}
						
						if (grow[3]) {
							if (y1 == sy - 1) {
								grow[3] = false;
							} else {
								if (hasTopAdu(x0, y1 + 1, x1, y1 + 1, limit)) {
									y1++;
								} else {
									grow[3] = false;
								}
							}
						}
					}
					
					// Pixel chauds...
					if (x1 == x0 && y1 == y0) continue;
					
					// Zone trop grande... Ignorée
					if (grow[0] || grow[1] || grow[2] || grow[3]) continue;
					
					int largex0 = x0, largey0 = y0, largex1 = x1, largey1 = y1;
					
					// Faire grossir de 3 pixels
					int growPx = 3;
					largex0 -= growPx;
					if (largex0 < 0) largex0 = 0;
					largex1 += growPx;
					if (largex1 >= sx) largex1 = sx - 1;
					largey0 -= growPx;
					if (largey0 < 0) largey0 = 0;
					largey1 += growPx;
					if (largey1 >= sy) largey1 = sy - 1;
					
					double [] bary = getBarycentre(largex0, largey0, largex1, largey1);
					double ecartType = getFWHM(largex0, largey0, largex1, largey1, bary[0], bary[1]);
					
					ImageStar star = new ImageStar();
					star.x = bary[0] * binFactor - img.getSx() / 2.0;
					star.y = bary[1] * binFactor - img.getSy() / 2.0;
					star.energy = bary[2] * binFactor * binFactor;
					star.fwhm = ecartType * binFactor;
					result.add(star);
					
					logger.debug("Star found at " + bary[0]+","+bary[1]+ " E=" + bary[2] + " fwhm=" + ecartType);
					
					clearTopAdu(x0, y0, x1, y1);
					try {
						mask.setRGB(
							(int)Math.ceil(bary[0]), 
							(int)Math.ceil(bary[1]), 0xffffff);
					} catch(Exception e) {
						
					}
					
		//			if (this.image[sc.x+sx*sc.y] < absoluteAduSeuil) {
		//				continue;
		//			}
		//			double err = evaluateStar(sc.x, sc.y, sc.pixel);
		//			if (err < absoluteAduSeuil * 0.1)
		//			{
		//				// fwhm*1.5 pour éviter les détection proches.
		//				substractGauss(gaussCenterX, gaussCenterY, gaussFwhm*1.5, gaussIntensity);
		//				System.err.println("Pixel at " + sc.x+","+sc.y+"   Star at " + gaussCenterX+","+gaussCenterY+"=>" +gaussIntensity+", fwhm="+ gaussFwhm+", mean relative error:" + err);
		//				mask.setRGB(
		//						(int)Math.round(gaussCenterX), 
		//						(int)Math.round(gaussCenterY), 0xffffff);
		//				count ++;
		//			} else {
		//				System.err.println("Rejecting start at " + sc.x +", "+sc.y + " : mean relative error:" + err);
		//			}
					
					break;
				}				
			}
		}
		
		logger.info("Star detected : " + result.size());
		
//		try {
//			ImageIO.write(mask, "png", new File("c:/mask.png"));
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
		
		return result;
	}
}
