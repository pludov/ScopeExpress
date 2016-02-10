package fr.pludov.scopeexpress.scope.ascom;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.log4j.*;
import org.jawin.*;

import fr.pludov.scopeexpress.async.*;
import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.platform.windows.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;
import net.ivoa.fits.*;
import net.ivoa.fits.hdu.*;

public class AscomCamera extends WorkThread implements Camera {
	private static final Logger logger = Logger.getLogger(AscomCamera.class);

	final WeakListenerCollection<Camera.Listener> listeners = new WeakListenerCollection<Camera.Listener>(Camera.Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;

	final String driver;
	DispatchPtr camera;
	boolean lastConnected;
	int cameraInterfaceVersion;

	// instance constante (duplique à chaque changement)
	volatile RunningShootInfo currentShoot;
	volatile CameraProperties cameraProperties;
	volatile TemperatureParameters temperatures;
	
	String connectedPropertyName;
	
	/**
	 *  Liste des propriétés qui ont déclenché une erreur en lecture
	 *  (pour ne pas boucler sur les erreurs. Réinitialisé à la connection)
	 */
	
	Map<String, Boolean> cameraSupportedProperties = new HashMap<>();

	public AscomCamera(String driver) {
		super();
		statusListeners = new SubClassListenerCollection<IDriverStatusListener, Camera.Listener>(listeners) {
			@Override
			protected Camera.Listener createListenerFor(final IDriverStatusListener i) {
				return new Camera.Listener() {
					@Override
					public void onConnectionStateChanged() {
						i.onConnectionStateChanged();
					}
					@Override
					public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {
					}
					@Override
					public void onShootStarted(RunningShootInfo shootInfo) {
					}
					@Override
					public void onShootInterrupted() {
					}
					@Override
					public void onTempeatureUpdated() {
					}
				};
			}
		};
		this.driver = driver;
		this.lastConnected = false;
	}
	
	private <T> T getCapacity(String name, T defaultValue)
	{
		Boolean supported = cameraSupportedProperties.get(name);
		if (supported != null && !supported.booleanValue()) {
			return defaultValue;
		}
		
		try {
			T result = (T)camera.get(name);
			if (supported == null) {
				cameraSupportedProperties.put(name, Boolean.TRUE);
			}
			return result;
		} catch(COMException  t) {
			if (supported == null) {
				logger.info("Property not supported: " + name, t);
				cameraSupportedProperties.put(name, Boolean.FALSE);
			} else {
				logger.error("Error getting property: " + name, t);
			}
			return defaultValue;
		}
	}
	
	// La caméra est connectée, détermine ce qu'elle peut faire
	private void refreshCapacity() throws Throwable
	{
		CameraProperties cp = new CameraProperties();
		
		cp.setCanAbortExposure(getCapacity("CanAbortExposure", false));
		cp.setCanStopExposure(getCapacity("CanStopExposure", false));
		cp.setCanFastReadout(getCapacity("CanFastReadout", false));
		cp.setCanGetCoolerPower(getCapacity("CanGetCoolerPower", false));
		cp.setCanSetCCDTemperature(getCapacity("CanSetCCDTemperature", false));
		cp.setPixelSizeX(getCapacity("PixelSizeX", (Double)null));
		cp.setPixelSizeY(getCapacity("PixelSizeY", (Double)null));
		cp.setSensorName(getCapacity("SensorName", (String)null));
		cp.setMaxBin(Math.max(1, Math.min(getCapacity("MaxBinX", (short) 1), getCapacity("MaxBinY", (short) 1))));
		this.cameraProperties = cp;
		
	}
	
	private void refreshTemperatures() throws Throwable
	{
		TemperatureParameters temps = new TemperatureParameters();
		temps.setCoolerOn(getCapacity("CoolerOn", false));
		if (temps.isCoolerOn()) {
			temps.setCoolerPower(getCapacity("CoolerPower", (Double)null));
		}
		temps.setCCDTemperature(getCapacity("CCDTemperature", (Double)null));
		temps.setHeatSinkTemperature(getCapacity("HeatSinkTemperature", (Double)null));
		temps.setSetCCDTemperature(getCapacity("SetCCDTemperature", (Double)null));

		
//		if (this.temperatures != null && this.temperatures.equals(temp)) {
//			return;
//		}
		this.temperatures = temps;
		listeners.getTarget().onTempeatureUpdated();
	}
	
	/** Appellé uniquement dans le thread de gestion de la caméra */
	private void refreshParameters() throws Throwable
	{
		boolean connectStatus;
		boolean imageReady = false;
		RunningShootInfo readyImageParameters = null;
		File imageFits = null;
		if (camera != null) {
			connectStatus = (Boolean)camera.get(connectedPropertyName);
			if (!connectStatus) {
				cameraSupportedProperties.clear();
			}
			if (currentShoot != null) {
				imageReady = (Boolean)camera.get("ImageReady");
				if (imageReady) {
					readyImageParameters = currentShoot;
					logger.info("Image ready");
					
					Object data0 = camera.get("ImageArray");
					logger.info("Got datas");
					int width = (int)camera.get("NumX");
					int height = (int)camera.get("NumY");
					logger.debug("Image ready:" + data0);
					
					int arrayLen = Array.getLength(data0);
					if (arrayLen != width * height) {
						throw new Exception("data size mismatch");
					}
					
					int maxADU = (int)camera.get("MaxADU");
					// FIXME: Pour l'instant on fait comme si on avait un maxADU systématiquement à 65535
					if (maxADU > 65535) {
						logger.warn("Max ADU is > 65535. 32 bits fits is not supported");
					}
					
					double lastExp = (double)camera.get("LastExposureDuration");
					String lastExpStart = (String)camera.get("LastExposureStartTime");
					
					Integer xbinning, ybinning;
					try {
						xbinning = (int)(Short)camera.get("BinX");
						ybinning = (int)(Short)camera.get("BinY");
					} catch(COMException e) {
						logger.warn("Could not retrieve pixel binning", e);
						xbinning = null;
						ybinning = null;
					}
					
					Double pixSx, pixSy;
					try {
						pixSx = (double)camera.get("PixelSizeX");
						pixSy = (double)camera.get("PixelSizeY");
						if (xbinning != null) pixSx *= xbinning;
						if (ybinning != null) pixSy *= ybinning;
					} catch(COMException e) {
						logger.warn("Could not retrieve pixel size", e);
						pixSx = null;
						pixSy = null;
					}
					
					logger.info("Now saving");
					try {
						File target = new File(readyImageParameters.getPath() + "/" + readyImageParameters.getFileName());
						File parent = target.getParentFile();
						parent.mkdirs();
						
						String baseName = target.getName();
						String ext = ".fits";
						File targetFile = null;
						for(int i = 0; i < 10000; ++i) {
							File f;
							if (i == 0) {
								f = new File(parent, baseName + ext);
							} else {
								f = new File(parent, baseName + "-" + i + ext);
							}
							if (f.createNewFile()) {
								targetFile = f;
								break;
							}
						}
						if (targetFile == null) {
							logger.warn("Unable to create new file for " + baseName);
						} else {
							try(FileOutputStream fos = new FileOutputStream(targetFile);
									DataOutputStream dos = new DataOutputStream(fos)){
								Fits fits = new Fits();
								short [][] result = new short[height][];
								int [] idata = (int[]) data0;
								for(int y = 0; y < height; ++y)
								{
									int m0 = y * width;
									short[] line = new short[width];
									result[y] = line;
									for(int i = 0; i < width; ++i) {
										line[i] = (short) (idata[m0 + i] - 32768);
									}
								}
								
		//						Header header = new Header();
		//						header.
		//						
								ImageHDU imageHdu = (ImageHDU) Fits.makeHDU(result);
		//						imageHdu.addValue(key, val, comment);
								// http://heasarc.gsfc.nasa.gov/docs/fcg/standard_dict.html
								imageHdu.addValue("BZERO", 32768, "zero point in scaling equation");
								imageHdu.addValue("BSCALE", 1, "linear factor in scaling equation");
								imageHdu.addValue("DATE-OBS", lastExpStart, "Date of observation start");
								imageHdu.addValue("EXPTIME", lastExp, "Exposure time (sec)");
								if (pixSx != null) {
									imageHdu.addValue("XPIXSZ", pixSx, "Pixel width in microns (after binning");
								}
								if (pixSy != null) {
									imageHdu.addValue("YPIXSZ", pixSy, "Pixel height in microns (after binning)");
								}
								if (xbinning != null) {
									imageHdu.addValue("XBINNING", xbinning, "binning factor used on X axis");
								}
								if (ybinning != null) {
									imageHdu.addValue("YBINNING", ybinning, "binning factor used on Y axis");
								}
								// FIXME : ce fits devrait encore être modifiés plusieurs fois par la suite
								// Pour inclure plus de metadata...
								// Comment supporter le focuser ? la roue à filtre ?
								// La position de la monture, le centre et la rotation (super utile pour retrouver une image)
								fits.addHDU(imageHdu);
								fits.write(dos);
								fos.close();
								
								imageFits = targetFile;
								// On a un tableau de int (à peu près...) 
								// On veut écrire un fits avec.
								logger.info("saved: " + imageFits.getAbsolutePath());
							}
						}
					} catch(Throwable e) {
						logger.warn("Unable to retrieve photo", e);
					}
				}
			}
		} else {
			connectStatus = false;
		}
		
		boolean fireConnectionChanged = false;
		
		synchronized(this)
		{
			if (this.lastConnected != connectStatus) {
				this.lastConnected = connectStatus;
				fireConnectionChanged = true;
			}
			if (imageReady) {
				currentShoot = null;
			}
		}

		if (fireConnectionChanged) {
			this.listeners.getTarget().onConnectionStateChanged();
		}
		if (imageReady) {
			this.listeners.getTarget().onShootDone(readyImageParameters, imageFits);
		}
	}

	@Override
	public IWeakListenerCollection<IDriverStatusListener> getStatusListener() {
		return this.statusListeners;
	}
	
	@Override
	public void close()
	{
		setTerminated();
		
		boolean retry;
		do {
			try {
				retry = false;
				join();
			} catch(InterruptedException e) {
				e.printStackTrace();
				retry = true;
			}
		} while(retry);
	}

	// Quand cette méthode retourne, 
	protected void chooseDevice() throws CancelationException
	{
		try {
			Ole.initOle();

			logger.debug("driver : " + driver);
			
			camera = new DispatchPtr(driver);
			
			try {
				cameraInterfaceVersion = (Short)camera.get("InterfaceVersion");
			} catch(COMException  t) {
				logger.info("Could not get InterfaceVersion", t);
				cameraInterfaceVersion = 1;
			}
			connectedPropertyName = "Connected";
			camera.put(connectedPropertyName, true);
			this.cameraSupportedProperties.clear();
			refreshCapacity();
			refreshParameters();
			refreshTemperatures();
		} catch (Throwable e) {
			camera = null;
			if (e instanceof CancelationException) throw (CancelationException)e;
			e.printStackTrace();
			return;
		}
	}
	
	private void notifyFinalDisconnection()
	{
		synchronized(this) {
			if (!this.lastConnected) return;
			this.lastConnected = false;
		}
		this.listeners.getTarget().onConnectionStateChanged();
	}
	

	@Override
	public void run() {
		try {
			chooseDevice();
			
		} catch(Throwable t) {
			this.listeners.getTarget().onConnectionStateChanged();
			if (t instanceof CancelationException) {
				return;
			}
			
			t.printStackTrace();
			return;
		}
		
		// On reste à l'écoute du travail à faire...
		try {
			try {
				setPeriodicTask(new Task() {
					int idleCount = 0;
					int tempCount = 0;
					@Override
					public Object run() throws Throwable {
						if (currentShoot != null || (idleCount >= 10)) {
							idleCount = 0;
							refreshParameters();
						} else {
							idleCount++;
						}
						
						if (tempCount >= 20) {
							refreshTemperatures();
							tempCount = 0;
						} else {
							tempCount ++;
						}
						
						return null;
					}
				}, 50);
				super.run();
			} finally {
				notifyFinalDisconnection();
			}
		} finally {
			// releaseOle();
		}
	}

	@Override
	public boolean isConnected() {
		return lastConnected;
	}

	@Override
	public void setCcdTemperature(final boolean onOff, final Double setTemp) throws CameraException {
		logger.info("Setting temperature to " + setTemp);
		try {
			exec(new AsyncOrder() {
				@Override
				public Object run() throws Throwable {
					try {
						if (!onOff) {
							// on le coupe avant
							camera.put("CoolerOn", onOff);
						}
						if (setTemp != null) {
							camera.put("SetCCDTemperature", setTemp.doubleValue());
						}
						if (onOff) {
							// On l'active après avoir mis la température
							camera.put("CoolerOn", onOff);
						}
					} catch(Throwable t) {
						throw t;
					} finally {
						refreshTemperatures();
					}
					
					return null;
				}
			});
		} catch(Throwable t) {
			if (t instanceof CameraException) {
				throw (CameraException)t;
			}
			throw new CameraException("Erreur de shoot", t);
		}
	}
	
	@Override
	public void startShoot(ShootParameters parameters) throws CameraException {
		final RunningShootInfo rsi = new RunningShootInfo(parameters);
		logger.info("Shooting for " + parameters.getExp());
		try {
			exec(new AsyncOrder() {
				@Override
				public Object run() throws Throwable {
					if (currentShoot != null) {
						throw new CameraException("Camera is busy");
					}
					// FIXME: on ignore tous les autres paramètres là !
					rsi.setStartTime(System.currentTimeMillis());
					listeners.getTarget().onShootStarted(rsi);
					try {
						currentShoot = rsi;
						camera.invoke("StartExposure", rsi.getExp(), true);
					} catch(Throwable t) {
						currentShoot = null;
						listeners.getTarget().onShootDone(rsi, null);
						throw t;
					}
					
					return null;
				}
			});
		} catch(Throwable t) {
			if (t instanceof CameraException) {
				throw (CameraException)t;
			}
			throw new CameraException("Erreur de shoot", t);
		}
	}

	@Override
	public void cancelCurrentShoot() throws CameraException {
		logger.info("Canceling shoot");
		try {
			exec(new AsyncOrder() {
				@Override
				public Object run() throws Throwable {
					if (currentShoot == null) {
						return null;
					}
					camera.invoke("StopExposure");
					currentShoot = null;
					listeners.getTarget().onShootInterrupted();
					return null;
				}
			});
		} catch(Throwable t) {
			if (t instanceof CameraException) {
				throw (CameraException)t;
			}
			throw new CameraException("Erreur de shoot", t);
		}	
	}
	
	@Override
	public RunningShootInfo getCurrentShoot() {
		return currentShoot;
	}

	@Override
	public WeakListenerCollection<Listener> getListeners() {
		return listeners;
	}

	@Override
	public CameraProperties getProperties() {
		return cameraProperties;
	}

	@Override
	public TemperatureParameters getTemperature() {
		return temperatures;
	}
	
}
