package fr.pludov.scopeexpress.tasks.autofocus;

import java.io.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.MosaicListener.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;

public class TaskCheckFocus extends BaseTask {

	Mosaic mosaic;
	FocusUi focusUi;

	Mosaic listenedMosaic;
	MosaicListener mosaicListener;
	
	Image shooted;

	public TaskCheckFocus(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskCheckFocusDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
		this.focusUi = focusUi;
		mosaic = focusUi.getFocusMosaic();
	}


	@Override
	protected void cleanup() {
		removeListeners();
	}
	
	private void removeListeners()
	{
		removeMosaicListener();
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
	public TaskCheckFocusDefinition getDefinition()
	{
		return (TaskCheckFocusDefinition) super.getDefinition();
	}
	

	@Override
	public void start() {
		shootImage();
	}
	
	void shootImage()
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				shootImage();
			}
		});

		setStatus(TaskAutoFocusStatus.Shoot);

		shooted = null;
		// FIXME: On attend jusque duration + 20s avant de sortir en erreur.
		setMosaicListener(mosaic, new MosaicListener() {
			@Override
			public void imageAdded(Image image, ImageAddedCause cause) {
//				if (cause == ImageAddedCause.AutoDetected) {
//					// C'est bon, on le tien !
//					removeListeners();
//					try {
//						imageShooted(image);
//					} catch (Exception e) {
//					}
//				}
			}
			@Override
			public void starAnalysisDone(Image image) {
				if (image == shooted) {
					removeListeners();
					checkResult();
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
		
		ChildLauncher shoot = new ChildLauncher(this, getDefinition().shoot) {
			{
				// FIXME: le temps de pause doit être ajusté !!!
				// set(TaskAutoFocus.this.getDefinition().shootExposure, 1.0);
				set(TaskCheckFocus.this.getDefinition().shootKind, ShootKind.TestExposure);
			}
			
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					try {
						String path = bt.get(TaskShootDefinition.getInstance().fits);
						Image image = focusUi.getApplication().getImage(new File(path));
						mosaic.addImage(image, ImageAddedCause.AutoDetected);
						
						shooted = image;
						logger.info("Analyse des étoiles...");
						FindStarTask task = new FindStarTask(mosaic, image);
						focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
					} catch(Throwable t) {
						setFinalStatus(BaseStatus.Error, t);
						
					}
				} else {
					// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
					setFinalStatus(BaseStatus.Error);
				}
			}
		};
		shoot.getTask().setTitle("Shoot de vérification");
		shoot.start();
	}
	
	void checkResult()
	{
		Double fwhm = TaskAutoFocus.getFwhm(mosaic, shooted);
		logger.info("FWHM: " + fwhm);
		
		if (fwhm != null) {
			set(getDefinition().passed, fwhm <= get(getDefinition().fwhmSeuil) ? 1 : 0);
		}
		
		setFinalStatus(fwhm != null ? BaseStatus.Success : BaseStatus.Error);
	}
}
