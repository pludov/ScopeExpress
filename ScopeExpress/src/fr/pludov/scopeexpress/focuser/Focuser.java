package fr.pludov.scopeexpress.focuser;

import fr.pludov.scopeexpress.ui.IDeviceBase;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public interface Focuser extends IDeviceBase {
	
	/** Les notifications ne sont pas re�ues dans le thread swing */
	public static interface Listener {
		/** Emis suite � un ordre de d�placement qui se termine */
		void onMoveEnded();
		/** Emis periodiquement tant que le focuser est en cours de mouvement */
		void onMoving();
		void onConnectionStateChanged();
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	@Override
	boolean isConnected();

	
	int maxStep();
	int position();
	
	void moveTo(int newPosition) throws FocuserException;
	
	@Override
	void start();
	@Override
	void close();
}
