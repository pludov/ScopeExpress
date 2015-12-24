package fr.pludov.scopeexpress.camera;

import java.io.File;

import fr.pludov.scopeexpress.focuser.Focuser.Listener;
import fr.pludov.scopeexpress.ui.IDeviceBase;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public interface Camera extends IDeviceBase {
	public static interface Listener {
		/** Lorsque l'état de la connection change */
		void onConnectionStateChanged();
		
		void onShootStarted(RunningShootInfo currentShoot);

		/** Interrupted suite à un cancelCurrentShoot */
		void onShootInterrupted();
		
		/** Fin d'un cliché */
		void onShootDone(RunningShootInfo shootInfo, File generatedFits);
		
		void onTempeatureUpdated();
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
}
