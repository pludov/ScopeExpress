package fr.pludov.scopeexpress.tasks.shoot;

import java.io.File;

import org.apache.log4j.Logger;

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
	private static final Logger logger = Logger.getLogger(TaskShoot.class);
	
	public TaskShoot(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskShootDefinition taskDefinition) {
		super(focusUi, tm, parentLauncher, taskDefinition);
	}

	@Override
	public TaskShootDefinition getDefinition()
	{
		return (TaskShootDefinition)super.getDefinition();
	}
	
	Camera camera;
	boolean shootCanceled; // Pour éviter d'annuler plusieurs fois le même shoot
	
	@Override
	protected void cleanup()
	{
		camera.getListeners().removeListener(this.listenerOwner);
		camera = null;
		shootCanceled = false;
	}
	
	@Override
	public boolean requestPause() {
		if (getStatus() != BaseStatus.Processing) {
			return false;
		}

		if (super.requestPause() && camera != null && !shootCanceled) {
			shootCanceled = true;
			try {
				camera.cancelCurrentShoot();
			} catch (CameraException e) {
				logger.info("Failed to interrupt", e);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void requestCancelation() {
		super.requestCancelation();
		if (hasPendingCancelation() && camera != null && !shootCanceled) {
			shootCanceled = true;
			try {
				camera.cancelCurrentShoot();
			} catch(CameraException e) {
				logger.info("Failed to interrupt", e);
			}
		}
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
				public void onShootInterrupted() {
					if (TaskShoot.this.isPauseRequested()) {
						cleanup();
						doPause(new Runnable() {
							@Override
							public void run() {
								start();
							}
						});
					} else if (TaskShoot.this.hasPendingCancelation()) {
						setFinalStatus(BaseStatus.Canceled);
					} else {
						setFinalStatus(BaseStatus.Error, "Operation canceled");
					}
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
			shootCanceled = false;
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

}
