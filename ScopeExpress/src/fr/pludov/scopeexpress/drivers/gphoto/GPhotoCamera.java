package fr.pludov.scopeexpress.drivers.gphoto;

import java.io.*;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.drivers.gphoto.GPhoto.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

/**
 * The GPhoto process is addressed async, using a short-lived thread per request (GPhotoJob.running)
 */
public class GPhotoCamera implements Camera {
	public static final Logger logger = Logger.getLogger(GPhotoCamera.class);

	final WeakListenerCollection<Camera.Listener> listeners = new WeakListenerCollection<Camera.Listener>(Camera.Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;

	GPhoto currentProcess;
	volatile CameraProperties cameraProperties;
	/** Modifié en asynhchrone par les thread de traitement (GPhotoJob) */
	volatile boolean lastConnected;
	/** Repositionné à null  en asynhchrone par les thread de traitement (GPhotoJob) */
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
					public void onConnectionError(Throwable message) {
						i.onConnectionError(message);
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
		currentProcess.startRunnable(()-> {
			try {
				currentProcess.startProcess();
			
				try {
					currentProcess.waitInit();

					CameraProperties result = new CameraProperties();
					result.setCanAbortExposure(true);
					result.setCanFastReadout(false);
					result.setCanGetCoolerPower(false);
					result.setCanSetCCDTemperature(false);
					result.setCanStopExposure(true);
					result.setMaxBin(1);
					result.setSensorName(currentProcess.model != null ? currentProcess.model : "Unknown model");
					
					cameraProperties = result;

				} catch (CameraException e) {
					throw new IOException("Initialisation failed: " + e.getMessage(), e);
				}

			} catch(Throwable t) {
				t.printStackTrace();
				// statusListeners.
				listeners.getTarget().onConnectionError(t);
				lastConnected = false;
				listeners.getTarget().onConnectionStateChanged();
				return;
			}
			lastConnected = true;
			listeners.getTarget().onConnectionStateChanged();
		});
		
	}
	
	class BulbShootJob implements GPhotoCancelableJob {
		final GPhoto p;
		
		final RunningShootInfo rsi;
		
		volatile boolean aborted;
		
		BulbShootJob(GPhoto p, RunningShootInfo rsi)
		{
			this.p = p;
			this.rsi = rsi;
		}
		
		private synchronized void sleepOrAbort(long duration, GPhotoJob abort) throws IOException, InterruptedException
		{
			long now = System.currentTimeMillis();
			if (aborted) {
				abort.run();
				throw new InterruptedException();
			}
			long elapsed = now + duration;
			
			while((now = System.currentTimeMillis()) < elapsed) {
				wait(elapsed - now);
				if (aborted) {
					abort.run();
					throw new InterruptedException();
				}
			}
		}
		
		synchronized void abort(){
			aborted = true;
			notifyAll();
		}
		
		@Override
		public void canceled() {
			listeners.getTarget().onShootDone(rsi, null);
		}
		
		@Override
		public void run() throws IOException, InterruptedException {

			boolean success = false;
			try {
				int ms = (int)Math.floor(rsi.getExp() * 1000);

				p.noError(p.doCommand("set-config output 1"));
				
				sleepOrAbort(2000, ()->{
					p.doCommand("set-config output 0");
					p.trashEvents();
				});
				p.noError(p.doCommand("set-config bulb 1"));
				// FIXME : wait bulb start event ?
				long t = System.currentTimeMillis();
				rsi.setStartTime(t);
				long elapsed = System.currentTimeMillis() - t;
				long toSleep = ms - elapsed;
				if (toSleep > 0) {
					System.out.println("Sleeping :" + toSleep);
					sleepOrAbort(toSleep, ()->{
						p.doCommand("set-config bulb 0", "set-config output 0");
						p.trashEvents();
					});
				} else {
					System.out.println("Too late :" + toSleep);
				}
				CommandResult cr = p.doCommand("set-config bulb 0",
						"set-config output 0",
						"wait-event-and-download 3s");
				
				if (cr.cr2 == null) {
					logger.warn("No CR2 found during bulb");
				} else {
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
						listeners.getTarget().onShootDone(rsi, targetFile);
					}
				}
				p.trashEvents();
			} catch(CameraException e) {
				e.printStackTrace();
			} catch(InterruptedException e) {
			} finally {
				if (!success) {
					if (currentShoot == rsi) {
						currentShoot = null;
					}
					listeners.getTarget().onShootDone(rsi, null);
				}
			}
		}
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

		currentProcess.startRunnable(new BulbShootJob(currentProcess, rsi));

		listeners.getTarget().onShootStarted(rsi);
	}

	@Override
	public void cancelCurrentShoot() throws CameraException {
		GPhoto process = this.currentProcess;
		if (process == null) return;
		GPhotoJob job = process.getRunning();
		if (job == null) {
			return;
		}
		if (job instanceof BulbShootJob) {
			((BulbShootJob)job).abort();
		}
	}

	@Override
	public RunningShootInfo getCurrentShoot() {
		return currentShoot;
	}

	@Override
	public CameraProperties getProperties() {
		return cameraProperties;
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
