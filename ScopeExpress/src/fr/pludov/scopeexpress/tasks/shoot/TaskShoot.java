package fr.pludov.scopeexpress.tasks.shoot;

import java.io.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.*;

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
	boolean shootCanceled; // Pour éviter d'annuler plusieurs fois le même shoot
	
	@Override
	protected void cleanup()
	{
		if (camera != null) {
			camera.getListeners().removeListener(this.listenerOwner);
		}
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
	public void requestCancelation(BaseStatus targetStatus) {
		super.requestCancelation(targetStatus);
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
					logger.debug("Début de la capture");
				}
				
				@Override
				public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {
					shootDone(shootInfo, generatedFits);		
				}
				
				@Override
				public void onShootInterrupted() {
					logger.debug("Capture interrompue");
					if (TaskShoot.this.isPauseRequested()) {
						cleanup();
						doPause(new Runnable() {
							@Override
							public void run() {
								start();
							}
						});
					} else if (TaskShoot.this.hasPendingCancelation()) {
						setFinalStatus(TaskShoot.this.getPendingCancelation());
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
			String path = get(getDefinition().path);
			sp.setPath(path);
			// Appliquer les paramètres
			String fileName = get(getDefinition().fileName);
			sp.setFileName(new FileNameGenerator(focusUi).performFileNameExpansion(path, fileName, this, getDefinition()));
			try {
				camera.startShoot(sp);
			} catch(CameraException ce) {
				ce.printStackTrace();
				setFinalStatus(BaseStatus.Error, "Erreur de shoot: " + ce.getMessage());
			}
			shootCanceled = false;
		} catch(Throwable t) {
			setFinalStatus(BaseStatus.Error, t);
		}
	}

	void shootDone(RunningShootInfo shootInfo, File generatedFits)
	{
		logger.debug("Capture terminée");
		if (generatedFits != null) {
			logger.info("Fichier enregistré : " + generatedFits);
		}
		try {
			if (getStatus() != BaseStatus.Processing) {
				return;
			}
			if (generatedFits != null ){
				logger.info("Fichier généré: " + generatedFits.getPath());
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
