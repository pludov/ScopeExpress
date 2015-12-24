package fr.pludov.scopeexpress.tasks.platesolve;

import java.io.File;

import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.shoot.TaskShootDefinition;
import fr.pludov.scopeexpress.ui.CorrelateTask;
import fr.pludov.scopeexpress.ui.FindStarTask;
import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.ui.utils.BackgroundTaskListener;

/**
 * Cette tache prend des photo, trouve des étoile et lance une correlation, jusqu'à ce que la correlaiton ait réussi
 * elle double l'exposition à chaque shoot
 *
 */
public class PlateSolveTask extends BaseTask {


	Mosaic mosaic;
	FocusUi focusUi;
	
	Mosaic listenedMosaic;
	MosaicListener mosaicListener;

	
	public PlateSolveTask(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, PlateSolveTaskDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
		this.focusUi = focusUi;
		mosaic = focusUi.getAlignMosaic();
	}

	@Override
	protected void cleanup() {
		removeListeners();
	}
	
	private void removeListeners()
	{
		removeMosaicListener();
		unlistenCorrelateTask();
	}

	private void setMosaicListener(Mosaic mosaic, MosaicListener listener)
	{
		removeListeners();
		this.listenedMosaic = mosaic;
		this.mosaicListener = listener;
		mosaic.listeners.addListener(this.listenerOwner, listener);
	}
	
	private void removeMosaicListener()
	{
		if (this.listenedMosaic != null) {
			this.listenedMosaic.listeners.removeListener(this.listenerOwner);
		}
		this.mosaicListener = null;
	}
	
	@Override
	public PlateSolveTaskDefinition getDefinition()
	{
		return (PlateSolveTaskDefinition) super.getDefinition();
	}
	
	double exposure;
	CorrelateTask correlate;
	
	@Override
	public void start() {

		setStatus(BaseStatus.Processing);
		exposure = get(getDefinition().startExposure);
		shootImage();
	}

	
	
	void unlistenCorrelateTask()
	{
		if (correlate != null) {
			correlate.listeners.removeListener(this.listenerOwner);
			correlate.abort();
			correlate = null;
		}
	}
	
	void shootImage()
	{
		// Ce mosaic listener va attendre qu'une image réussisse le plate solving
		setMosaicListener(mosaic, new MosaicListener() {
			@Override
			public void imageAdded(Image image, ImageAddedCause cause) {
			}
			@Override
			public void starAnalysisDone(final Image image) {
				if (mosaic.getStarOccurences(image).size() < get(getDefinition().minStarCount)) {
					exposure *= 2.0;
					if (exposure > get(getDefinition().maxExposure)) {
						setFinalStatus(BaseStatus.Error, "Could not get enough stars");
					} else {
						nextShoot();
					}
				} else {
					CorrelateTask correlate = new CorrelateTask(focusUi.getMosaic(), focusUi.getAstrometryParameter().getParameter(), image);
					correlate.listeners.addListener(PlateSolveTask.this.listenerOwner, new BackgroundTaskListener() {
						@Override
						public void onDone() {

							unlistenCorrelateTask();
							MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
							if (mip == null || !mip.isCorrelated()) {
								nextShoot();
							} else {
								// On a réussi. FIXME: mettre ra/dec, où ?
								setFinalStatus(BaseStatus.Success);
							}
						}
					});
					focusUi.getApplication().getBackgroundTaskQueue().addTask(correlate);
				}
			}

			@Override
			public void imageRemoved(Image image, MosaicImageParameter mip) {				
			}

			@Override
			public void starAdded(Star star) {
			}

			@Override
			public void starRemoved(Star star) {
			}

			@Override
			public void starOccurenceAdded(StarOccurence sco) {
			}

			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
			}

			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
			}

			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
			}

			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
			}

			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
			}
		});
		
		nextShoot();
	}
	
	void nextShoot() {
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				nextShoot();
			}
		});
		ChildLauncher shoot = new ChildLauncher(this, getDefinition().shoot) {
			{
				set(PlateSolveTask.this.getDefinition().shootExposure, exposure);
			}
			
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					
					String path = bt.get(TaskShootDefinition.getInstance().fits);
					
//					path = "C:\\APT_Images\\CameraCCD_1\\2014-07-30\\ATL_Bin1x1_s_2014-07-30_23-15-27.fit";
					Image image = focusUi.getApplication().getImage(new File(path));
					mosaic.addImage(image, ImageAddedCause.AutoDetected);
					FindStarTask task = new FindStarTask(mosaic, image);
					focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
					//imageShooted(image);
				} else {
					// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
					setFinalStatus(BaseStatus.Error);
				}
			}
		};
		shoot.getTask().setTitle("Shoot @" + exposure + "s");
		shoot.start();
	}
}
