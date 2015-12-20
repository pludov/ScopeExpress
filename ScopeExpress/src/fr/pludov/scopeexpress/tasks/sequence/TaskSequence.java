package fr.pludov.scopeexpress.tasks.sequence;

import java.io.File;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.shoot.TaskShootDefinition;
import fr.pludov.scopeexpress.ui.FindStarTask;
import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.ui.LoadMetadataTask;

public class TaskSequence extends BaseTask {

	public TaskSequence(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher,
			BaseTaskDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}


	
	@Override
	public TaskSequenceDefinition getDefinition()
	{
		return (TaskSequenceDefinition) super.getDefinition();
	}
	
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		startFilter();
	}
	
	void startFilter() {
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				startFilter();
			}
		});
		ChildLauncher filterWheel = new ChildLauncher(this, getDefinition().filterWheel) {
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					doneFilter();
				} else {
					// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
					setFinalStatus(BaseStatus.Error);
				}
			}
		};
		filterWheel.getTask().setTitle("Filtre");
		filterWheel.start();
		
	}

	void doneFilter() {
		startAutofocus();
	}
	
	void startAutofocus()
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				startAutofocus();
			}
		});
		ChildLauncher autofocus = new ChildLauncher(this, getDefinition().autofocus) {
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					doneAutofocus();
				} else {
					// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
					setFinalStatus(BaseStatus.Error);
				}
			}
		};
		autofocus.getTask().setTitle("autofocus");
		autofocus.start();
	}
	
	void doneAutofocus()
	{
		startShoots();
	}
	
	int imageCount;
	
	void startShoots()
	{
		imageCount = 0;
		nextImage();
	}
	
	void doneShoots()
	{
		setFinalStatus(BaseStatus.Success);
	}
	
	void nextImage()
	{
		if (imageCount >= get(getDefinition().shootCount)) {
			doneShoots();
			return;
		}
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				nextImage();
			}
		});
		imageCount++;
		ChildLauncher shoot = new ChildLauncher(this, getDefinition().shoot) {
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					
					String path = bt.get(TaskShootDefinition.getInstance().fits);
					Image image = focusUi.getApplication().getImage(new File(path));
					
					Mosaic targetMosaic = focusUi.getImagingMosaic();
					
					MosaicImageParameter mip = targetMosaic.addImage(image, ImageAddedCause.AutoDetected);
					

					LoadMetadataTask loadTask = new LoadMetadataTask(targetMosaic, mip);
					focusUi.getApplication().getBackgroundTaskQueue().addTask(loadTask);
					
					FindStarTask task = new FindStarTask(targetMosaic, image);
					focusUi.getApplication().getBackgroundTaskQueue().addTask(task);
					
					
					nextImage();
				} else {
					// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
					setFinalStatus(BaseStatus.Error);
				}
			}
		};
		shoot.getTask().setTitle("Shoot " + imageCount);
		shoot.start();
	}
	
}
