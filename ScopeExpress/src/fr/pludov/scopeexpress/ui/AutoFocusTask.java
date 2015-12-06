package fr.pludov.scopeexpress.ui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fr.pludov.external.apt.AptComm;
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
import fr.pludov.scopeexpress.ui.utils.SwingThreadMonitor;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
import fr.pludov.utils.PolynomialFitter;
import fr.pludov.utils.PolynomialFitter.Polynomial;

/** FIXME: il faudrait pouvoir faire un pause / resume ? */
public class AutoFocusTask {

	public static interface Listener {
		// Emis quand le status ou le flag interrupting change
		void onStatusChanged();
	}
	
	AutoFocusParameters parameters;
	
	public static enum Status { 
		Shoot("Photo", Color.orange), 
		Move("Déplacement", Color.orange),
		Analysing("Analyse", Color.orange), 
		ClearingBacklash("Backlash", Color.orange),
		Finished("Terminé", Color.green),
		Error("Erreur", Color.red), 
		Canceled("Annulé", Color.red); 
		
		public final String title;
		public final Color color;
		
		Status(String title, Color color)
		{
			this.title = title;
			this.color = color;
		}
	};
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	public final WeakListenerCollection<Listener> listeners = new WeakListenerCollection<>(Listener.class);
	
	Status status;
	final FocusUi focusUi;
	final Mosaic mosaic;
	
	int passId;
	private int currentCenter;
	int stepId;
	int imageId;
	String error;
	
	Focuser listenedFocuser;
	Focuser.Listener focuserListener;
	Mosaic listenedMosaic;
	MosaicListener mosaicListener;
	
	// Passe => position => images
	Map<Integer, Map<Integer, List<Image>>> images;
	Map<Integer, Integer> passCenter;
	Integer finalPosition;
	
	boolean interrupting;
	
	public AutoFocusTask(Mosaic mosaic, FocusUi focusUi, AutoFocusParameters parameters) {
		this.parameters = parameters;
		this.focusUi = focusUi;
		this.mosaic = mosaic;
		this.images = new TreeMap<Integer, Map<Integer, List<Image>>>();
		this.interrupting = false;
		this.passCenter = new HashMap<>();
		focusUi.getFocuserManager().listeners.addListener(this.listenerOwner, new DeviceManager.Listener() {
			@Override
			public void onDeviceChanged() {
				try {
					setError("Focuser changed");
				} catch (Exception e) {
				}
			}
		});
	}

	private void removeListeners()
	{
		removeFocuserListener();
		removeMosaicListener();
	}
	
	private void removeFocuserListener()
	{
		if (this.listenedFocuser != null) {
			this.listenedFocuser.getListeners().removeListener(this.listenerOwner);
		}
		this.focuserListener = null;
	}

	private void removeMosaicListener()
	{
		if (this.listenedMosaic != null) {
			this.listenedMosaic.listeners.removeListener(this.listenerOwner);
		}
		this.mosaicListener = null;
	}
	
	private void setFocusListener(Focuser focuser, Focuser.Listener listener)
	{
		removeListeners();
		this.listenedFocuser = focuser;
		this.focuserListener = listener;
		this.listenedFocuser.getListeners().addListener(this.listenerOwner, listener);
	}
	
	private void setMosaicListener(Mosaic mosaic, MosaicListener listener)
	{
		removeListeners();
		this.listenedMosaic = mosaic;
		this.mosaicListener = listener;
		mosaic.listeners.addListener(this.listenerOwner, listener);
	}
	
	void start() throws Exception
	{
		passId = 0;
		stepId = 0;
		imageId = 0;
		this.currentCenter = parameters.startPosition;
		clearBacklash();
	}
	
	void setError(String error) throws Exception
	{
		removeListeners();
		this.error = error;
		this.setStatus(Status.Error);
		throw new Exception(error);
	}
	
	private void doInterrupt() throws Exception {
		if (interrupting) {
			removeListeners();
			interrupting = false;
			this.setStatus(Status.Canceled);
			throw new Exception("Canceled");		
		}
	}
	
	Focuser getFocuser() throws Exception
	{
		Focuser focuser = focusUi.getFocuserManager().getConnectedDevice();
		if (focuser == null || !focuser.isConnected()) {
			setError("Focuser not connected");
		}
		return focuser;
	}
	
	// Lance une demande d'interruption. La tache s'arretera à la fin de l'opération en cours
	public void interrupt() {
		if (!interrupting) {
			interrupting = true;
			this.listeners.getTarget().onStatusChanged();
		}
	}
	
	void clearBacklash() throws Exception
	{
		doInterrupt();

		this.passCenter.put(this.passId, this.currentCenter);
		setStatus(Status.ClearingBacklash);
		
		if (parameters.backlash == 0) {
			moveToImagePos();
			return;
		}
		
		Focuser focuser = getFocuser();
		
		int passStart = parameters.getPassPos(this.currentCenter, passId, 0);
		int backlashTarget = passStart - parameters.backlash;
		if (backlashTarget < 0) {
			backlashTarget = 0;
		}
		if (backlashTarget > passStart) {
			moveToImagePos();
			return;
		}
		
		setFocusListener(focuser, new Focuser.Listener() {
			
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
				try {
					getFocuser();
				} catch (Exception e) {
				}
			}
		});
		focuser.moveTo(backlashTarget);
	}
	
	void moveToImagePos() throws Exception
	{
		doInterrupt();

		setStatus(Status.Move);
		
		int passStart = parameters.getPassPos(this.currentCenter, this.passId, this.stepId);

		
		Focuser focuser = getFocuser();

		// si on est hors bound, passer à l'image suivante
		if (passStart < 0 || passStart > focuser.maxStep()) {
			imageShooted(null);
			return;
		}
		
		
		setFocusListener(focuser, new Focuser.Listener() {
			
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
				try {
					getFocuser();
				} catch (Exception e) {
				}
			}
		});
		focuser.moveTo(passStart);
	}
	
	void shootImage() throws Exception
	{
		doInterrupt();

		setStatus(Status.Shoot);

		// FIXME: On attend jusque duration + 20s avant de sortir en erreur.
		setMosaicListener(focusUi.focusMosaic, new MosaicListener() {
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
		if (focusUi.scriptTest != null) {
			new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000 * parameters.photoDuration);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					focusUi.scriptTest.step();
				}
			}.start();
		} else {
			new Thread() {
				@Override
				public void run() {
					try {
						AptComm.getInstance().shoot(parameters.photoDuration);
					} catch (IOException e1) {
						SwingThreadMonitor.acquire();
						try {
							setError("Erreur de shoot: " + e1.getMessage());
						} catch (Exception e) {
						}
						SwingThreadMonitor.release();
					}
				};
			}.start();
		}
	}
	
	void imageShooted(Image shooted) throws Exception
	{
		doInterrupt();

		if (shooted != null) {
			Map<Integer, List<Image>> imagesOfPass = this.images.get(this.passId);
			if (imagesOfPass == null) {
				imagesOfPass = new HashMap<Integer, List<Image>>();
				this.images.put(this.passId, imagesOfPass);
			}
			// FIXME : devrait être enregistré par imageMove
			Integer currPos = parameters.getPassPos(this.currentCenter, this.passId, this.stepId);
			List<Image> imagesOfPos = imagesOfPass.get(currPos);
			if (imagesOfPos == null) {
				imagesOfPos = new ArrayList<Image>();
				imagesOfPass.put(currPos, imagesOfPos);
			}
			imagesOfPos.add(shooted);
		}		
		
		this.stepId ++;
		if (this.stepId >= parameters.getStepCount(this.passId)) {
			this.stepId = 0;
			// Passer à l'étape suivante
			// FIXME : attendre la correlation
			this.imageId++;
			if (this.imageId >= parameters.photoCount) {
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
	
	void endOfPass() throws Exception
	{
		if (!allImagesDone()) {
			doInterrupt();

			setStatus(Status.Analysing);
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
	
	class Interpolation
	{
		Integer minKey, maxKey;
		Polynomial polynome;
		int bestPos;
	}
	
	// Attendre que toutes les images ait été correllées
	void endOfAnalysis() throws Exception
	{
		Interpolation interpolation = getInterpolation(this.passId);

		if (interpolation == null) {
			setError("Pas assez d'images avec des étoiles");
			return;
		}
		
		int bestPos = interpolation.bestPos;
//		int bestPos = (parameters.getPassPos(this.currentCenter, this.passId, 0) + 
//				parameters.getPassPos(this.currentCenter, this.passId, parameters.getStepCount(this.passId) - 1)) / 2;
		
		this.currentCenter = bestPos;
		
		int nextPass = parameters.getNextPass(this.passId);
		if (nextPass == -1) {
			clearFinalBacklash(this.currentCenter);
		} else {
			this.passId = nextPass;
			clearBacklash();
			return;
		}
	}

	/* Retourne l'interpolation en fonction des images déja analysées */ 
	public Interpolation getInterpolation(Integer passId) throws Exception {
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

	void clearFinalBacklash(final int targetPos) throws Exception
	{
		doInterrupt();

		setStatus(Status.ClearingBacklash);
		
		if (parameters.backlash == 0) {
			finalMove(targetPos);
			return;
		}
		
		Focuser focuser = getFocuser();
		
		int passStart = targetPos;
		int backlashTarget = passStart - parameters.backlash;
		if (backlashTarget < 0) {
			backlashTarget = 0;
		}
		if (backlashTarget > passStart) {
			finalMove(targetPos);
			return;
		}
		
		setFocusListener(focuser, new Focuser.Listener() {
			
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
				try {
					getFocuser();
				} catch (Exception e) {
				}
			}
		});
		focuser.moveTo(backlashTarget);

	}
	
	void finalMove(int targetPos) throws Exception
	{
		doInterrupt();

		setStatus(Status.Move);
		
		Focuser focuser = getFocuser();
		
		setFocusListener(focuser, new Focuser.Listener() {
			
			@Override
			public void onMoving() {
			}
			
			@Override
			public void onMoveEnded() {
				removeListeners();
				try {
					setStatus(Status.Finished);
				} catch (Exception e) {
				}
			}
			
			@Override
			public void onConnectionStateChanged() {
				try {
					getFocuser();
				} catch (Exception e) {
				}
			}
		});
		focuser.moveTo(this.currentCenter);
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
//		if (this.status == status) {
//			return;
//		}
		this.status = status;
		this.listeners.getTarget().onStatusChanged();
	}

	public void cancel()
	{
		removeListeners();
		setStatus(Status.Canceled);
	}

	public Map<Integer, List<Image>> getImagesOfPass(Integer passId) {
		
		Map<Integer, List<Image>> map = this.images.get(passId);
		if (map == null) map = Collections.EMPTY_MAP;
		return map;

	}

	public int getPassCenter(int passId) {
		while(passId > 0) {
			Integer val = this.passCenter.get(passId);
			if (val != null) return val;
			passId--;
		}
		return parameters.startPosition;
	}

	public boolean isInterrupting() {
		return this.interrupting;
	}
}
