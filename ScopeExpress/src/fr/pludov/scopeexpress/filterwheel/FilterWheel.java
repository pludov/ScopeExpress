package fr.pludov.scopeexpress.filterwheel;

import fr.pludov.scopeexpress.focuser.FocuserException;
import fr.pludov.scopeexpress.focuser.Focuser.Listener;
import fr.pludov.scopeexpress.ui.IDeviceBase;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public interface FilterWheel extends IDeviceBase {
	
	/** Les notifications ne sont pas re�ues dans le thread swing */
	public static interface Listener {
		/** Emis suite � un ordre de d�placement qui se termine */
		void onMoveEnded();
		/** Emis periodiquement tant que la roue � filtre est en cours de mouvement */
		void onMoving();
		void onConnectionStateChanged();
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	@Override
	boolean isConnected();

	void moveTo(int newPosition) throws FilterWheelException;
	
	int getCurrentPosition() throws FilterWheelException;
	
	String [] getFilters() throws FilterWheelException;
	
	@Override
	void start();
	@Override
	void close();
}
