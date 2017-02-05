package fr.pludov.scopeexpress;

import java.util.*;
import java.util.function.*;

import fr.pludov.io.*;
import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.scope.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.DeviceManager.*;
import fr.pludov.scopeexpress.utils.*;

public class OrientationModel {
	public final WeakListenerCollection<OrientationModelListener> listeners = new WeakListenerCollection<OrientationModelListener>(OrientationModelListener.class, true);
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);	
	private final WeakListenerOwner additionalListenerOwner = new WeakListenerOwner(this);	
	final FocusUi focusUi;
	
	// Pendant les sync
	boolean disableTracking = false;
	
	public static enum Origin {
		Astrometry(true), 			// Direct
		DeriveFromAstrometry(true),	// Dérivé
		User(true),
		Driver(false);
		
		public boolean storeValue;
		
		Origin(boolean storeValue)
		{
			this.storeValue = storeValue;
		}
	}
	
	public static enum Field {
		/** Target (jnow) */
		Target(null, OrientationModel::derivateTargetFromAstrometry),
		/** Orientation (jnow) */
		Angle(null, null),
		/** Focale en mm */
		Focale(OrientationModel::getDriverFocale, null),
		/** "/pixel */
		Ech(OrientationModel::getDriverEch, null),
		/** Taille de pixels en um */
		PixSize(OrientationModel::getDriverPixSize, null),
		MountDelta(null, null);
		
		
		final Function<OrientationModel, ? extends Object> driverValue;
		final Function<OrientationModel, ? extends Object> derivateFromAstrometry;
		
		Field(Function<OrientationModel, ? extends Object> driverValue, Function<OrientationModel, ? extends Object> derivateFromAstrometry)
		{
			this.driverValue = driverValue;
			this.derivateFromAstrometry = derivateFromAstrometry;
		}

		public List<Origin> getPossibleOrigins() {
			List<Origin> result = new ArrayList<>(Origin.values().length);
			if (derivateFromAstrometry == null) {
				result.add(Origin.Astrometry);
			} else {
				result.add(Origin.DeriveFromAstrometry);
			}
			if (driverValue != null) {
				result.add(Origin.Driver);
			}
			result.add(Origin.User);
			
			
			return result;
		}
	}

	Object [] currentValues;
	Origin [] origins;
	//	// La cible (centre camera)
//	Double ra, dec;
//	
//	// Quand il est renseigné, on préfère utiliser les coordonnées montures
//	Double raMountDelta, decMountDelta;
//	
//	Double focale;
//	
//	// Pas stocké, fourni par la caméra
//		
//	Double unbinnedEch;
//	Double northAngle;
	
	double [] getDriverEch()
	{
		Double driverFocale = getDriverFocale();
		if (driverFocale == null) return null;
		double [] driverPixSize = getDriverPixSize();
		if (driverPixSize == null) return null;
		
		double [] result = new double[2];
		for(int i = 0; i < 2; ++i) {
			result[i] = 206 * driverPixSize[i] / driverFocale;
		}
		return result;
	}
	
	Double getDriverFocale()
	{
		Scope scope = this.focusUi.getScopeManager().getConnectedDevice();
		if (scope == null || !scope.isConnected()) {
			return null;
		}
		ScopeProperties props = scope.getProperties();
		if (props == null) {
			return null;
		}
		return props.getFocale();
	}
	
	double [] getDriverPixSize()
	{
		Camera cam = this.focusUi.getCameraManager().getConnectedDevice();
		if (cam == null) return null;
		CameraProperties properties = cam.getProperties();
		if (properties == null) return null;

		Double dx = properties.getPixelSizeX();
		Double dy = properties.getPixelSizeY();
		if (dx == null || dy == null) return null;
		return new double[]{dx, dy};
	}
	
	
	public OrientationModel(FocusUi se) {
		this.focusUi = se;
		currentValues = new Object[Field.values().length];
		origins = new Origin[Field.values().length];
		for(Field f : Field.values())
		{
			origins[f.ordinal()] = f.getPossibleOrigins().get(0);
		}
		
		
		se.getCameraManager().listeners.addListener(this.listenerOwner, new Listener() {

			@Override
			public void onDeviceChanged() {
				updateDevice(se.getCameraManager());
			}
		});
		se.getScopeManager().listeners.addListener(this.listenerOwner, new Listener() {

			@Override
			public void onDeviceChanged() {
				updateDevice(se.getScopeManager());
			}
		});

	}

	IdentityHashMap<DeviceManager<?>, IDeviceBase> currentDevices = new IdentityHashMap<>();
	
	private void addAdditionalDeviceListener(IDeviceBase device)
	{
		if (device instanceof Scope) {
			((Scope) device).getListeners().addListener(this.additionalListenerOwner, new Scope.Listener() {
				
				@Override
				public void onCoordinateChanged() {
					// FIXME: pas pendant un sync...
					if (origins[Field.Target.ordinal()] == Origin.Astrometry && !disableTracking) {
						// Uniquement si on a des données...
						origins[Field.Target.ordinal()] = Origin.DeriveFromAstrometry;
					}
					listeners.getTarget().onChange();
				}
			});
		}
	}
	private void dropAdditionalDeviceListener(IDeviceBase device)
	{
		if (device instanceof Scope) {
			((Scope)device).getListeners().removeListener(this.additionalListenerOwner);
		}
	}
	
	private <T extends IDeviceBase> void updateDevice(DeviceManager<T> dm)
	{
		T newDevice = dm.getDevice();
		T current = (T)currentDevices.get(dm);
		
		if (newDevice == current) return;
		if (current != null) {
			current.getStatusListener().removeListener(this.listenerOwner);
			dropAdditionalDeviceListener(current);
			currentDevices.remove(dm);
		}
		
		if (newDevice != null) {
			newDevice.getStatusListener().addListener(this.listenerOwner, new IDriverStatusListener() {
				@Override
				public void onConnectionStateChanged() {
					listeners.getTarget().onChange();
				}
			});
			currentDevices.put(dm, newDevice);
			addAdditionalDeviceListener(newDevice);
		}
	}
	
	
	public Origin getOrigin(Field field)
	{
		return origins[field.ordinal()];
	}
	
	public void clear(Field field)
	{
		if (currentValues[field.ordinal()] == null) return;
		currentValues[field.ordinal()] = null;
		listeners.getTarget().onChange();
	}

	public Object getValue(Field field) {
		switch(getOrigin(field)) {
		case User:
		case Astrometry:
			return currentValues[field.ordinal()];
			
		case DeriveFromAstrometry:
//			Object actual = currentValues[field.ordinal()];
//			if (actual == null) return null;
			if (field.derivateFromAstrometry == null) {
				return null;
			}
			return field.derivateFromAstrometry.apply(this);
			
		case Driver:
			if (field.driverValue == null) return null;
			return field.driverValue.apply(this);
		default:
			throw new RuntimeException("not supported");
		}
	}

	public double[] derivateTargetFromAstrometry()
	{
		double [] currentValue = (double[])currentValues[Field.Target.ordinal()];
		if (disableTracking) {
			return currentValue;
		}
		
		double[] currentDelta = (double[])getValue(Field.MountDelta);
		if (currentDelta == null) {
			return currentValue;
		}
		Scope scope = focusUi.getScopeManager().getConnectedDevice();
		if (scope == null) {
			return currentValue;
		}
		// FIXME: parqué ?
		double scopeRa = 360 * scope.getRightAscension() / 24;
		double scopeDec = scope.getDeclination();
		
		double [] result = new double[]{SkyProjection.deg360(scopeRa + currentDelta[0]), SkyProjection.deg360(scopeDec + currentDelta[1])};
		currentValues[Field.Target.ordinal()] = result;
		return result;
	}
	
	
	public void setOrigin(Field field, Origin o) {
		if (getOrigin(field) == o) {
			return;
		}
		currentValues[field.ordinal()] = field.driverValue.apply(this);
		
		if (o == Origin.Astrometry) {
			currentValues[field.ordinal()] = null;
		}
		origins[field.ordinal()] = o;
		listeners.getTarget().onChange();
		
	}

	public void astrometryDone(MosaicImageParameter mip)
	{
		double imgX = mip.getImage().getWidth() * 0.25;
		double imgY = mip.getImage().getHeight() * 0.25;
		
		// FIXME: uniquement si l'astrometrie est recente ?
//		mip.getImage().getMetadata().getStartMsEpoch();
		
		double [] raDecNow = mip.getProjection().getRaDec(imgX, imgY, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		
		if (getOrigin(Field.Target) != Origin.User) {
			currentValues[Field.Target.ordinal()] = raDecNow;	
		}
		
		// Mettre à jour le décallage
		if (getOrigin(Field.MountDelta) != Origin.User) {
			Scope scope = focusUi.getScopeManager().getConnectedDevice();
			if (scope != null && scope.isConnected()) {
				// Met à jour le décallage de la monture
				double [] scopeNow = new double[]{scope.getRightAscension(),scope.getDeclination()};
				scopeNow[0] *= 360 / 24;
				double [] delta = new double[]{
						SkyProjection.deg360(raDecNow[0] - scopeNow[0]), 
						SkyProjection.deg360(raDecNow[1] - scopeNow[1])};
				currentValues[Field.MountDelta.ordinal()] = delta;
			}
		}
		
		if (getOrigin(Field.PixSize) == Origin.Astrometry) {
			
			CameraFrameMetadata metadata = mip.getImage().getMetadata();
			if (metadata != null) {
				Double sx, sy;
				sx = metadata.getPixSizeX();
				sy = metadata.getPixSizeY();
				if (sx != null && sy != null) {
					currentValues[Field.PixSize.ordinal()] = new double[] {sx, sy};
				} else {
					currentValues[Field.PixSize.ordinal()] = null;
				}
			} else {
				currentValues[Field.PixSize.ordinal()] = null;
			}
		}
		
		// Mettre à jour la focale... Si on connait la taille du capteur
		if (getOrigin(Field.Focale) == Origin.Astrometry)
		{
			//if (getOrigin(Field.PixSize) != Origin.Astrometry)
			{
				double [] pixSize = (double[])this.currentValues[Field.PixSize.ordinal()];
				if (pixSize != null) {
					int [] bin;
					
					CameraFrameMetadata metadata = mip.getImage().getMetadata();
					if (metadata != null && metadata.getBinX() != null && metadata.getBinY() != null) {
						bin = new int[]{metadata.getBinX(), metadata.getBinY()};
					} else {
						bin = new int[]{1, 1};
					}
//				
//				int []physicalPixSize = new int[]{mip.getImage().getWidth() * bin[0], mip.getImage().getHeight() * bin[1]};
//				// Calcul de la taille de la diagonale en um
//				
					double pixDegreeSize = mip.getProjection().getPixelRad() / 2 * 180 / Math.PI;
					double pixArcSize = pixDegreeSize * 3600;
					
					double focale = 206 * pixSize[0] / (pixArcSize / bin[0]);
					this.currentValues[Field.Focale.ordinal()] = focale;
					
				} else {
					this.currentValues[Field.Focale.ordinal()] = null;
				}
//			} else {
//				this.currentValues[Field.Focale.ordinal()] = null;
			}
		}
		
		if (getOrigin(Field.Ech) == Origin.Astrometry)
		{
			double pixDegreeSize = mip.getProjection().getPixelRad() / 2 * 180 / Math.PI;
			
			int [] bin;
			CameraFrameMetadata metadata = mip.getImage().getMetadata();
			if (metadata != null && metadata.getBinX() != null && metadata.getBinY() != null) {
				bin = new int[]{metadata.getBinX(), metadata.getBinY()};
			} else {
				bin = new int[]{1, 1};
			}
			
			this.currentValues[Field.Ech.ordinal()] = new double[]{3600 * pixDegreeSize / bin[0], 3600 * pixDegreeSize / bin[1]};
		}
		
		
		if (getOrigin(Field.Angle) == Origin.Astrometry)
		{
			this.currentValues[Field.Angle.ordinal()] = mip.getProjection().getNorthAngle(
					mip.getImage().getWidth() * 0.5 * 0.5, 
					mip.getImage().getHeight() * 0.5 * 0.5, 
					SkyProjection.jNow());
		}
		
		listeners.getTarget().onChange();
	}
	
	public void startSync()
	{
		disableTracking = true;
	}
	
	public void finishSync(boolean success)
	{
		if (success) {
			this.currentValues[Field.MountDelta.ordinal()] = new double[]{0,0};
		}
		disableTracking = false;
	}
	
//	double [] getDriverTarget()
//	
//	public double [] getMountDelta()
//	{
//		if (raMountDelta == null || decMountDelta == null) return null;
//		return new double[]{raMountDelta, decMountDelta};
//	}
//		
//	private Double getDriverFocale()
//	{
//		Scope scope = focusUi.getScopeManager().getConnectedDevice();
//		if (scope == null) return null;
//		return scope.getFocale();
//	}
//	
//	public Double getFocale()
//	{
//		switch(getOrigin(Field.Focale))
//		{
//		case Auto:
//			// Si on l'a dans le driver, sinon 
//			
//			
//		case Driver:
//			return getDriverFocale();
//		case User:
//			return focale;
//		}
//		// FIXME: selon le mode ?
//		return focale;
//	}
}
