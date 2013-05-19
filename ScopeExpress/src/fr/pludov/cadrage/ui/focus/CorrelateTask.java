package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.correlation.Correlation;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.ui.utils.SwingThreadMonitor;
import fr.pludov.cadrage.utils.DynamicGridPoint;
import fr.pludov.cadrage.utils.DynamicGridPointWithAdu;
import fr.pludov.cadrage.utils.PointMatchAlgorithm;

public class CorrelateTask extends BackgroundTask {
	final Mosaic mosaic;
	final Image image;
	
	public CorrelateTask(Mosaic mosaic, Image image)
	{
		super("Correlation de " + image.getPath().getName());
		this.mosaic = mosaic;
		this.image = image;
	}

	@Override
	protected boolean isReady() {
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
	
	private static class ImageStar implements DynamicGridPointWithAdu
	{
		StarOccurence so;
		double adu;
		ImageStar(StarOccurence so)
		{
			this.so = so;
		}
		
		@Override
		public double getX() {
			return so.getX();
		}
		
		@Override
		public double getY() {
			return so.getY();
		}
		
		@Override
		public double getAduLevel() {
			double rslt = 0;
			int [] aduSum = so.getAduSumByChannel();
			for(int i : aduSum)
			{
				rslt += i;
			}
			return rslt;
		}
	}
	
	private List<ImageStar> getImageStars(Image image)
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
			
			ImageStar imageStar = new ImageStar(so);
			
			imageStars.add(imageStar);
		}
		
		return imageStars;
	}

	
	@Override
	protected void proceed() throws BackgroundTaskCanceledException, Throwable {
		List<Mosaic.CorrelatedGridPoint> referenceStars;
		List<ImageStar> destStars;
		
		Correlation correlation = new Correlation();

		SwingThreadMonitor.acquire();
		try {
			// Vérifier que l'image n'est pas déjà correllée... Sinon, remonter une erreur
			if (!this.mosaic.hasImage(image)) {
				throw new TaskException("L'image a été retirée de la mosaique");
			}
			
			MosaicImageParameter parameter = this.mosaic.getMosaicImageParameter(image);
			if (parameter.isCorrelated()) {
				throw new TaskException("L'image est déjà corellée");
			}
			
			// Vérifier qu'il y a une image correllée
//			boolean hasCorrelatedImage = false;
//			for(Image candidates : this.mosaic.getImages())
//			{
//				MosaicImageParameter candidateParameter = this.mosaic.getMosaicImageParameter(candidates);
//				if (candidateParameter.isCorrelated()) {
//					hasCorrelatedImage = true;
//					break;
//				}
//			}
//			
			
			referenceStars = this.mosaic.calcCorrelatedImages();
			
			if (referenceStars.isEmpty()) {
				// Cas simple, c'est la première image de la mosaique
				parameter.setCorrelated(1.0, 0.0, 0.0, 0.0);				
				return;
			}
			
			destStars = getImageStars(this.image);
			if (destStars.isEmpty()) {
				throw new TaskException("Il n'y a pas d'étoiles disponibles pour la correlation");
			}
		} finally {
			SwingThreadMonitor.release();
		}
		
		try {
			correlation.correlate(destStars, referenceStars);
			// correlation.identity();
			if (!correlation.isFound()) {
				throw new TaskException("Pas de correlation trouvée");
			}
			
			List<Mosaic.CorrelatedGridPoint> referenceStarList = referenceStars;

			double [] referenceX = new double[referenceStarList.size()];
			double [] referenceY = new double[referenceStarList.size()];
			for(int i = 0; i < referenceStarList.size(); ++i)
			{
				Mosaic.CorrelatedGridPoint referenceSo = referenceStarList.get(i);
				
				double x = referenceSo.getX();
				double y = referenceSo.getY();

				double nvx = correlation.getTx() + x * correlation.getCs() + y * correlation.getSn();
				double nvy = correlation.getTy() + y * correlation.getCs() - x * correlation.getSn();
				
				referenceX[i] = nvx;
				referenceY[i] = nvy;
			}

			double [] otherX = new double[destStars.size()];
			double [] otherY = new double[destStars.size()];
			
			for(int i = 0; i < destStars.size(); ++i)
			{
				ImageStar otherSo = destStars.get(i);
				otherX[i] = otherSo.getX();
				otherY[i] = otherSo.getY();
			}

			PointMatchAlgorithm algo = new PointMatchAlgorithm(referenceX, referenceY, otherX, otherY, 20);
			
			List<PointMatchAlgorithm.Correlation> correlations = algo.proceed();

			double [] correlationParams = new double[] {correlation.getCs(), correlation.getSn(), correlation.getTx(), correlation.getTy()};
			
			for(int i = 0; i < correlationParams.length; ++i)
			{
				if (Double.isNaN(correlationParams[i])) {
					throw new TaskException("Correlation invalide trouvée (erreur interne!)");
				}
			}
			if (correlations.size() > 0) {
				// Procéder à l'ajustement
				adjustCorrelationParams(correlationParams, correlations, referenceStarList, destStars);
				
				for(int i = 0; i < correlationParams.length; ++i)
				{
					if (Double.isNaN(correlationParams[i])) {
						throw new TaskException("Correlation invalide trouvée (erreur interne!)");
					}
				}
			}
			
			SwingThreadMonitor.acquire(); 
			try {
				Mosaic mosaic = this.mosaic;
	
				if (mosaic.getMosaicImageParameter(this.image) == null) {
					throw new TaskException("L'image a été retirée de la mosaique");
				}
				if (correlations.size() > 0) {
					for(PointMatchAlgorithm.Correlation c : correlations)
					{
						int refStarId = c.getP1();
						int otherStarId = c.getP2();
						
						Mosaic.CorrelatedGridPoint referenceSo = referenceStarList.get(refStarId);
						ImageStar iStar = destStars.get(otherStarId);
						StarOccurence otherSo = iStar.so;
						
						if (!mosaic.exists(otherSo)) {
							continue;
						}
						
						if (!mosaic.exists(referenceSo.getStar())) {
							continue;
						}
						
						// On déplacer la starOccurence de other vers ref.
						StarOccurence currenOtherSo = mosaic.getStarOccurence(referenceSo.getStar(), this.image);
						if (currenOtherSo != null) {
							// Déjà ok...
							if (otherSo == currenOtherSo) {
								continue;
							}
						}
						
						mosaic.mergeStarOccurence(referenceSo.getStar(), otherSo);
						
					}
					
					// FIXME : rompre les associations éventuelles pour les StarOccurence source ou dest qui n'ont pas été trouvés
					mosaic.getMosaicImageParameter(this.image).setCorrelated(correlationParams[0], correlationParams[1], correlationParams[2], correlationParams[3]);
				}
			} finally {
				SwingThreadMonitor.release();
			}
			
			
		} catch(Throwable t) 
		{
			t.printStackTrace();
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
	
	private static void adjustCorrelationParams(double [] correlationCsSnTxTy, 
								List<PointMatchAlgorithm.Correlation> correlations,
								List<Mosaic.CorrelatedGridPoint> referenceStarList,
								List<? extends DynamicGridPoint> otherStarList)
	{
		// Pour chaque segment disponible, on va calculer la rotation
		// On fait la moyenne des angles et on retient cet angle
		// On fait la moyenne des ratios et on retient ce ratio

		double [] tmpVec = new double[3];
		double [] sumVec = new double[3];
		
		int ptCount = 0;
		double weight = 0.0;
		
		// Ensuite, on calcule la moyenne des translations
		for(int p1 = 0; p1 < correlations.size(); ++p1)
		{
			double thisP1X, thisP1Y, otherP1X, otherP1Y;
			
			int thisPt1Index = correlations.get(p1).getP1();
			thisP1X = referenceStarList.get(thisPt1Index).getX();
			thisP1Y = referenceStarList.get(thisPt1Index).getY();

			int otherPt1Index = correlations.get(p1).getP2();
			otherP1X = otherStarList.get(otherPt1Index).getX();
			otherP1Y = otherStarList.get(otherPt1Index).getY();
			
			for(int p2 = p1 + 1; p2 < correlations.size(); ++p2)
			{
				double thisP2X, thisP2Y, otherP2X, otherP2Y;
				
				int thisPt2Index = correlations.get(p2).getP1();
				thisP2X = referenceStarList.get(thisPt2Index).getX();
				thisP2Y = referenceStarList.get(thisPt2Index).getY();

				int otherPt2Index = correlations.get(p2).getP2();
				otherP2X = otherStarList.get(otherPt2Index).getX();
				otherP2Y = otherStarList.get(otherPt2Index).getY();
				

				double thisDst = Math.sqrt((thisP2X - thisP1X) * (thisP2X - thisP1X) + (thisP2Y - thisP1Y) * (thisP2Y - thisP1Y));
				double otherDst = Math.sqrt((otherP2X - otherP1X) * (otherP2X - otherP1X) + (otherP2Y - otherP1Y) * (otherP2Y - otherP1Y));
				
				if (thisDst <= 0.0001) continue;
				
				calcAngleRatio(otherP1X, otherP1Y, otherP2X, otherP2Y, otherDst, thisP1X, thisP1Y, thisP2X, thisP2Y, thisDst, tmpVec);
				double thisWeight = (thisDst + otherDst);
				
				sumVec[0] += tmpVec[0] * thisWeight;
				sumVec[1] += tmpVec[1] * thisWeight;
				sumVec[2] += tmpVec[2] * thisWeight;
				
				ptCount ++;
				weight += thisWeight;
			}
		}
		
		if (weight == 0) return;
		
		sumVec[0] /= weight;
		sumVec[1] /= weight;
		sumVec[2] /= weight;
		
		// Renormer cs et sn
		double csSnNorm = Math.sqrt(sumVec[0] * sumVec[0] + sumVec[1] * sumVec[1]);
		sumVec[0] /= csSnNorm;
		sumVec[1] /= csSnNorm;
		
		// Multiplier par le ration moyen
		sumVec[0] *= sumVec[2];
		sumVec[1] *= sumVec[2];
		correlationCsSnTxTy[0] = sumVec[0];
		correlationCsSnTxTy[1] = sumVec[1];
		
		double tx = 0, ty = 0;
		double cs = correlationCsSnTxTy[0];
		double sn = correlationCsSnTxTy[1];
		for(int p1 = 0; p1 < correlations.size(); ++p1)
		{
			double thisP1X, thisP1Y, otherP1X, otherP1Y;
			
			int thisPt1Index = correlations.get(p1).getP1();
			thisP1X = referenceStarList.get(thisPt1Index).getX();
			thisP1Y = referenceStarList.get(thisPt1Index).getY();

			int otherPt1Index = correlations.get(p1).getP2();
			otherP1X = otherStarList.get(otherPt1Index).getX();
			otherP1Y = otherStarList.get(otherPt1Index).getY();
			

			double xRotateScale = thisP1X * cs + thisP1Y * sn; 
			double yRotateScale = thisP1Y * cs - thisP1X * sn; 
			
			double dltx = otherP1X - xRotateScale;
			double dlty = otherP1Y - yRotateScale;
			
			tx += dltx;
			ty += dlty;
		}
		tx /= correlations.size();
		ty /= correlations.size();
		
		correlationCsSnTxTy[2] = tx;
		correlationCsSnTxTy[3] = ty;
	}
	
	public Image getImage() {
		return image;
	}

	public Mosaic getMosaic() {
		return mosaic;
	}
}
