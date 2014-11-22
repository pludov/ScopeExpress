package fr.pludov.scopeexpress.ui;

import fr.pludov.io.CameraFrame;
import fr.pludov.scopeexpress.focus.DarkLibrary;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask;
import fr.pludov.scopeexpress.ui.utils.SwingThreadMonitor;
import fr.pludov.utils.MultiStarFinder;
import fr.pludov.utils.StarFinder;

public final class FindStarTask extends BackgroundTask {
	private final Image image;
	CameraFrame frame;
	final Mosaic mosaic;
	
	public FindStarTask(Mosaic mosaic, Image image) {
		super("Recherche d'étoiles dans " + image.getPath().getName());
		this.mosaic = mosaic;
		this.image = image;
	}

	@Override
	protected boolean isReady() {
		// Attendre la fin éventuelle d'une tache de type LoadMetadata
		// Pas deux chargement sur la même image...
		for(LoadMetadataTask otherFindStar : getQueue().getTasksWithStatus(LoadMetadataTask.class, Status.Running))
		{
			if (otherFindStar.getMosaic() == this.getMosaic() && otherFindStar.getImage().getImage() == this.getImage()) {
				return false;
			}
		}
		
		// Pas deux detections sur la même étoiles...
		for(FindStarTask otherFindStar : getQueue().getTasksWithStatus(FindStarTask.class, Status.Running))
		{
			if (otherFindStar.getMosaic() == this.getMosaic() && otherFindStar.getImage() == this.getImage()) {
				return false;
			}
		}
		
		// Pas si il y a une correlation en cours...
		for(CorrelateTask ct : getQueue().getTasksWithStatus(CorrelateTask.class, Status.Running))
		{
			if (ct.getMosaic() == this.getMosaic() && ct.getImage() == this.getImage()) {
				return false;
			}
		}
		
		return super.isReady();
	}
	
	@Override
	public int getResourceOpportunity() {
		return image.hasReadyCameraFrame() ? 1 : 0;
	}
	
	@Override
	protected void proceed() throws BackgroundTaskCanceledException, Throwable {
		
		SwingThreadMonitor.acquire();
		try {
			if (!mosaic.containsImage(image))
			{
				throw new BackgroundTaskCanceledException();
			}
			
		} finally {
			SwingThreadMonitor.release();
		}

		Image darkImage = DarkLibrary.getInstance().getDarkFor(image);
		frame = image.getCameraFrameWithDark(darkImage);
		setPercent(20);
		
		final MultiStarFinder msf = new MultiStarFinder(frame, image, darkImage) {
			@Override
			public void percent(int pct) {
				setPercent(30 + pct * (98 - 30) / 100);
				try {
					checkInterrupted();
				} catch(BackgroundTaskCanceledException ex)
				{
					throw new RuntimeException("stopped");
				}
			}
		};
		frame = null;

		SwingThreadMonitor.acquire();
		try {
			if (!mosaic.containsImage(image))
			{
				throw new RuntimeException("Image discarded");
			}

			for(Star existingStar : mosaic.getStars())
			{
				StarOccurence occurence = mosaic.getStarOccurence(existingStar, image);
				if (occurence == null || !occurence.isAnalyseDone() || !occurence.isStarFound())
				{
					continue;
				}
				msf.getCheckedArea().add(occurence.getStarMask());
			}
		} finally {
			SwingThreadMonitor.release();
		}
		
		setPercent(30);
		
		msf.proceed();
		
		setPercent(98);

		SwingThreadMonitor.acquire();
		try {
			if (!mosaic.containsImage(image))
			{
				throw new RuntimeException("Image discarded");
			}

			for(StarFinder sf : msf.getStars())
			{
				Star star = new Star(sf.getCenterX(), sf.getCenterY(), image);
				mosaic.addStar(star);
				StarOccurence occurence = new StarOccurence(mosaic, image, star);
				occurence.initFromStarFinder(sf);
				mosaic.addStarOccurence(occurence);
			}
		} finally {
			SwingThreadMonitor.release();
		};
	}

	public Image getImage() {
		return image;
	}

	public Mosaic getMosaic() {
		return mosaic;
	}
}