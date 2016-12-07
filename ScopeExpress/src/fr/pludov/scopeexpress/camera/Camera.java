package fr.pludov.scopeexpress.camera;

import java.io.*;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public interface Camera extends IDeviceBase {
	public static final class LoggerContainer {
		private static final Logger logger = Logger.getLogger(Camera.class);
	}
	
	public static interface Listener extends IDriverStatusListener {
		default void onShootStarted(RunningShootInfo currentShoot) {};

		default void onShootProgress() {};
		
		/** Interrupted suite à un cancelCurrentShoot */
		default void onShootInterrupted() {};
		
		/** Fin d'un cliché */
		default void onShootDone(RunningShootInfo shootInfo, File generatedFits) {};
		
		default void onTempeatureUpdated() {};
	}

	WeakListenerCollection<Listener> getListeners();
	
	/** Throw une exception si les choses tournent mal, sinon, on va emettre un onShootDone/onShootInterrupted... */
	void startShoot(ShootParameters parameters) throws CameraException;

	/** on va recevoir un onShootInterrupted avec un generatedFits vide */
	void cancelCurrentShoot() throws CameraException;
	
	/** 
	 * Ceci n'est changé que de manière synchrone avec Swing
	 * retourne null si pas de shoot en cours
	 */
	RunningShootInfo getCurrentShoot();
	
	/**
	 * Valable tant que la connection est établie
	 */
	CameraProperties getProperties();
	
	TemperatureParameters getTemperature();

	void setCcdTemperature(boolean coolerStatus, Double setTemp) throws CameraException;
	
	default NativeTask coShoot(Object params) {
		final ShootParameters sp = JSTask.fromJson(ShootParameters.class, params);
		return new NativeTask() {
			boolean started = false;
			
			@Override
			protected void cancel() {
				super.cancel();
				if (started) {
					started = false;
					try {
						cancelCurrentShoot();
					} catch (CameraException e) {
						LoggerContainer.logger.warn("Unable to cancel shoot", e);
					}
				}
			}
			
			@Override
			protected void init() throws Throwable {
				
				getListeners().addListener(this.listenerOwner, new Listener() {
					@Override
					public void onConnectionStateChanged() {
						started = false;
						failed("disconnected");
					}
					
					@Override
					public void onShootInterrupted() {
						started = false;
						failed("interrupted");
					}
					
					@Override
					public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {
						started = false;
						done(generatedFits);
					}
				});
				
				startShoot(sp);
				started = true;
			}
		};
		
	}
}
