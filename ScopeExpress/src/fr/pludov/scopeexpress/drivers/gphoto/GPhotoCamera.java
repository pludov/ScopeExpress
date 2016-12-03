package fr.pludov.scopeexpress.drivers.gphoto;

import java.io.*;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.drivers.gphoto.GPhoto.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public class GPhotoCamera implements Camera {
	public static final Logger logger = Logger.getLogger(GPhotoCamera.class);

	final WeakListenerCollection<Camera.Listener> listeners = new WeakListenerCollection<Camera.Listener>(Camera.Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;

	GPhoto currentProcess;
	boolean lastConnected;
	volatile RunningShootInfo currentShoot;
	
	public GPhotoCamera() {
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
	}

	@Override
	public IWeakListenerCollection<IDriverStatusListener> getStatusListener() {
		return statusListeners;
	}

	@Override
	public boolean isConnected() {
		return currentProcess != null && currentProcess.connected;
	}
	
	@Override
	public void close()
	{
		if (currentProcess != null) {
			currentProcess.kill();
			currentProcess = null;
			listeners.getTarget().onConnectionStateChanged();
		}
		
//		
//		boolean retry;
//		do {
//			
//			try {
//				retry = false;
//				join(100);
//				if (isAlive()) {
//					GPhoto process = currentProcess;
//					if (process != null) {
//						process.kill();
//					}
//				}
//			} catch(InterruptedException e) {
//				e.printStackTrace();
//				retry = true;
//			}
//		} while(retry);
	}


	@Override
	public WeakListenerCollection<Listener> getListeners() {
		return listeners;
	}

	@Override
	public void start() {
		currentProcess = new GPhoto();
		currentProcess.startProcess(()-> {
			lastConnected = true;
			listeners.getTarget().onConnectionStateChanged();
		});
		
	}
	@Override
	public void startShoot(ShootParameters parameters) throws CameraException {
		if (currentProcess == null) {
			throw new CameraException("not connected");
		}
		final RunningShootInfo rsi = new RunningShootInfo(parameters);
		if (currentShoot != null) {
			throw new CameraException("camera is busy");
		}
		currentShoot = rsi;
		final GPhoto p = currentProcess;
		currentProcess.startRunnable(() -> {
			boolean success = false;
			try {
				int ms = (int)Math.floor(rsi.getExp() * 1000);
				
				p.doCommand("set-config output 1", 
						"wait-event 2s");
				p.doCommand("set-config bulb 1");
				p.doCommand("wait-event 1");
				// expect("UNKNOWN Camera Status 1");
				long t = System.currentTimeMillis();
				rsi.setStartTime(t);
				long elapsed = System.currentTimeMillis() - t;
				long toSleep = ms - elapsed;
				if (toSleep > 0) {
					System.out.println("Sleeping :" + toSleep);
					Thread.sleep(toSleep);
				} else {
					System.out.println("Too late :" + toSleep);
				}
				CommandResult cr = p.doCommand("set-config bulb 0",
						"set-config output 0",
						"wait-event-and-download 3s");
				File targetFile = rsi.createTargetFile(".cr2");
				if (targetFile != null) {
					try(FileOutputStream fos = new FileOutputStream(targetFile))
					{
						fos.write(cr.cr2);
						
					}
					logger.info("saved: " + targetFile.getAbsolutePath());
					success = true;
					if (currentShoot == rsi) {
						currentShoot = null;
					}
					this.listeners.getTarget().onShootDone(rsi, targetFile);
				}
			} finally {
				if (currentShoot == rsi) {
					currentShoot = null;
				}
				if (!success) {
					this.listeners.getTarget().onShootDone(rsi, null);
				}
			}
		});
		listeners.getTarget().onShootStarted(rsi);
	}

	@Override
	public void cancelCurrentShoot() throws CameraException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RunningShootInfo getCurrentShoot() {
		return currentShoot;
	}

	@Override
	public CameraProperties getProperties() {
		CameraProperties result = new CameraProperties();
		result.setCanAbortExposure(true);
		result.setCanFastReadout(false);
		result.setCanGetCoolerPower(false);
		result.setCanSetCCDTemperature(false);
		result.setCanStopExposure(true);
		result.setMaxBin(1);
		result.setSensorName("GPHOTO ???");
		
		return result;
	}

	@Override
	public TemperatureParameters getTemperature() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCcdTemperature(boolean coolerStatus, Double setTemp) throws CameraException {
		throw new CameraException("setCcdTemperature not supported");
	}

	
}
