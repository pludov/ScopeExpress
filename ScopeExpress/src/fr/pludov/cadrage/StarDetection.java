package fr.pludov.cadrage;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

public class StarDetection {
	
	// Portion des pixels les plus faibles utilisés pour l'évaluation du fond.
	// Tous ce qui est en dessous de ça est considéré comme noir.
	private double backgroundEvaluationPct;
	
	// Paramètre global de détéction des ADU.
	private double absoluteAduSeuil;
	
	// Nombre de pixels utilisés dans le calculs de la valeur B&W
	// 1 - 2 - 3
	private int binFactor;
	
	private int backgroundSquare = 24;
	
	public StarDetection()
	{
		backgroundEvaluationPct = 0.8;
		absoluteAduSeuil = 0.2;
		binFactor = 2;
	}
	
	
	public StarDetection(StarDetection copy)
	{
		this.backgroundEvaluationPct = copy.backgroundEvaluationPct;
		this.absoluteAduSeuil = copy.absoluteAduSeuil;
		this.binFactor = copy.binFactor;
	}
	
	private static class StarCandidate {
		int x, y;
		double energy;
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
		int nbSquareX = (sx + backgroundSquare - 1) / backgroundSquare;
		int nbSquareY = (sy + backgroundSquare - 1) / backgroundSquare;
		
		if (nbSquareX == 0) nbSquareX = 1;
		if (nbSquareY == 0) nbSquareY = 1;
		
		
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
				
				image[x + sx * y] -= val;
			}
		try {
			ImageIO.write(mask, "png", new File("c:/background.png"));
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	List<StarCandidate> detectCandidates()
	{
		List<StarCandidate> result = new ArrayList<StarDetection.StarCandidate>();
		int id = 0;
		int candidateCount = 0;
		for(int y = 0; y < sy; ++y)
			for(int x = 0; x < sx; ++x)
			{

				float val = image[id++];
//				if (binFactor * x < 528 - 70) continue;
//				if (binFactor * x > 528 + 70) continue;
//				if (binFactor * y < 430 - 70) continue;
//				if (binFactor * y > 430 + 70) continue;

				if (val > absoluteAduSeuil) {
					StarCandidate candidate = new StarCandidate();
					candidate.x = x;
					candidate.y = y;
					candidate.energy = val;
				
					result.add(candidate);
					
					candidateCount ++;
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
		gaussIntensityMin = Math.max(gaussIntensity * 0.5, this.absoluteAduSeuil);
		
		for(int i = 0; i < 8; ++i)
		{
			System.err.println("Before step " + i + " : " + gaussCenterX+"," + gaussCenterY+"=>"+gaussIntensity+", fwhm=" + gaussFwhm + ", mean relative error=" + gaussEvaluation);

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
	
	private boolean hasTopAdu(int x0, int y0, int x1, int y1)
	{
		for(int y = y0; y <= y1; ++y)
			for(int x = x0; x <= x1; ++x)
			{
				int id = x + sx * y;
				if (this.image[id] >= this.absoluteAduSeuil) {
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
				if (this.image[id] >= this.absoluteAduSeuil) {
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
		int nbPix = (int)(array.length * this.backgroundEvaluationPct);
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
				
				rx += (x - x0) * p;
				ry += (y - y0) * p;
				weightSum += p;
			}
		return new double[] {x0 + rx / weightSum, y0 + ry / weightSum, weightSum};
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
	
	// Double adu255 : valeur d'adu pour 255.
	public List<ImageStar> proceed(BufferedImage img, double adu255, int nbStarMax)
	{
		// Construire un tableau avec les valeurs  
		double mul = adu255 / (255.0 * binFactor * binFactor);
		
		sx = img.getWidth() / binFactor;
		sy = img.getHeight() / binFactor;
		
		image = new float[sx * sy];

		
		int id = 0;
		Raster raster = img.getData();
		int [] pixels = null;
		for(int y = 0; y < sy; y ++)
			for(int x = 0; x < sx; x++)
			{
				// Faire la somme des ADU sur x et y
				pixels = raster.getPixels(x * binFactor, y * binFactor, binFactor, binFactor, pixels);
				int sum = 0;
				
				for(int i = 0; i < pixels.length; ++i)
				{
					sum += pixels[i];
				}
				
				image[id ++] = (float)(mul * sum);
			}
		
		background();
		median3();
		
		
		
		// Parcourir le tableau à la recherche des pixels > seuilADU
		List<StarCandidate> candidate = detectCandidates();
		
		// Trier par intensité décroissante
		Collections.sort(candidate, pixelValueComparator());
		
		// placer candidate
		BufferedImage mask = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
		
		List<ImageStar> result = new ArrayList<ImageStar>();
		int count = 0;
		for(StarCandidate sc : candidate)
		{
			// Agrandir tant qu'on trouve un pct de pixel superieur à l'adu...
			int x0 = sc.x;
			int y0 = sc.y;
			int x1 = x0;
			int y1 = y0;
			
			// FIXME: paramètre
			int maxSize = 16;
			boolean grow[] = {true,true, true, true};
			while((grow[0] || grow[1] || grow[2] || grow[3]) && (x1 - x0 + 1 < maxSize)&& (y1 - y0 + 1 < maxSize))
			{
				if (grow[0]) {
					if (x0 == 0) {
						grow[0] = false;
					} else {
						// on peut grandir à gauche
						if (hasTopAdu(x0-1, y0, x0-1, y1)) {
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
						if (hasTopAdu(x1 + 1, y0, x1+1, y1)) {
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
						if (hasTopAdu(x0, y0 - 1, x1, y0 - 1)) {
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
						if (hasTopAdu(x0, y1 + 1, x1, y1 + 1)) {
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
			star.x = bary[0] * binFactor - img.getWidth() / 2.0;
			star.y = bary[1] * binFactor - img.getHeight() / 2.0;
			star.energy = bary[2] * binFactor * binFactor;
			star.fwhm = ecartType * binFactor;
			result.add(star);
			count ++;
			
			System.err.println("Star found at " + bary[0]+","+bary[1]+ " E=" + bary[2] + " fwhm=" + ecartType);
			
			clearTopAdu(x0, y0, x1, y1);
			try {
				mask.setRGB(
					(int)Math.ceil(bary[0]), 
					(int)Math.ceil(bary[1]), 0xffffff);
			} catch(Exception e) {
				
			}
			
			if (count >= nbStarMax) {
				System.err.println("Enough star found.");
				break;
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
			
		}
		
		System.err.println("Star detected : " + count);
//		
//		try {
//			ImageIO.write(mask, "png", new File("c:/mask.png"));
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//		
		return result;
	}
	

	public double getBackgroundEvaluationPct() {
		return backgroundEvaluationPct;
	}

	public void setBackgroundEvaluationPct(double backgroundEvaluationPct) {
		this.backgroundEvaluationPct = backgroundEvaluationPct;
	}

	public double getAbsoluteAdu() {
		return absoluteAduSeuil;
	}

	public void setAbsoluteAdu(double absoluteAdu) {
		this.absoluteAduSeuil = absoluteAdu;
	}

	public int getBinFactor() {
		return binFactor;
	}

	public void setBinFactor(int binFactor) {
		this.binFactor = binFactor;
	}

}
