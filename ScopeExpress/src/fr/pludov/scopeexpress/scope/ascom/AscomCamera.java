package fr.pludov.scopeexpress.scope.ascom;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.Arrays;

import net.ivoa.fits.Fits;
import net.ivoa.fits.Header;
import net.ivoa.fits.hdu.ImageHDU;

import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.scopeexpress.async.CancelationException;
import fr.pludov.scopeexpress.camera.Camera;
import fr.pludov.scopeexpress.camera.CameraException;
import fr.pludov.scopeexpress.camera.CameraProperties;
import fr.pludov.scopeexpress.camera.RunningShootInfo;
import fr.pludov.scopeexpress.camera.ShootParameters;
import fr.pludov.scopeexpress.camera.TemperatureParameters;
import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.focuser.FocuserException;
import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.ui.IDriverStatusListener;
import fr.pludov.scopeexpress.utils.IWeakListenerCollection;
import fr.pludov.scopeexpress.utils.SubClassListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WorkThread;

public class AscomCamera extends WorkThread implements Camera {
	private static final Logger logger = Logger.getLogger(AscomCamera.class);

	final WeakListenerCollection<Camera.Listener> listeners = new WeakListenerCollection<Camera.Listener>(Camera.Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;

	final String driver;
	DispatchPtr camera;
	boolean lastConnected;
	int cameraInterfaceVersion;

	// instance constante (duplique � chaque changement)
	volatile RunningShootInfo currentShoot;
	volatile CameraProperties cameraProperties;
	volatile TemperatureParameters temperatures;
	
	String connectedPropertyName;

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
		try {
			return (T)camera.get(name);
		} catch(COMException  t) {
			t.printStackTrace();
			return defaultValue;
		}
	}
	
	// La cam�ra est connect�e, d�termine ce qu'elle peut faire
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
	
	/** Appell� uniquement dans le thread de gestion de la cam�ra */
	private void refreshParameters() throws Throwable
	{
		boolean connectStatus;
		boolean imageReady = false;
		RunningShootInfo readyImageParameters = null;
		File imageFits = null;
		if (camera != null) {
			connectStatus = (Boolean)camera.get(connectedPropertyName);

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
					// FIXME: Pour l'instant on fait comme si on avait un maxADU syst�matiquement � 65535
					if (maxADU > 65535) {
						logger.warn("Max ADU is > 65535. 16 bits fits is not possible");
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
								// FIXME : ce fits devrait encore �tre modifi�s plusieurs fois par la suite
								// Pour inclure plus de metadata...
								// Comment supporter le focuser ? la roue � filtre ?
								// La position de la monture, le centre et la rotation (super utile pour retrouver une image)
								fits.addHDU(imageHdu);
								fits.write(dos);
								fos.close();
								
								imageFits = targetFile;
								// On a un tableau de int (� peu pr�s...) 
								// On veut �crire un fits avec.
								logger.info("saved: " + imageFits);
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

	// Quand cette m�thode retourne, 
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
		
		// On reste � l'�coute du travail � faire...
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
							// On l'active apr�s avoir mis la temp�rature
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
					// FIXME: on ignore tous les autres param�tres l� !
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