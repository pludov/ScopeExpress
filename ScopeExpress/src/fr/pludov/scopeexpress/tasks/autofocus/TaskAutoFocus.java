package fr.pludov.scopeexpress.tasks.autofocus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fr.pludov.external.apt.AptComm;
import fr.pludov.scopeexpress.focus.Application;
import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.focus.MosaicListener.ImageAddedCause;
import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.focuser.FocuserException;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.ChildLauncher;
import fr.pludov.scopeexpress.tasks.TaskInterruptedException;
import fr.pludov.scopeexpress.tasks.TaskManager;
import fr.pludov.scopeexpress.tasks.shoot.TaskShootDefinition;
import fr.pludov.scopeexpress.ui.AutoFocusParameters;
import fr.pludov.scopeexpress.ui.DeviceManager;
import fr.pludov.scopeexpress.ui.FindStarTask;
import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.ui.utils.SwingThreadMonitor;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
import fr.pludov.utils.PolynomialFitter;
import fr.pludov.utils.PolynomialFitter.Polynomial;

public class TaskAutoFocus extends BaseTask {

	class Interpolation
	{
		Integer minKey, maxKey;
		Polynomial polynome;
		int bestPos;
	}
	

	Mosaic mosaic;
	FocusUi focusUi;
	
	int passId;
	private int currentCenter;
	int stepId;
	int imageId;
	
	// Passe => position => images
	Map<Integer, Map<Integer, List<Image>>> images;
	Map<Integer, Integer> passCenter;
	Integer finalPosition;
	

	Focuser listenedFocuser;
	Focuser.Listener focuserListener;
	Mosaic listenedMosaic;
	MosaicListener mosaicListener;

	// FIXME: ça doit dégager
	// AutoFocusParameters parameters;

	
	public TaskAutoFocus(FocusUi focusUi, TaskManager tm, ChildLauncher parentLauncher, TaskAutoFocusDefinition tafd) {
		super(focusUi, tm, parentLauncher, tafd);
		this.focusUi = focusUi;
		mosaic = focusUi.getMosaic();
		this.images = new TreeMap<Integer, Map<Integer, List<Image>>>();
		this.passCenter = new HashMap<>();
	}

	@Override
	protected void cleanup() {
		removeListeners();
	}
	
	private void removeListeners()
	{
		removeFocuserListener();
		removeMosaicListener();
	}

	private void setFocusListener(Focuser.Listener listener)
	{
		removeListeners();
		this.listenedFocuser = focuser;
		this.focuserListener = listener;
		this.listenedFocuser.getListeners().addListener(this.listenerOwner, listener);
	}
	
	
	private void removeFocuserListener()
	{
		if (this.listenedFocuser != null) {
			this.listenedFocuser.getListeners().removeListener(this.listenerOwner);
		}
		this.focuserListener = null;
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
	public TaskAutoFocusDefinition getDefinition()
	{
		return (TaskAutoFocusDefinition) super.getDefinition();
	}
	

	Focuser focuser;
	
	@Override
	public void start() {

		focuser = focusUi.getFocuserManager().getConnectedDevice();
		if (focuser == null) {
			setFinalStatus(BaseStatus.Error, "Pas de focuser connecté");
		}
		
		// Initialisation du pass center
		// FIXME: pas bon.
		Integer startPosition = get(TaskAutoFocusDefinition.getInstance().initialFocuserPosition);
		if (startPosition == null) {
			startPosition = focuser.position();
		}
		// FIXME: respect des bornes min et max
		this.currentCenter = startPosition;
		this.passCenter.put(0, this.currentCenter);
		setStatus(BaseStatus.Processing);
		
		focusUi.getFocuserManager().listeners.addListener(this.listenerOwner, new DeviceManager.Listener() {
			@Override
			public void onDeviceChanged() {
				setFinalStatus(BaseStatus.Error, "Focuser changed");
			}
		});
		
		clearBacklash();
	}
	
	
	
	void clearBacklash()
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				clearBacklash();
			}
		});

		this.passCenter.put(this.passId, this.currentCenter);
		setStatus(TaskAutoFocusStatus.ClearingBacklash);
		
		if (getBacklash() == 0) {
			moveToImagePos();
			return;
		}
		
		int passStart = getPassPos(this.currentCenter, passId, 0);
		int backlashTarget = passStart - getBacklash();
		if (backlashTarget < 0) {
			backlashTarget = 0;
		}
		if (backlashTarget > passStart) {
			moveToImagePos();
			return;
		}
		
		setFocusListener(new Focuser.Listener() {
			
			@Override
			public void onMoving() {
			}
			
			@Override
			public void onMoveEnded() {
				removeListeners();
				try {
					moveToImagePos();
				} catch (Exception e) {
				}
			}
			
			@Override
			public void onConnectionStateChanged() {
			}
		});
		
		moveFocuser(backlashTarget);
	}
	
	void moveToImagePos()
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				moveToImagePos();
			}
		});

		setStatus(TaskAutoFocusStatus.Move);
		
		int passStart = getPassPos(this.currentCenter, this.passId, this.stepId);

		
		// si on est hors bound, passer à l'image suivante
		if (passStart < 0 || passStart > focuser.maxStep()) {
			imageShooted(null);
			return;
		}
		
		
		setFocusListener(new Focuser.Listener() {
			
			@Override
			public void onMoving() {
			}
			
			@Override
			public void onMoveEnded() {
				removeListeners();
				try {
					shootImage();
				} catch (Exception e) {
				}
			}
			
			@Override
			public void onConnectionStateChanged() {
			}
		});
		moveFocuser(passStart);
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

		// FIXME: On attend jusque duration + 20s avant de sortir en erreur.
		setMosaicListener(mosaic, new MosaicListener() {
			@Override
			public void imageAdded(Image image, ImageAddedCause cause) {
				if (cause == ImageAddedCause.AutoDetected) {
					// C'est bon, on le tien !
					removeListeners();
					try {
						imageShooted(image);
					} catch (Exception e) {
					}
				}
			}
			@Override
			public void starAnalysisDone(Image image) {}

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
				// FIXME: le temps de pause doit être ajusté
				set(TaskAutoFocus.this.getDefinition().shootExposure, 1.0);
			}
			
			@Override
			public void onDone(BaseTask bt) {
				if (bt.getStatus() == BaseStatus.Success) {
					String path = bt.get(TaskShootDefinition.getInstance().fits);
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
		shoot.getTask().setTitle("Shoot " + (1 + stepId + imageId * getStepCount(passId)) + "/" + (getStepCount(passId) * getPhotoCount()));
		shoot.start();
//		
//		
//		// Il faut une tache "Prendre une photo"
//		if (focusUi.scriptTest != null) {
//			new Thread() {
//				@Override
//				public void run() {
//					try {
//						Thread.sleep(1000 * getPassesDefinition().photoDuration);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					focusUi.scriptTest.step();
//				}
//			}.start();
//		} else {
//			new Thread() {
//				@Override
//				public void run() {
//					try {
//						AptComm.getInstance().shoot(getPassesDefinition().photoDuration);
//					} catch (IOException e1) {
//						SwingThreadMonitor.acquire();
//						try {
//							setError("Erreur de shoot: " + e1.getMessage());
//						} catch (Exception e) {
//						}
//						SwingThreadMonitor.release();
//					}
//				};
//			}.start();
//		}
	}
	
	void imageShooted(Image shooted)
	{
		doInterrupt();

		if (shooted != null) {
			Map<Integer, List<Image>> imagesOfPass = this.images.get(this.passId);
			if (imagesOfPass == null) {
				imagesOfPass = new HashMap<Integer, List<Image>>();
				this.images.put(this.passId, imagesOfPass);
			}
			// FIXME : devrait être enregistré par imageMove
			Integer currPos = getPassPos(this.currentCenter, this.passId, this.stepId);
			List<Image> imagesOfPos = imagesOfPass.get(currPos);
			if (imagesOfPos == null) {
				imagesOfPos = new ArrayList<Image>();
				imagesOfPass.put(currPos, imagesOfPos);
			}
			imagesOfPos.add(shooted);
		}		
		
		this.stepId ++;
		if (this.stepId >= getStepCount(this.passId)) {
			this.stepId = 0;
			// Passer à l'étape suivante
			// FIXME : attendre la correlation
			this.imageId++;
			if (this.imageId >= getPhotoCount()) {
				// On passe à la position suivante
				this.imageId = 0;
				
				endOfPass();
				
			} else {
//				shootImage();
				clearBacklash();
			}

		} else {
			moveToImagePos();
		}
		
	}
	
	boolean allImagesDone()
	{
		Map<Integer, List<Image>> imagesOfStep = this.images.get(passId);
		if (imagesOfStep == null) imagesOfStep = Collections.EMPTY_MAP;
		List<Image> allImages = new ArrayList<Image>();
		for(List<Image> imageList : imagesOfStep.values())
		{
			allImages.addAll(imageList);
		}
		for(Image image : allImages)
		{
			if (!mosaic.hasImage(image)) {
				continue;
			}
			MosaicImageParameter mip = mosaic.getMosaicImageParameter(image);
			Boolean starDetectionStatus = mip.getStarDetectionStatus();
			if (starDetectionStatus == null || starDetectionStatus == false) {
				return false;
			}
		}
		return true;
	}
	
	void endOfPass()
	{
		if (!allImagesDone()) {
			doInterrupt();

			setStatus(TaskAutoFocusStatus.Analysing);
			setMosaicListener(this.mosaic, new MosaicListener() {
				@Override
				public void starAnalysisDone(Image image) {
					if (allImagesDone()) {
						try {
							endOfAnalysis();
						} catch (Exception e) {
						}
					}
				}
				
				@Override
				public void starRemoved(Star star) {}
				
				@Override
				public void starOccurenceRemoved(StarOccurence sco) {}
				
				@Override
				public void starOccurenceAdded(StarOccurence sco) {}
				
				@Override
				public void starAdded(Star star) {}
				
				@Override
				public void pointOfInterestRemoved(PointOfInterest poi) {}
				
				@Override
				public void pointOfInterestAdded(PointOfInterest poi) {}
				
				@Override
				public void imageRemoved(Image image, MosaicImageParameter mip) {}
				
				@Override
				public void imageAdded(Image image, ImageAddedCause cause) {}
				
				@Override
				public void exclusionZoneRemoved(ExclusionZone ze) {}
				
				@Override
				public void exclusionZoneAdded(ExclusionZone ze) {}
			});
		} else {
			endOfAnalysis();
		}
	}
	
	Double getFwhm(Image image)
	{
		int starCount = 0;
		double result = 0;
		for(StarOccurence so : mosaic.getStarOccurences(image))
		{
			if (so.isSaturationDetected()) continue;
			if (!so.isStarFound()) continue;
			if (!so.isAnalyseDone()) continue;
			result += so.getFwhm();
			starCount++;
		}
		if (starCount == 0) {
			return null;
		}
		return result / starCount;
	}
	
	
	// Attendre que toutes les images ait été correllées
	void endOfAnalysis()
	{
		Interpolation interpolation = getInterpolation(this.passId);

		if (interpolation == null) {
			setFinalStatus(BaseStatus.Error, "Pas assez d'images avec des étoiles");
		}
		
		int bestPos = interpolation.bestPos;
//		int bestPos = (getPassesDefinition().getPassPos(this.currentCenter, this.passId, 0) + 
//				getPassesDefinition().getPassPos(this.currentCenter, this.passId, getPassesDefinition().getStepCount(this.passId) - 1)) / 2;
		
		this.currentCenter = bestPos;
		
		int nextPass = getNextPass(this.passId);
		if (nextPass == -1) {
			clearFinalBacklash(this.currentCenter);
		} else {
			this.passId = nextPass;
			clearBacklash();
			return;
		}
	}

	/* Retourne l'interpolation en fonction des images déja analysées */ 
	public Interpolation getInterpolation(Integer passId) {
		Interpolation interpolation = new Interpolation();
		
		interpolation.minKey = null;
		interpolation.maxKey = null;
		Map<Integer, Double> bestFwhm = new HashMap<Integer, Double>();

		Map<Integer, List<Image>> imagesByPos = this.images.get(passId);
		if (imagesByPos != null) {
			for(Map.Entry<Integer, List<Image>> entry : imagesByPos.entrySet())
			{
				Integer focuserPos = entry.getKey();
				Double min = null;
				for(Image image : entry.getValue())
				{
					Double fwhm = getFwhm(image);
					if (fwhm != null && (min == null || min > fwhm)) {
						min = fwhm;
					}
				}
				if (min != null) {
					if (interpolation.minKey == null || focuserPos < interpolation.minKey) {
						interpolation.minKey = focuserPos;
					}
					if (interpolation.maxKey == null || focuserPos > interpolation.maxKey) {
						interpolation.maxKey = focuserPos;
					}
					bestFwhm.put(focuserPos, min);
				}
			}
		}

		if (bestFwhm.size() >= 4) {
			PolynomialFitter fitter = new PolynomialFitter(4);
			for(Map.Entry<Integer, Double> entry : bestFwhm.entrySet())
			{
				fitter.addPoint(entry.getKey(), entry.getValue());
			}
			// Trouver le min sur la partie [minKey, maxKey]
			interpolation.polynome = fitter.getBestFit();
			// Il faut résoudre la dérivée
			interpolation.bestPos = (int)Math.round(interpolation.polynome.findMin(interpolation.minKey, interpolation.maxKey));
		} else {
			//setError("Pas assez d'images avec des étoiles");
			interpolation = null;
		}
		return interpolation;
	}

	void clearFinalBacklash(final int targetPos)
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				clearFinalBacklash(targetPos);
			}
		});
		
		setStatus(TaskAutoFocusStatus.ClearingBacklash);
		
		if (getBacklash() == 0) {
			finalMove(targetPos);
			return;
		}
				
		int passStart = targetPos;
		int backlashTarget = passStart - getBacklash();
		if (backlashTarget < 0) {
			backlashTarget = 0;
		}
		if (backlashTarget > passStart) {
			finalMove(targetPos);
			return;
		}
		
		setFocusListener(new Focuser.Listener() {
			
			@Override
			public void onMoving() {
			}
			
			@Override
			public void onMoveEnded() {
				removeListeners();
				try {
					finalMove(targetPos);
				} catch (Exception e) {
				}
			}
			
			@Override
			public void onConnectionStateChanged() {
			}
		});

		moveFocuser(backlashTarget);
	}
	
	void finalMove(final int targetPos)
	{
		doInterrupt();
		doPause(new Runnable() {
			@Override
			public void run() {
				finalMove(targetPos);
			}
		});
		
		setStatus(TaskAutoFocusStatus.Move);
				
		setFocusListener(new Focuser.Listener() {
			
			@Override
			public void onMoving() {
			}
			
			@Override
			public void onMoveEnded() {
				removeListeners();
				finalPosition = targetPos;
				//set(TaskAutoFocusDefinition.getInstance()., value);
				setFinalStatus(BaseStatus.Success);
			}
			
			@Override
			public void onConnectionStateChanged() {
			}
		});
		moveFocuser(this.currentCenter);
	}
	
	private void moveFocuser(int target)
	{
		try {
			focuser.moveTo(target);
		} catch(FocuserException e) {
			e.printStackTrace();
			setFinalStatus(BaseStatus.Error, "Erreur de mouvement:" + e.getMessage());
		}
	}
	
	private int getBacklash()
	{
		return get(getDefinition().backlash);
	}
	
	private int getPhotoCount()
	{
		return get(getDefinition().photoCount);
	}
	
	private int getFocuserRange() {
		return get(getDefinition().initialFocuserRange);
	}

	int getStepCount(int pass) {
		return get(getDefinition().stepCount);
	}

	int getPassPos(int passCenter, int passId, int stepId) {
		int width = getFocuserRange();
		
		return (int)(passCenter - (width / 2) + (width * (long)stepId) / (getStepCount(passId) - 1));
	}

	int getNextPass(int passId) {
		// Une seule passe (mais le code supporte encore plusieurs passes)
		return -1;
	}

	/** Valable uniquement sur status started*/
	public Integer getPassCenter() {
		return passCenter.get(0);
	}

	public Map<Integer, List<Image>> getImagesOfPass(Integer passId) {
		
		Map<Integer, List<Image>> map = this.images.get(passId);
		if (map == null) map = Collections.EMPTY_MAP;
		return map;

	}

}
