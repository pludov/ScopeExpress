package fr.pludov.scopeexpress.scope;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public interface Scope extends IDeviceBase {

	/** Les notifications ne sont pas re�ues dans le thread swing */
	public static interface Listener extends IDriverStatusListener{
		void onCoordinateChanged();
		default void onPropertyChanged() {};
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	@Override
	boolean isConnected();
	
	// Utiliser pour simuler des d�placements en debug.
	double getRaBias();
	void setRaBias(double d);
	double getDecBias();
	void setDecBias(double d);
	
	double getRightAscension();
	double getDeclination();
	
	ScopeProperties getProperties();
	
	// Bloque l'appelant
	void slew(double ra, double dec) throws ScopeException;
	void sync(double ra, double dec) throws ScopeException;

	// Fait un slew, en degres.
	NativeTask coPulse(double ra, double dec) throws ScopeException;
	
	// D�marre (tente la connection et �met des evenements)
	@Override
	void start();
	
	@Override
	void close();
}
