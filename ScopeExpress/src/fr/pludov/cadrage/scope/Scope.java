package fr.pludov.cadrage.scope;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public interface Scope {

	/** Les notifications ne sont pas re�ues dans le thread swing */
	public static interface Listener {
		void onCoordinateChanged();
		void onConnectionStateChanged();
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	boolean isConnected();
	
	// Utiliser pour simuler des d�placements en debug.
	double getRaBias();
	void setRaBias(double d);
	double getDecBias();
	void setDecBias(double d);
	
	double getRightAscension();
	double getDeclination();
	
	// Bloque l'appelant
	void slew(double ra, double dec) throws ScopeException;
	void sync(double ra, double dec) throws ScopeException;
	
	// D�marre (tente la connection et �met des evenements)
	void start();
	
	void close();
}
