package fr.pludov.scopeexpress.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask;
import fr.pludov.scopeexpress.ui.utils.SwingThreadMonitor;
import fr.pludov.scopeexpress.utils.DynamicGridPointWithAdu;
import fr.pludov.scopeexpress.utils.PointMatchAlgorithm;
import fr.pludov.scopeexpress.utils.PointMatchAlgorithm.Correlation;

/**
 * Match stars from the previous image.
 */
public class StarShiftTask extends BackgroundTask {
	private static final Logger logger = Logger.getLogger(StarShiftTask.class);
	
	final Mosaic mosaic;
	final Image image;
	
	public StarShiftTask(Mosaic mosaic, Image image)
	{
		super("Correlation de " + image.getPath().getName());
		this.mosaic = mosaic;
		this.image = image;
	}

	@Override
	protected boolean isReady() {
		for(StarShiftTask task : getQueue().getTasksWithStatus(StarShiftTask.class, Status.Running))
		{
			if (task.mosaic == this.mosaic) {
				// Une autre travaille déjà sur la mosaic... On attend gentillement.
				return false;
			}
		}
		
		for(CorrelateTask task : getQueue().getTasksWithStatus(CorrelateTask.class, Status.Running))
		{
			if (task.mosaic == this.mosaic) {
				// Une autre travaille déjà sur la mosaic... On attend gentillement.
				return false;
			}
		}
		
		// Les recherches d'étoiles en attente ou en cours sont bloquante pour la correlation...
		for(FindStarTask findStarTask : getQueue().getTasksWithStatus(FindStarTask.class, Status.Pending, Status.Running))
		{
			if (findStarTask.getMosaic() == this.mosaic && findStarTask.getImage() == this.image) {
				// Une détection en cours sur la mosaic... On attend gentillement
				return false;
			}
		}

		return super.isReady();
	}
	
	@Override
	public int getResourceOpportunity() {
		// On n'utilise pas de données en cache
		return 0;
	}
	
	static class ImageStar implements DynamicGridPointWithAdu
	{
		StarOccurence so;
		double adu;
		double scale;
		ImageStar(StarOccurence so, double scale)
		{
			this.so = so;
			double rslt = 0;
			int [] aduSum = so.getAduSumByChannel();
			for(int i : aduSum)
			{
				rslt += i;
			}
			this.adu = rslt;
			this.scale = scale;
		}
		
		@Override
		public double getX() {
			return so.getCorrectedX() * scale;
		}
		
		@Override
		public double getY() {
			return so.getCorrectedY() * scale;
		}
		
		@Override
		public double getAduLevel() {
			return adu;
		}
	}
	
	static class ImageStarComparator implements Comparator<ImageStar>
	{
		@Override
		public int compare(ImageStar arg0, ImageStar arg1) {
			return (int)Math.signum(arg0.getAduLevel() - arg1.getAduLevel());
		}
		
	}
	
	private List<ImageStar> getImageStars(Image image, double scale)
	{
		List<ImageStar> imageStars = new ArrayList<ImageStar>();
		Mosaic focus = this.mosaic;
		
		for(Star star : focus.getStars())
		{
			StarOccurence so = focus.getStarOccurence(star, image);
			if (so == null || !so.isAnalyseDone() || !so.isStarFound())
			{
				continue;
			}
			
			ImageStar imageStar = new ImageStar(so, scale);
			
			imageStars.add(imageStar);
		}
		
		return imageStars;
	}

	@Override
	protected void proceed() throws BackgroundTaskCanceledException, Throwable {
		List<List<ImageStar>> previousStarsByImage;
		List<ImageStar> destStars;
		

		SwingThreadMonitor.acquire();
		try {
			checkCorrelationPossible();
			
			previousStarsByImage = new ArrayList<>();
			for(int i = 0; i < 10; ++i) {
				int id = mosaic.getImages().size() - i - 1;
				if (id < 0) break;
				Image previousImage = mosaic.getImages().get(id);
				
				if (previousImage == image) continue;
				double scale = this.image.getWidth() * 1.0 / previousImage.getWidth();
				List<ImageStar> previousStars = getImageStars(previousImage, scale);
				if (!previousStars.isEmpty()) {
					previousStarsByImage.add(previousStars);
				}
			}
			
			if (previousStarsByImage.isEmpty()) {
				throw new TaskException("Il n'y a pas d'étoiles disponibles pour la correlation");
			}

			destStars = getImageStars(this.image, 1.0);
			if (destStars.isEmpty()) {
				throw new TaskException("Il n'y a pas d'étoiles disponibles pour la correlation");
			}

		} finally {
			SwingThreadMonitor.release();
		}
		
		Collections.sort(destStars, new ImageStarComparator());

		int destCount = Math.min(30, destStars.size());
		// Dans l'image cible
		double maxDist = this.image.getWidth() / 15.0;
		
		int bestCorrelation = 0;
		double bestVx = 0, bestVy = 0;
		List<ImageStar> bestPreviousStars = null;

		for(List<ImageStar> previousStars : previousStarsByImage) {
			Collections.sort(previousStars, new ImageStarComparator());
			int previousCount = Math.min(30, previousStars.size());
			
			double [] x1 = new double[previousCount];
			double [] y1 = new double[previousCount];
			for(int i = 0; i < previousCount; ++i) {
				x1[i] = previousStars.get(i).getX();
				y1[i] = previousStars.get(i).getY();
			}
			
			for(int previousId = 0; previousId < previousCount; ++previousId) {
				// Trouver les étoiles dans un rayon de
				ImageStar previousStar = previousStars.get(previousId);
				double previousX = previousStar.getX();
				double previousY = previousStar.getY();
				
				for(int destId = 0; destId < destCount; ++destId) {
					ImageStar destStar = destStars.get(destId);
	
					double v = (previousX - destStar.getX()) * (previousX - destStar.getX())
							+ (previousY - destStar.getY()) * (previousY - destStar.getY());
					if (v < maxDist * maxDist) {
						// Il y a une correlation.
						double vx = destStar.getX() - previousX;
						double vy = destStar.getY() - previousY;
						double [] x2 = new double[destCount];
						double [] y2 = new double[destCount];
						
						for(int i = 0; i < destCount; ++i) {
							x2[i] = destStars.get(i).getX() - vx;
							y2[i] = destStars.get(i).getY() - vy;
						}
						
						PointMatchAlgorithm pma = new PointMatchAlgorithm(x1, y1, x2, y2, 3.5);
						int count = pma.proceed().size();
						if (count > bestCorrelation) {
							bestCorrelation = count;
							bestVx = vx;
							bestVy = vy;
							bestPreviousStars = previousStars;
						}
					}
				}
			}
		}
		
		if (bestCorrelation == 0) {
			throw new TaskException("Pas de correlation");
		}
		
		double [] x1 = new double[bestPreviousStars.size()];
		double [] y1 = new double[bestPreviousStars.size()];
		for(int i = 0; i < bestPreviousStars.size(); ++i) {
			x1[i] = bestPreviousStars.get(i).getX();
			y1[i] = bestPreviousStars.get(i).getY();
		}
		
		double [] x2 = new double[destCount];
		double [] y2 = new double[destCount];
		for(int i = 0; i < destCount; ++i) {
			x2[i] = destStars.get(i).getX() - bestVx;
			y2[i] = destStars.get(i).getY() - bestVy;
		}
		
		PointMatchAlgorithm pma = new PointMatchAlgorithm(x1, y1, x2, y2, 3.5);
		List<Correlation> correlations = pma.proceed();

		SwingThreadMonitor.acquire(); 
		try {
			checkCorrelationPossible();

			for(Correlation c : correlations)
			{
				int refStarId = c.getP1();
				int otherStarId = c.getP2();

				ImageStar ref = bestPreviousStars.get(refStarId);
				ImageStar iStar = destStars.get(otherStarId);

				StarOccurence otherSo = iStar.so;
				
				if (!mosaic.exists(otherSo)) {
					continue;
				}
				
				if (!mosaic.exists(ref.so)) {
					continue;
				}
				
				// ref est soit une image du catalogue, soit une image qui n'est pas dans le catalogue
//				if (ref.so.star == null) {
//					// otherSo devient liée à une image du catalogue
//					otherSo.getStar().setCorrelatedPos(ref.getSky3dPos());
//					otherSo.getStar().setReference(ref.getReference());
//					otherSo.getStar().setMagnitude(ref.getMagnitude());
//					otherSo.getStar().setPositionStatus(StarCorrelationPosition.Reference);
//				} else {
				// On merge juste les stars
				mosaic.mergeStarOccurence(ref.so.getStar(), otherSo);
//				}
			}

			
//			double [] [] images = new double[destStars.size()][];
//			
//			for(int i = 0; i < destStars.size(); ++i)
//			{
//				ImageStar is = destStars.get(i);
//				images[i] = new double[]{2.0 * is.getX(), 2.0 * is.getY(), is.getAduLevel(), 0.0};
//			}
//			
//
//			// Calculer la projection de toutes les étoiles de l'image sur l'image
//			double fwhmSum = 0;
//			for(int i = 0; i < destStars.size(); ++i)
//			{
//				ImageStar otherSo = destStars.get(i);
//				fwhmSum += otherSo.so.getFwhm();
//			}
//			fwhmSum = fwhmSum / (double)destStars.size();
//			
//			// On va mettre dans referenceStars :
//			//   - les étoiles du catalogue
//			//   - les étoiles déjà dans l'image, si on est capable de trouver un rapport adu/magnitude 
//
//			// On va chercher toutes les étoiles du catalogue (sans ajouter celles qui sont déjà connue)
//
//			CorrelateTaskStarMatchingContext references = new CorrelateTaskStarMatchingContext(mosaic, imageMosaicProjectionResult, image.getWidth(), image.getHeight(), 5 * fwhmSum);
//			references.searchStars();
//
//			SwingThreadMonitor.acquire(); 
//			try {
//				references.loadMosaicStars();
//			} finally {
//				SwingThreadMonitor.release();
//			}
//
//			references.correlate(destStars);
			
//			SwingThreadMonitor.acquire(); 
//			try {
//				Mosaic mosaic = this.mosaic;
//	
//				if (mosaic.getMosaicImageParameter(this.image) == null) {
//					throw new TaskException("L'image a été retirée de la mosaique");
//				}
//			
//				// Charger les étoiles qui sont dans le champ de l'image
//				references.mergeStars();
//				
//				// FIXME : rompre les associations éventuelles pour les StarOccurence source ou dest qui n'ont pas été trouvés
//				// pour tester avec une transfo orthogonale: 
//				mosaic.getMosaicImageParameter(this.image).setCorrelated(imageMosaicProjectionResult);
//			} finally {
//				SwingThreadMonitor.release();
//			}
			
			
		} catch(Throwable t) 
		{
			t.printStackTrace();
		} finally {
			SwingThreadMonitor.release();

		}
	}

	private void checkCorrelationPossible() throws TaskException {
		// Vérifier que l'image n'est pas déjà correllée... Sinon, remonter une erreur
		if (!this.mosaic.hasImage(image)) {
			throw new TaskException("L'image a été retirée de la mosaique");
		}
			
		MosaicImageParameter parameter = this.mosaic.getMosaicImageParameter(image);
		if (parameter.isCorrelated()) {
			throw new TaskException("L'image est déjà corellée");
		}
	}

	// Fourni cos et sin et ratio pour passer de this à other
	private static void calcAngleRatio(double thisP1X, double thisP1Y, double thisP2X, double thisP2Y, double thisDst,
								double otherP1X, double otherP1Y, double otherP2X, double otherP2Y, double otherDst,
								double [] calc)
	{
		double Xa = otherP2X - otherP1X;
		double Ya = otherP2Y - otherP1Y;
		double Xb = thisP2X - thisP1X;
		double Yb = thisP2Y - thisP1Y;
		
//		double thisDst = Math.sqrt((thisP2X - thisP1X) * (thisP2X - thisP1X) + (thisP2Y - thisP1Y) * (thisP2Y - thisP1Y));
//		double otherDst = Math.sqrt((otherP2X - otherP1X) * (otherP2X - otherP1X) + (otherP2Y - otherP1Y) * (otherP2Y - otherP1Y));
		
		
		calc[0] = (Xa*Xb+Ya*Yb)/(thisDst*otherDst);
		calc[1] = (Ya*Xb-Xa*Yb)/(thisDst*otherDst);
		calc[2] = thisDst / otherDst;

	}
//	
//	private static void adjustCorrelationParams(double [] correlationCsSnTxTy, 
//								List<PointMatchAlgorithm.Correlation> correlations,
//								List<Mosaic.CorrelatedGridPoint> referenceStarList,
//								List<? extends DynamicGridPoint> otherStarList)
//	{
//		// Pour chaque segment disponible, on va calculer la rotation
//		// On fait la moyenne des angles et on retient cet angle
//		// On fait la moyenne des ratios et on retient ce ratio
//
//		double [] tmpVec = new double[3];
//		double [] sumVec = new double[3];
//		
//		int ptCount = 0;
//		double weight = 0.0;
//		
//		// Ensuite, on calcule la moyenne des translations
//		for(int p1 = 0; p1 < correlations.size(); ++p1)
//		{
//			double thisP1X, thisP1Y, otherP1X, otherP1Y;
//			
//			int thisPt1Index = correlations.get(p1).getP1();
//			thisP1X = referenceStarList.get(thisPt1Index).getX();
//			thisP1Y = referenceStarList.get(thisPt1Index).getY();
//
//			int otherPt1Index = correlations.get(p1).getP2();
//			otherP1X = otherStarList.get(otherPt1Index).getX();
//			otherP1Y = otherStarList.get(otherPt1Index).getY();
//			
//			for(int p2 = p1 + 1; p2 < correlations.size(); ++p2)
//			{
//				double thisP2X, thisP2Y, otherP2X, otherP2Y;
//				
//				int thisPt2Index = correlations.get(p2).getP1();
//				thisP2X = referenceStarList.get(thisPt2Index).getX();
//				thisP2Y = referenceStarList.get(thisPt2Index).getY();
//
//				int otherPt2Index = correlations.get(p2).getP2();
//				otherP2X = otherStarList.get(otherPt2Index).getX();
//				otherP2Y = otherStarList.get(otherPt2Index).getY();
//				
//
//				double thisDst = Math.sqrt((thisP2X - thisP1X) * (thisP2X - thisP1X) + (thisP2Y - thisP1Y) * (thisP2Y - thisP1Y));
//				double otherDst = Math.sqrt((otherP2X - otherP1X) * (otherP2X - otherP1X) + (otherP2Y - otherP1Y) * (otherP2Y - otherP1Y));
//				
//				if (thisDst <= 0.0001) continue;
//				
//				calcAngleRatio(otherP1X, otherP1Y, otherP2X, otherP2Y, otherDst, thisP1X, thisP1Y, thisP2X, thisP2Y, thisDst, tmpVec);
//				double thisWeight = (thisDst + otherDst);
//				
//				sumVec[0] += tmpVec[0] * thisWeight;
//				sumVec[1] += tmpVec[1] * thisWeight;
//				sumVec[2] += tmpVec[2] * thisWeight;
//				
//				ptCount ++;
//				weight += thisWeight;
//			}
//		}
//		
//		if (weight == 0) return;
//		
//		sumVec[0] /= weight;
//		sumVec[1] /= weight;
//		sumVec[2] /= weight;
//		
//		// Renormer cs et sn
//		double csSnNorm = Math.sqrt(sumVec[0] * sumVec[0] + sumVec[1] * sumVec[1]);
//		sumVec[0] /= csSnNorm;
//		sumVec[1] /= csSnNorm;
//		
//		// Multiplier par le ration moyen
//		sumVec[0] *= sumVec[2];
//		sumVec[1] *= sumVec[2];
//		correlationCsSnTxTy[0] = sumVec[0];
//		correlationCsSnTxTy[1] = sumVec[1];
//		
//		double tx = 0, ty = 0;
//		double cs = correlationCsSnTxTy[0];
//		double sn = correlationCsSnTxTy[1];
//		for(int p1 = 0; p1 < correlations.size(); ++p1)
//		{
//			double thisP1X, thisP1Y, otherP1X, otherP1Y;
//			
//			int thisPt1Index = correlations.get(p1).getP1();
//			thisP1X = referenceStarList.get(thisPt1Index).getX();
//			thisP1Y = referenceStarList.get(thisPt1Index).getY();
//
//			int otherPt1Index = correlations.get(p1).getP2();
//			otherP1X = otherStarList.get(otherPt1Index).getX();
//			otherP1Y = otherStarList.get(otherPt1Index).getY();
//			
//
//			double xRotateScale = thisP1X * cs + thisP1Y * sn; 
//			double yRotateScale = thisP1Y * cs - thisP1X * sn; 
//			
//			double dltx = otherP1X - xRotateScale;
//			double dlty = otherP1Y - yRotateScale;
//			
//			tx += dltx;
//			ty += dlty;
//		}
//		tx /= correlations.size();
//		ty /= correlations.size();
//		
//		correlationCsSnTxTy[2] = tx;
//		correlationCsSnTxTy[3] = ty;
//	}
//	
	public Image getImage() {
		return image;
	}

	public Mosaic getMosaic() {
		return mosaic;
	}
}
