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
	
	private static class ImageStar implements DynamicGridPoint
	{
		StarOccurence so;
		
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
	}
	
	private List<DynamicGridPoint> getImageStars(Image image)
	{
		List<DynamicGridPoint> imageStars = new ArrayList<DynamicGridPoint>();
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
		List<DynamicGridPoint> destStars;
		
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
			boolean hasCorrelatedImage = false;
			for(Image candidates : this.mosaic.getImages())
			{
				MosaicImageParameter candidateParameter = this.mosaic.getMosaicImageParameter(candidates);
				if (candidateParameter.isCorrelated()) {
					hasCorrelatedImage = true;
					break;
				}
			}
			
			if (!hasCorrelatedImage) {
				// Cas simple, c'est la première image de la mosaique
				parameter.setCorrelated(1.0, 0.0, 0.0, 0.0);				
				return;
			}
			
			referenceStars = this.mosaic.calcCorrelatedImages();
			
			if (referenceStars.isEmpty()) {
				throw new TaskException("Il n'y a pas d'étoiles de référence disponibles pour la correlation");
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
			
			
			SwingThreadMonitor.acquire(); 
			try {
				Mosaic mosaic = this.mosaic;
	
				List<Mosaic.CorrelatedGridPoint> referenceStarList = referenceStars;
				List<StarOccurence> otherStarList = new ArrayList<StarOccurence>();
				
				for(Star star : mosaic.getStars())
				{
					StarOccurence otherSo = mosaic.getStarOccurence(star, this.image);
					if (otherSo != null && otherSo.isAnalyseDone() && otherSo.isStarFound())
					{
						otherStarList.add(otherSo);
					}
				}

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
				
				double [] otherX = new double[otherStarList.size()];
				double [] otherY = new double[otherStarList.size()];
				
				for(int i = 0; i < otherStarList.size(); ++i)
				{
					StarOccurence otherSo = otherStarList.get(i);
					otherX[i] = otherSo.getX();
					otherY[i] = otherSo.getY();
				}

				PointMatchAlgorithm algo = new PointMatchAlgorithm(referenceX, referenceY, otherX, otherY, 60);
				
				List<PointMatchAlgorithm.Correlation> correlations = algo.proceed();
				
				if (correlations.size() > 0) {
					for(PointMatchAlgorithm.Correlation c : correlations)
					{
						int refStarId = c.getP1();
						int otherStarId = c.getP2();
						
						Mosaic.CorrelatedGridPoint referenceSo = referenceStarList.get(refStarId);
						StarOccurence otherSo = otherStarList.get(otherStarId);
						
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
					
					mosaic.getMosaicImageParameter(this.image).setCorrelated(correlation.getCs(), correlation.getSn(), correlation.getTx(), correlation.getTy());
				}
			} finally {
				SwingThreadMonitor.release();
			}
			
			
		} catch(Throwable t) 
		{
			t.printStackTrace();
		}
	}

	public Image getImage() {
		return image;
	}

	public Mosaic getMosaic() {
		return mosaic;
	}
}
