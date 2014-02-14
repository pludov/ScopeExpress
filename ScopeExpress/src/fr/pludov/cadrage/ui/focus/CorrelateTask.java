package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import fr.pludov.astrometry.AstrometryProcess;
import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarCorrelationPosition;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.Mosaic.CorrelatedGridPoint;
import fr.pludov.cadrage.focus.correlation.Correlation;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.ui.utils.SwingThreadMonitor;
import fr.pludov.cadrage.utils.DynamicGridPoint;
import fr.pludov.cadrage.utils.DynamicGridPointWithAdu;
import fr.pludov.cadrage.utils.PointMatchAlgorithm;

public class CorrelateTask extends BackgroundTask {
	private static final Logger logger = Logger.getLogger(CorrelateTask.class);
	
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
			double rslt = 0;
			int [] aduSum = so.getAduSumByChannel();
			for(int i : aduSum)
			{
				rslt += i;
			}
			this.adu = rslt;
		}
		
		@Override
		public double getX() {
			return so.getCorrectedX();
		}
		
		@Override
		public double getY() {
			return so.getCorrectedY();
		}
		
		@Override
		public double getAduLevel() {
			return adu;
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
		List<ImageStar> destStars;
		List<Mosaic.CorrelatedGridPoint> referenceStars;

		
		double imagePixSizeInDeg;
		// Il faut multiplier les coordonnées dans la projection par cette valeur pour être à l'échelle de l'image
		SkyProjection imageSkyProjection;
		SkyProjection mosaicProjection;
		// Est ce que la référence est un catalogue (auquel cas, on suppose qu'il contient beaucoup plus d'étoiles, et le filtrage y est plus aggressif)
//		double projectionToImageRatio;
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
			
			double imageSizeInDeg = 57.3 * this.image.getWidth() * parameter.getPixelSize() / (1000 * parameter.getFocal());
			
			// Le 2 correspond à la division due à la matrice de bayer
			imagePixSizeInDeg = imageSizeInDeg * 2 / (this.image.getWidth());


			destStars = getImageStars(this.image);
			if (destStars.isEmpty()) {
				throw new TaskException("Il n'y a pas d'étoiles disponibles pour la correlation");
			}

			imageSkyProjection = new SkyProjection(imagePixSizeInDeg * 3600);
			imageSkyProjection.setCenterx(image.getWidth() / 4.0);
			imageSkyProjection.setCentery(image.getHeight() / 4.0);
			
			mosaicProjection = mosaic.getSkyProjection();
			
			referenceStars = this.mosaic.calcCorrelatedImages();
			
			if (referenceStars.isEmpty()) {
				// Cas simple, c'est la première image de la mosaique
				parameter.setCorrelated(new SkyProjection(imagePixSizeInDeg * 3600));
				return;
			}
			
		} finally {
			SwingThreadMonitor.release();
		}
		
		try {
			double [] [] images = new double[destStars.size()][];
			
			for(int i = 0; i < destStars.size(); ++i)
			{
				ImageStar is = destStars.get(i);
				images[i] = new double[]{2.0 * is.getX(), 2.0 * is.getY(), is.getAduLevel(), 0.0};
			}
			
			AstrometryProcess ap = new AstrometryProcess(image.getWidth(), image.getHeight(), 0, 90, 45);
			ap.setFieldMin(0.3 * imagePixSizeInDeg * Math.sqrt(image.getWidth() * image.getWidth() + image.getHeight() * image.getHeight()));
			ap.setFieldMax(9 * ap.getFieldMin());
			
			SkyProjection imageSkyProjectionResult = ap.doAstrometry(images);
			// Et ensuite, on veut trouver 
			
			// correlation.identity();
			if (imageSkyProjectionResult == null) {
				throw new TaskException("Pas de correlation trouvée");
			}
			
			// à cause de problème de x2 lié au bayer
			imageSkyProjectionResult.setCenterx(imageSkyProjectionResult.getCenterx() / 2);
			imageSkyProjectionResult.setCentery(imageSkyProjectionResult.getCentery() / 2);
			imageSkyProjectionResult.setPixelRad(2 * imageSkyProjectionResult.getPixelRad());
			// Et parce qu'on empile imageSkyProjectionResult avec mosaicProjection
			imageSkyProjectionResult.setTransform(mosaicProjection.getTransform().invert().combine(imageSkyProjectionResult.getTransform()));

			// Calculer la projection de toutes les étoiles de l'image sur la mosaic
			double [] otherX = new double[destStars.size()];
			double [] otherY = new double[destStars.size()];
			double [] tmp2d = new double[2];
			double [] tmp3d = new double[3];
			for(int i = 0; i < destStars.size(); ++i)
			{
				ImageStar otherSo = destStars.get(i);
				tmp2d[0] = otherSo.getX();
				tmp2d[1] = otherSo.getY();
				
				imageSkyProjectionResult.image2dToSky3d(tmp2d, tmp3d);
				mosaicProjection.image3dToImage2d(tmp3d, tmp2d);
				
				otherX[i] = tmp2d[0];
				otherY[i] = tmp2d[1];
			}
			

			double [] referenceX = new double[referenceStars.size()];
			double [] referenceY = new double[referenceStars.size()];
			for(int i = 0; i < referenceStars.size(); ++i)
			{
				Mosaic.CorrelatedGridPoint referenceSo = referenceStars.get(i);
				
				double x = referenceSo.getX();
				double y = referenceSo.getY();
				
				referenceX[i] = x;
				referenceY[i] = y;
			}


			double refToImageRatio = mosaicProjection.getPixelRad() / imageSkyProjectionResult.getPixelRad();
			PointMatchAlgorithm algo = new PointMatchAlgorithm(referenceX, referenceY, otherX, otherY, 18.0 / refToImageRatio);
			
			List<PointMatchAlgorithm.Correlation> correlations = algo.proceed();

			
			
			SwingThreadMonitor.acquire(); 
			try {
				Mosaic mosaic = this.mosaic;
	
				if (mosaic.getMosaicImageParameter(this.image) == null) {
					throw new TaskException("L'image a été retirée de la mosaique");
				}
				if (!mosaic.getSkyProjection().equals(mosaicProjection)) {
					throw new TaskException("La projection de la mosaic a changé");
				}
			
				// Charger les étoiles qui sont dans le champ de l'image
				
				
				if (correlations.size() > 0) {
					for(PointMatchAlgorithm.Correlation c : correlations)
					{
						int refStarId = c.getP1();
						int otherStarId = c.getP2();
						
						Mosaic.CorrelatedGridPoint referenceSo = referenceStars.get(refStarId);
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
					// pour tester avec une transfo orthogonale: 
//					SkyProjection skp = new SkyProjection(correlation.getSkyProjection(), new AffineTransform3D());
//					skp.setTransform(skp.getTransform().rotateY(Math.cos(0.29), Math.sin(0.29)));
//					skp.setTransform(skp.getTransform().rotateZ(Math.cos(0.32), Math.sin(0.32)));
					// skyProjection doit être relatif à l'objet "mosaic..." 
					mosaic.getMosaicImageParameter(this.image).setCorrelated(imageSkyProjectionResult);
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
