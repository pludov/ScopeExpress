package fr.pludov.scopeexpress.ui;

import fr.pludov.io.CameraFrameMetadata;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask;
import fr.pludov.scopeexpress.ui.utils.SwingThreadMonitor;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.BackgroundTaskCanceledException;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.Status;
import fr.pludov.utils.MultiStarFinder;


public class LoadMetadataTask extends BackgroundTask {

	final Mosaic mosaic;
	final MosaicImageParameter mip;
	
	public LoadMetadataTask(Mosaic mosaic, MosaicImageParameter mip)
	{
		super("Chargement des méta données de " + mip.getImage().getPath());
		this.mosaic = mosaic;
		this.mip = mip;
	}
	
	@Override
	protected boolean isReady() {
		// Pas deux chargement sur la même image...
		for(LoadMetadataTask otherFindStar : getQueue().getTasksWithStatus(LoadMetadataTask.class, Status.Running))
		{
			if (otherFindStar.getMosaic() == this.getMosaic() && otherFindStar.getImage() == this.getImage()) {
				return false;
			}
		}
		return super.isReady();
	}

	@Override
	public int getResourceOpportunity() {
		return mip.getImage().hasReadyMetadata() ? 1 : 0;
	}


	@Override
	protected void proceed() throws BackgroundTaskCanceledException, Throwable {
		
		SwingThreadMonitor.acquire();
		try {
			if (mosaic.getMosaicImageParameter(mip.getImage()) != mip)
			{
				throw new BackgroundTaskCanceledException();
			}
			
		} finally {
			SwingThreadMonitor.release();
		}
		// FIXME: on veut:
		//   gain
		//   duration
		//   temperature
		//   bin
		CameraFrameMetadata metadata = mip.getImage().getMetadata();
		
		SwingThreadMonitor.acquire();
		try {
			if (mosaic.getMosaicImageParameter(mip.getImage()) != mip)
			{
				throw new BackgroundTaskCanceledException();
			}

			
		} finally {
			SwingThreadMonitor.release();
		}

	}
	
	public Mosaic getMosaic() {
		return mosaic;
	}
	
	public MosaicImageParameter getImage()
	{
		return mip;
	}
}
