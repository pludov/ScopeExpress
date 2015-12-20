package fr.pludov.scopeexpress.tasks.shoot;

import java.io.File;

import fr.pludov.scopeexpress.camera.Camera;
import fr.pludov.scopeexpress.camera.CameraException;
import fr.pludov.scopeexpress.camera.RunningShootInfo;
import fr.pludov.scopeexpress.camera.ShootParameters;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.BaseTaskDefinition;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.ui.FocusUi;

public class TaskShoot extends BaseTask {
	
	public TaskShoot(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskShootDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}

	@Override
	public TaskShootDefinition getDefinition()
	{
		return (TaskShootDefinition)super.getDefinition();
	}
	
	Camera camera;
	
	@Override
	protected void cleanup()
	{
		camera.getListeners().removeListener(this.listenerOwner);
		camera = null;
	}
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		try {
			
			camera = focusUi.getCameraManager().getConnectedDevice();
			if (camera == null) {
				setFinalStatus(BaseStatus.Error, "Pas de camera connectée");
				return;
			}
	
			camera.getListeners().addListener(this.listenerOwner, new Camera.Listener() {
				
				@Override
				public void onTempeatureUpdated() {				
				}
				
				@Override
				public void onShootStarted(RunningShootInfo currentShoot) {
					System.out.println("shoot started");
				}
				
				@Override
				public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {
					shootDone(shootInfo, generatedFits);		
				}
				
				@Override
				public void onConnectionStateChanged() {
					if (focusUi.getCameraManager().getConnectedDevice() != camera) {
						setFinalStatus(BaseStatus.Error, "Perte de connection");
					}
				}
			});
			ShootParameters sp = new ShootParameters();
			sp.setBinx(get(getDefinition().bin));
			sp.setBiny(get(getDefinition().bin));
			sp.setExp(get(getDefinition().exposure));
			sp.setPath(get(getDefinition().path));
			sp.setFileName(get(getDefinition().fileName));
			try {
				camera.startShoot(sp);
			} catch(CameraException ce) {
				ce.printStackTrace();
				setFinalStatus(BaseStatus.Error, "Erreur de shoot: " + ce.getMessage());
			}
		} catch(Throwable t) {
			reportError(t);
			throw t;
		}
	}

	void shootDone(RunningShootInfo shootInfo, File generatedFits)
	{
		try {
			if (getStatus() != BaseStatus.Processing) {
				return;
			}
			if (generatedFits != null ){
				set(TaskShootDefinition.getInstance().fits, generatedFits.getPath());
				// On a terminé la photo
				setFinalStatus(BaseStatus.Success);
			} else {
				setFinalStatus(BaseStatus.Error, "Erreur de récupération de l'image");
			}
		} catch(Throwable t) {
			reportError(t);
			throw t;
		}
	}

	@Override
	public void requestCancelation() {
		if (getStatus() != BaseStatus.Processing) {
			return;
		}
		// FIXME: abort shoot !
	}
	
	@Override
	public boolean hasPendingCancelation() {
		return false;
	}
	
}
