package fr.pludov.scopeexpress.tasks.sequence;

import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import com.google.gson.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.focus.MosaicListener.*;
import fr.pludov.scopeexpress.openphd.*;
import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.tasks.autofocus.*;
import fr.pludov.scopeexpress.tasks.shoot.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.DeviceManager.*;

/**
 * 
 * La tache sequence:
 *   active le filtre choisi (FIXME: si actif)
 *   fait un autofocus (FIXME: si actif)
 * 
 * if (filter) {
 * 		if (!autoguider.ignore && autoguider.stopForFilterWheel) {
 *          stopAutoguider();
 *      }
 * 		switchFilter(filter);
 * }
 * 
 * 
 * if (!autoguider.ignore && !autoguidre.stopForFocus) {
 *      startAutoGuider();
 * }
 * 
 * if (autofocus == check) {
 * 		checkFocus();
 * 		if (checkFocus.result) {
 * 			if (!autoguider.ignore && autoguidre.stopForFocus) {
 * 				stopAutoguider();
 * 			}
 * 			autofocus();
 * 		}
 * } else if (autofocus == force) {
 * 		autofocus();
 * }
 * 
 * if (!autoguider.ignore && autoguidre.stopForFocus) {
 *      startAutoGuider();
 * }
 * 
 * loop:
 * if (timeToCheckFocus()) {
 * 		checkFocus();
 * 		if (checkFocus.result) {
 * 			if (!autoguider.ignore && autoguidre.stopForFocus) {
 * 				stopAutoguider();
 * 			}
 * 			autofocus();
 * 			if (!autoguider.ignore && autoguidre.stopForFocus) {
 * 				startAutoguider();
 * 			}
 * 		}
 * }
 * 
 * -- démarre la surveillance phd... 
 * camera.shoot();
 * 
 * goto loop;
 * 
 * @author utilisateur
 *
 */
public class TaskSequence extends BaseTask {
	int imageCount;

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
	protected void cleanup() {
		super.cleanup();
		
		unlistenPhd();
	}
	
	@Override
	public void start() {
		setStatus(BaseStatus.Processing);
		imageCount = 0;
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
		InitialFocusHandling focusHandling = get(getDefinition().initialFocusHandling);
		if (focusHandling == null) {
			setFinalStatus(BaseStatus.Error, "initialFocusHandling not set!");
		}
		
		switch(focusHandling)
		{
		case Forced:
			// On appelle interrupt car on ne connait pas l'état php ici (c'est le dernier état en fait...
			interruptGuiderBeforeAutofocus();
			return;
			
		case NotVerified:
			consecutiveCountWithoutChecking = 0;
			startGuiding();
			return;
		case Verified:
			consecutiveCountWithoutChecking = Integer.MAX_VALUE;
			startGuiding();
			return;
		}
		setFinalStatus(BaseStatus.Error, "invalid initialFocusHandling");
	}
	
	void interruptGuiderBeforeAutofocus()
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				interruptGuiderBeforeAutofocus();
			}
		});
		
		// FIXME: faire quelque chose !
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
		consecutiveCountWithoutChecking = 0;
		startGuiding();
	}
	
	void startGuiding()
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				startGuiding();
			}
		});
		if (get(getDefinition().guiderHandling) == GuiderHandling.Activate) {
			ChildLauncher startGuiding = new ChildLauncher(this, getDefinition().guiderStart) {
				@Override
				public void onDone(BaseTask bt) {
					if (bt.getStatus() == BaseStatus.Success) {
						doneGuiding();
					} else {
						// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
						setFinalStatus(BaseStatus.Error);
					}
				}
			};
			startGuiding.getTask().setTitle("start autoguider");
			startGuiding.start();
		} else {
			doneGuiding();
		}
	}
	
	void doneGuiding()
	{
		startShoots();
	}
	
	int consecutiveCountWithoutDithering;
	int consecutiveCountWithoutChecking;
	
	void startShoots()
	{
		initShoots();
	}
	
	// Partie écoute de phd pour surveillance de l'image en cours
	OpenPhdDevice openPhd;
	Timer openPhdRecoveryTimer;
	// Lancé par la supervision phd
	OpenPhdQuery openPhdGetPixelArcSecQuery;
	private ChildLauncher currentShoot;

	void listenPhd()
	{
		// On ne supervise que si le guidage est actif.
		if (get(getDefinition().guiderHandling) != GuiderHandling.Activate) {
			return;
		}
		
		openPhd = focusUi.getGuiderManager().getConnectedDevice();
		if (openPhd == null) {
			setFinalStatus(BaseStatus.Error, "No guider connected");
			return;
		}
		focusUi.getGuiderManager().listeners.addListener(this.listenerOwner, new Listener() {

			@Override
			public void onDeviceChanged() {
				if (focusUi.getGuiderManager().getConnectedDevice() != openPhd) {
					setFinalStatus(BaseStatus.Error, "Guider disconnected");
				}
			}
		});
		openPhd.getListeners().addListener(this.listenerOwner, new IGuiderListener() {
			void startTimer()
			{
				Double interval = get(getDefinition().guiderDriftTolerance);
				if (interval == null || interval < 5) {
					interval = 10.0;
				}
				final double itv = interval;
				openPhdRecoveryTimer = new Timer((int)(1000.0 * itv), new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						logger.warn("Pas de positions correcte du guidage depuis " + itv + " secondes; le guidage a certainement un problème");
						
						if (currentShoot == null) {
							logger.warn("Pas de shoot en cours ??? Erreur interne ?");
							return;
						}
						TaskShoot currentShootTask = (TaskShoot) currentShoot.getTask();
						if (currentShootTask == null) {
							logger.warn("Pas de tache de shoot en cours ??? Erreur interne ?");
							return;
						}
						currentShootTask.requestCancelation(BaseStatus.Aborted);
					}
				});
				openPhdRecoveryTimer.start();
			}
			
			Double arcsecPerPixel;
			
			@Override
			public void onConnectionStateChanged() {
				// Handled by onDeviceChanged
			}
			
			@Override
			public void onEvent(String event, JsonObject message) {
				// Si c'est un message de déconnection, donner un timeout avant erreur ?
				boolean messageIsGood = false;
				
				if (event.equals(OpenPhdQuery.GuideStep)) {
					Double dx = message.has("dx") ? message.get("dx").getAsDouble() : null;
					Double dy = message.has("dy") ? message.get("dy").getAsDouble() : null;
					if (dx != null && dy != null && arcsecPerPixel != null) {
						double dst = arcsecPerPixel * Math.sqrt(dx.doubleValue() * dx.doubleValue() + dy.doubleValue() * dy.doubleValue());
						logger.debug("Distance du guidage: " + dst + " (arcsec)");
						Double seuil = get(getDefinition().maxGuiderDriftArcSec);
						
						if (seuil == null || dst < seuil) {
							messageIsGood = true; 
						}
					}
				}
				
				if (messageIsGood) {
					openPhdRecoveryTimer.restart();
				}
			}
			
			{
				startTimer();
				openPhdGetPixelArcSecQuery = new OpenPhdQuery() {
					@Override
					public void onReply(JsonObject message) {
						arcsecPerPixel = message.get("result").getAsDouble();
					};
					
					@Override
					public void onFailure() {
						setFinalStatus(BaseStatus.Error, "Failed to get arcsec/pixel from phd");
					};
				};
				openPhdGetPixelArcSecQuery.put("method", "get_pixel_scale");

				openPhdGetPixelArcSecQuery.send(openPhd);
			}
		});
	}

	void unlistenPhd()
	{
		// Supprimer le listener openphd
		focusUi.getGuiderManager().listeners.removeListener(this.listenerOwner);
		if (openPhdGetPixelArcSecQuery != null) {
			openPhdGetPixelArcSecQuery.cancel();
			openPhdGetPixelArcSecQuery = null;
		}
		if (openPhdRecoveryTimer != null) {
			openPhdRecoveryTimer.stop();
			openPhdRecoveryTimer = null;
		}
		if (openPhd != null) {
			openPhd.getListeners().removeListener(this.listenerOwner);
			openPhd = null;
		}

		
	}
	
	void initShoots()
	{
		consecutiveCountWithoutDithering = 0;
		nextImage();
	}
	
	void doneShoots()
	{
		setFinalStatus(BaseStatus.Success);
	}
	
	void startCheckFocus()
	{
		ChildLauncher checkFocus = new ChildLauncher(this, getDefinition().focusCheck) {
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					Integer result = bt.get(TaskCheckFocusDefinition.getInstance().passed);
					if (result != null && result.intValue() != 0) {
						consecutiveCountWithoutChecking = 0;
						nextImage();
					} else {
						interruptGuiderBeforeAutofocus();
					}
				} else {
					setFinalStatus(BaseStatus.Error);
				}
			};
		};
		logger.info("Vérification de la mise au point...");
		checkFocus.start();
	}
	
	void nextImage()
	{
		if (imageCount >= get(getDefinition().shootCount)) {
			doneShoots();
			return;
		}

		// Est-ce qu'on doit faire une vérification de focus ?
		Integer focusCheckInterval = get(getDefinition().focusCheckInterval);
		if (focusCheckInterval != null && consecutiveCountWithoutChecking >= focusCheckInterval) {
			startCheckFocus();
			return;
		}
		
		// Est-ce qu'on doit fair un dithering ?
		
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				initShoots();
			}
		});
		listenPhd();

		imageCount++;
		consecutiveCountWithoutChecking++;
		currentShoot = new ChildLauncher(this, getDefinition().shoot) {
			@Override
			public void onDone(BaseTask bt) {
				if (currentShoot != this) {
					return;
				}
				currentShoot = null;
				unlistenPhd();
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
				} else if (bt.getStatus() == BaseStatus.Aborted) {
					logger.warn("Image abandonnée. Nouvel essai");
					imageCount --;
					nextImage();
				} else {
					// FIXME: en cas d'erreur d'une sous-tache, il faudrait pouvoir redémarrer la sous-tache
					setFinalStatus(BaseStatus.Error);
				}
			}
		};
		currentShoot.getTask().setTitle("Shoot " + imageCount);
		currentShoot.start();
	}
}
