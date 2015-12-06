package fr.pludov.scopeexpress.camera;

import java.io.File;

import fr.pludov.scopeexpress.focuser.Focuser.Listener;
import fr.pludov.scopeexpress.ui.IDeviceBase;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public interface Camera extends IDeviceBase {
	public static interface Listener {
		/** Lorsque l'�tat de la connection change */
		void onConnectionStateChanged();
		/** Fin d'un clich� */
		void onShootDone(RunningShootInfo shootInfo, File generatedFits);
		
		void onShootStarted(RunningShootInfo currentShoot);
		
		void onTempeatureUpdated();
	}

	WeakListenerCollection<Listener> getListeners();
	
	/** Throw une exception si les choses tournent mal, sinon, on va emettre un onShootDone... */
	void startShoot(ShootParameters parameters) throws CameraException;

	/** 
	 * Ceci n'est chang� que de mani�re synchrone avec Swing
	 * retourne null si pas de shoot en cours
	 */
	RunningShootInfo getCurrentShoot();
	
	/**
	 * Valable tant que la connection est �tablie
	 */
	CameraProperties getProperties();
	
	TemperatureParameters getTemperature();

	void setCcdTemperature(boolean coolerStatus, Double setTemp) throws CameraException;
}
