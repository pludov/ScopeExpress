package fr.pludov.scopeexpress.ui.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicImageParameterListener;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.ui.FocusUiScopeManager;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.DegConverter;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.EnumConverter;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.HourMinSecConverter;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
import fr.pludov.utils.VecUtils;


public class AstrometryParameterPanel extends AstrometryParameterDesign {
	private static final Logger logger = Logger.getLogger(AstrometryParameterPanel.class);
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	public static enum AstrometryParameterMode {
		AutomaticWithBadAlign("A utiliser avant l'alignement polaire (jusqu'à 15° d'écart)", 15.0),
		AutomaticWithPreciseAlign("Considère que la focale ne bouge pas et que le téléscope est bien aligné (erreur d'alignement < 1 degrés)", 1.0),
		Manual("Position et echelle entrée à la main", 0.0);
		
		
		AstrometryParameterMode(String details, double maxPolarErrorDeg)
		{
			this.details = details;
			this.maxPolarErrorDeg = maxPolarErrorDeg;
		}
		
		double maxPolarErrorDeg;
		String details = "";
	}
	
	public static final double diagAbsMin = 0.05;
	public static final double diagAbsMax = 4;
	
	public static class AstrometryParameter {
		AstrometryParameterMode mode;
		double diagMin, diagMax;
		double ra, dec, ray;
		
		public AstrometryParameter()
		{
			this.setMode(AstrometryParameterMode.AutomaticWithBadAlign);
			this.diagMin = diagAbsMin;
			this.diagMax = diagAbsMax;
			this.ra = 0;
			this.dec = 90;
			this.ray = 180;
		}
		

		AstrometryParameter(AstrometryParameter copy)
		{
			this.mode = copy.mode;
			this.diagMax = copy.diagMax;
			this.diagMin = copy.diagMin;
			this.ra = copy.ra;
			this.dec = copy.dec;
			this.ray = copy.ray;
		}
		
		
		// En auto, initialement, le rayon et la diagonale sont non fixe
		// 
		// En auto, à chaque mvt du téléscope, le rayon doit augmenter
		//
		// En auto, suite à un positionnement (la dernière image qui devient correllée):
		//     on marque cette position et le rayon devient 0
		//
		
		public AstrometryParameterMode getMode() {
			return mode;
		}
		
		public void setMode(AstrometryParameterMode mode) {
			this.mode = mode;
		}
		
		public double getDiagMin() {
			return diagMin;
		}
		public void setDiagMin(double diagMin) {
			this.diagMin = diagMin;
		}
		public double getDiagMax() {
			return diagMax;
		}
		public void setDiagMax(double diagMax) {
			this.diagMax = diagMax;
		}
		public double getRa() {
			return ra;
		}
		public void setRa(double ra) {
			this.ra = ra;
		}
		public double getDec() {
			return dec;
		}
		public void setDec(double dec) {
			this.dec = dec;
		}
		public double getRay() {
			return ray;
		}
		public void setRay(double ray) {
			this.ray = ray;
		}
	}
	
	Mosaic mosaic;
	final InputOutputHandler<AstrometryParameter> ioHandler;
	final AstrometryParameter parameter;
	EnumConverter<AstrometryParameter, AstrometryParameterMode> modeConverter;
	HourMinSecConverter<AstrometryParameter> raConverter;
	DegConverter<AstrometryParameter> decConverter;
	DegConverter<AstrometryParameter> rayonConverter;
	
	DegConverter<AstrometryParameter> diagMinConverter;
	DegConverter<AstrometryParameter> diagMaxConverter;
	
	public AstrometryParameterPanel() {
		super();
		ioHandler = new InputOutputHandler<AstrometryParameterPanel.AstrometryParameter>();
		scopeManager = null;
		modeConverter = new EnumConverter<AstrometryParameter, AstrometryParameterMode>(modeSelector, AstrometryParameterMode.values()){

			@Override
			AstrometryParameterMode getFromParameter(AstrometryParameter parameters) {
				return parameters.getMode();
			}

			@Override
			void setParameter(AstrometryParameter parameters, AstrometryParameterMode content) throws Exception {
				parameters.setMode(content);
				boolean canEdit = content == AstrometryParameterMode.Manual;
				raText.setEnabled(canEdit);
				decText.setEnabled(canEdit);
				rayonText.setEnabled(canEdit);
				fieldMinText.setEnabled(canEdit);
				fieldMaxText.setEnabled(canEdit);

				updateAutoMode();
			}
		};
		
		raConverter = new HourMinSecConverter<AstrometryParameter>(this.raText, this.raErrorLbl, null) {
			
			@Override
			public void setParameter(AstrometryParameter parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				parameters.setRa(content);
			}
			
			@Override
			public Double getFromParameter(AstrometryParameter parameters) {
				return parameters.getRa();
			}
		};
		
		decConverter = new DegConverter<AstrometryParameter>(this.decText, this.decErrorLbl, null) {
			@Override
			public void setParameter(AstrometryParameter parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				parameters.setDec(content);
			}
			
			@Override
			public Double getFromParameter(AstrometryParameter parameters) {
				return parameters.getDec();
			}
		};
		
		rayonConverter = new DegConverter<AstrometryParameter>(this.rayonText, this.rayonErrorLbl, null) {
			@Override
			public void setParameter(AstrometryParameter parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				parameters.setRay(content);
			}
			
			@Override
			public Double getFromParameter(AstrometryParameter parameters) {
				return parameters.getRay();
			}
		};

		diagMinConverter = new DegConverter<AstrometryParameter>(this.fieldMinText, this.fieldMinErrorLbl, null) {
			@Override
			public void setParameter(AstrometryParameter parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				parameters.setDiagMin(content);
			}
			
			@Override
			public Double getFromParameter(AstrometryParameter parameters) {
				return parameters.getDiagMin();
			}
		}; 

		diagMaxConverter = new DegConverter<AstrometryParameter>(this.fieldMaxText, this.fieldMaxErrorLbl, null) {
			@Override
			public void setParameter(AstrometryParameter parameters, Double content) throws Exception {
				if (content == null) throw new Exception("Obligatoire");
				parameters.setDiagMax(content);
			}
			
			@Override
			public Double getFromParameter(AstrometryParameter parameters) {
				return parameters.getDiagMax();
			}
		}; 
		
		
		ioHandler.init(new InputOutputHandler.Converter[] {
				modeConverter,
				raConverter,
				decConverter,
				rayonConverter,
				diagMinConverter,
				diagMaxConverter
		});
		
		this.parameter = new AstrometryParameter();

		ioHandler.loadParameters(this.parameter);
		
		updateAutoMode();
	}
	
	public static class ScopeState {
		long startTime;
		boolean connected;
		double ra, dec;
	}
	
	FocusUiScopeManager scopeManager;
	Scope currentScope;
	/** La liste des état du téléscope depuis le début */
	final LinkedList<ScopeState> scopeState = new LinkedList<ScopeState>();
	
	private ScopeState getCurrentScopeState()
	{
		ScopeState sc;
		if (scopeState.isEmpty()) {
			sc = new ScopeState();
			sc.connected = false;
			sc.startTime = System.currentTimeMillis();
			sc.ra = 0;
			sc.dec = 0;
			scopeState.add(sc);
		} else {
			sc = scopeState.getLast();
		}
		return sc;
	}
	
	private ScopeState newScopeState()
	{
		ScopeState current = getCurrentScopeState();
		ScopeState sc = new ScopeState();
		sc.connected = current.connected;
		sc.dec = current.dec;
		sc.ra = current.ra;
		sc.startTime = System.currentTimeMillis();
		scopeState.add(sc);
		return sc;
	}
	
	private List<ScopeState> getScopeStateSince(long when)
	{
		List<ScopeState> result = new ArrayList<ScopeState>();
		ScopeState possibleFirst = null;
		for(ScopeState s : scopeState)
		{
			if (s.startTime >= when) {
				if (possibleFirst != null) {
					result.add(possibleFirst);
				}
				result.add(s);
				possibleFirst = null;
			} else {
				if (result.isEmpty()) {
					possibleFirst = s;
				} else {
					possibleFirst = null;
				}
			}
		}
		return result;
	}
	
	public void setScopeManager(FocusUiScopeManager manager)
	{
		if (this.scopeManager != null && this.scopeManager != manager) {
			this.scopeManager.listeners.removeListener(this.listenerOwner);
		}
		
		this.scopeManager = manager;
		manager.listeners.addListener(this.listenerOwner, new FocusUiScopeManager.Listener() {
			
			@Override
			public void onScopeChanged() {
				setCurrentScope(scopeManager.getScope());
			}
		});
		
		setCurrentScope(scopeManager.getScope());
	}
	
	private void setCurrentScope(Scope scope)
	{
		if (currentScope != null) {
			currentScope.getListeners().removeListener(this.listenerOwner);
		}
		currentScope = scope;
		if (scope != null) {
			ScopeState sc = newScopeState();
			sc.connected = scope.isConnected();
			sc.ra = scope.getRightAscension() * 15;
			sc.dec = scope.getDeclination();
			
			updateAutoMode();
			
			currentScope.getListeners().addListener(this.listenerOwner, new Scope.Listener() {
				
				@Override
				public void onCoordinateChanged() {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							scopeMoved();	
						}
					});
				}
				
				@Override
				public void onConnectionStateChanged() {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							scopeConnectionStateChanged();		
						}
					});
					
				}
			});
		} else {
			ScopeState sc = newScopeState();
			sc.connected = false;
			sc.ra = 0;
			sc.dec = 0;
			
			updateAutoMode();
		}
	}
	
	private void scopeMoved() {
		if (currentScope == null) return;
		// FIXME : ra/dec en quelle unité ?
		double newRa = currentScope.getRightAscension() * 15;
		double newDec = currentScope.getDeclination();
		ScopeState state = getCurrentScopeState();
		if (state.ra == newRa && state.dec == newDec) {
			return;
		}
		ScopeState newState = newScopeState();
		newState.ra = newRa;
		newState.dec = newDec;
		
		updateAutoMode();
		
	}
	
	private void scopeConnectionStateChanged() {
		if (currentScope == null) return; 
		boolean isConnected = currentScope.isConnected();
		ScopeState state = getCurrentScopeState();
		if (state.connected == isConnected) return;
		ScopeState newState = newScopeState();
		newState.connected = isConnected;
		
		updateAutoMode();
	}
	
	private List<Image> autoAddedImages = new ArrayList<Image>();
	private Map<Image, Long> imageAddTime = new HashMap<Image, Long>();
	private MosaicImageParameter lastCorrelated;
	
	private void imageRemoved(final Image img)
	{
		for(int i = 0; i < autoAddedImages.size(); ++i) {
			if (autoAddedImages.get(i) == img) {
				autoAddedImages.remove(i);
				break;
			}
		}
		imageAddTime.remove(img);
		if (lastCorrelated != null && lastCorrelated.getImage() == img) {
			lastCorrelated = null;
		}
	}

	private boolean isLastCorrelated(final Image img)
	{
		for(int i = 0; i < autoAddedImages.size(); ++i)
		{
			if (autoAddedImages.get(i) == img) {
				for(int j = i + 1; j < autoAddedImages.size(); ++j)
				{
					Image otherImage = autoAddedImages.get(j);
					if (mosaic.getMosaicImageParameter(otherImage).isCorrelated()) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	
	private void imageAdded(final Image img)
	{
		autoAddedImages.add(img);
		imageAddTime.put(img, System.currentTimeMillis());
		
		final MosaicImageParameter mip = mosaic.getMosaicImageParameter(img);
		mip.listeners.addListener(this.listenerOwner, new MosaicImageParameterListener() {
			
			@Override
			public void onPixelSizeChanged() {
			}
			
			@Override
			public void onFocalChanged() {
			}

						
			@Override
			public void correlationStatusUpdated() {
				// Si elle est correllée
				if (mip.isCorrelated()) {
					if (isLastCorrelated(mip.getImage())) {
						lastCorrelated = mip;
						updateAutoMode();
					}
				}
			}
		});
		if (mip.isCorrelated()) {
			if (isLastCorrelated(mip.getImage())) {
				lastCorrelated = mip;
				updateAutoMode();
			}
		}
	}

	private double [] getSky3dForImagePos(MosaicImageParameter mip, double [] imgPos)
	{
		double [] sky3dPos = new double[3];

		Image img = mip.getImage();
		
		mip.getProjection().image2dToSky3d(new double[]{0.5 * img.getWidth() * imgPos[0], 0.5 * img.getHeight() * imgPos[1]},
											sky3dPos);

		return sky3dPos;
	}

	private void printAutoModeValues()
	{
		raConverter.loadParameter(ioHandler);
		decConverter.loadParameter(ioHandler);
		rayonConverter.loadParameter(ioHandler);
		diagMinConverter.loadParameter(ioHandler);
		diagMaxConverter.loadParameter(ioHandler);
	}
	
	
	private void updateAutoMode()
	{
		if (parameter.getMode() == AstrometryParameterMode.Manual) return;
		try {
			ScopeState currentScopeState = getCurrentScopeState();
			
			long currentTime = System.currentTimeMillis();
			if (lastCorrelated == null || !lastCorrelated.isCorrelated()) {
				lastCorrelated = null;
				
				// Téléscope pas connecté à ce moment là... On ne sait rien !
				if (currentScopeState.connected) {
					parameter.setRa(currentScopeState.ra);
					parameter.setDec(currentScopeState.dec);
					parameter.setRay(parameter.getMode().maxPolarErrorDeg);
				} else {
					double [] raDecZenith = SkyProjection.convertAltAzToRaDec(new double[]{90.0, 90.0}, currentTime);
					parameter.setRa(raDecZenith[0]);
					parameter.setDec(raDecZenith[1]);
					parameter.setRay(90.0);
				}
				parameter.setDiagMin(diagAbsMin);
				parameter.setDiagMax(diagAbsMax);
				return;
			}
			
			// uniquement si automatique
	
			double [] sky3dCenter = getSky3dForImagePos(lastCorrelated, new double[]{0.5, 0.5});
			double [] sky3dRaDec = new double[2];
			SkyProjection.convert3DToRaDec(sky3dCenter, sky3dRaDec);
			double [] sky3d00 = getSky3dForImagePos(lastCorrelated, new double[]{0.0, 0.0});
			double [] sky3d11 = getSky3dForImagePos(lastCorrelated, new double[]{1.0, 1.0});
			
			double diagRad = SkyProjection.sky3dDst2Rad(VecUtils.norm(VecUtils.sub(sky3d00, sky3d11)));
			double diagDeg = diagRad * 180 / Math.PI;

			Long imageStartTime = this.imageAddTime.get(lastCorrelated.getImage());
			if (imageStartTime == null) return;
			boolean wasAlwaysConnected = true;
			List<ScopeState> states = getScopeStateSince(imageStartTime);
			if (states.isEmpty() || states.get(0).startTime > imageStartTime) {
				wasAlwaysConnected = false;
			} else {
				for(ScopeState scopeState : states)
				{
					if (!scopeState.connected) {
						wasAlwaysConnected = false;
					}
				}
			}
			
			// Calculer la distance théorique/constatée
			if (wasAlwaysConnected) {
				double [] scopeTarget = new double[3];
				SkyProjection.convertRaDecTo3D(new double[]{states.get(0).ra, states.get(0).dec}, scopeTarget);
				double radDst = SkyProjection.sky3dDst2Rad(VecUtils.norm(VecUtils.sub(sky3dCenter, scopeTarget)));
				logger.info("Ecart ascom/correlation:" + Utils.formatDegMinSec(radDst * 180 / Math.PI));
				
				// Compter les mouvements faits depuis.
				double totalPathRad = 0.0;
				for(int i = 1; i < states.size(); ++i) {
					if (states.get(i).startTime > imageStartTime) {
						double [] posPrev = new double[3];
						SkyProjection.convertRaDecTo3D(new double[]{states.get(i - 1).ra, states.get(i - 1).dec}, posPrev);
						double [] posCurr = new double[3];
						SkyProjection.convertRaDecTo3D(new double[]{states.get(i).ra, states.get(i).dec}, posCurr);
						double pathRad = SkyProjection.sky3dDst2Rad(VecUtils.norm(VecUtils.sub(posPrev, posCurr)));
						totalPathRad += pathRad;
					}
				}
				
				// C'est pas la peine d'aller plus loin si on s'est déplacé de tout ça !
				if (totalPathRad > Math.PI) totalPathRad = Math.PI;
				
				double errorDist = (currentTime - imageStartTime) / (86400000.0 / 2);
				if (errorDist <0) errorDist = 1.0;
				errorDist += totalPathRad / Math.PI;
				if (errorDist > 1.0) errorDist = 1.0;
				
				double rayDeg = (radDst * 180/Math.PI) + parameter.getMode().maxPolarErrorDeg * errorDist;
				if (rayDeg > parameter.getMode().maxPolarErrorDeg) rayDeg = parameter.getMode().maxPolarErrorDeg;
				
				if (currentScopeState.connected) {
					parameter.setRa(currentScopeState.ra);
					parameter.setDec(currentScopeState.dec);
					parameter.setRay(rayDeg);
				} else {
					parameter.setRa(sky3dRaDec[0]);
					parameter.setDec(sky3dRaDec[1]);
					parameter.setRay(rayDeg);
				}
			} else {
				
				// Téléscope pas connecté à ce moment là... On ne sait rien !
				if (currentScopeState.connected) {
					parameter.setRa(currentScopeState.ra);
					parameter.setDec(currentScopeState.dec);
					parameter.setRay(parameter.getMode().maxPolarErrorDeg);
				} else {
					double [] raDecZenith = SkyProjection.convertAltAzToRaDec(new double[]{90.0, 90.0}, currentTime);
					parameter.setRa(raDecZenith[0]);
					parameter.setDec(raDecZenith[1]);
					parameter.setRay(90.0);
				}
			}
			
			parameter.setDiagMin(diagDeg * 0.9);
			parameter.setDiagMax(diagDeg / 0.9);
		
		} finally {
			printAutoModeValues();
		}
	}
	
	public void setMosaic(Mosaic mosaic2) {
		if (mosaic != null) {
			mosaic.listeners.removeListener(listenerOwner);
			for(Image img : mosaic.getImages())
			{
				MosaicImageParameter mip = mosaic.getMosaicImageParameter(img);
				mip.listeners.removeListener(this.listenerOwner);
			}
			
		}
		this.mosaic = mosaic2;
		this.mosaic.listeners.addListener(listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
			}
			
			@Override
			public void starOccurenceAdded(StarOccurence sco) {
			}
			
			@Override
			public void starAdded(Star star) {
			}
			
			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
			}
			
			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
			}
			
			@Override
			public void imageRemoved(Image image, MosaicImageParameter mip) {
				AstrometryParameterPanel.this.imageRemoved(image);
			}
			
			@Override
			public void imageAdded(Image image, ImageAddedCause cause) {
				if (cause == ImageAddedCause.AutoDetected) {
					AstrometryParameterPanel.this.imageAdded(image);
				}
			}
			
			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
			}
			
			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
			}
		});
		
	}

	public AstrometryParameter getParameter() {
		return new AstrometryParameter(parameter);
	}
}
