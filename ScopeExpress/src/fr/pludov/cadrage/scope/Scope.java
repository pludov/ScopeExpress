package fr.pludov.cadrage.scope;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public interface Scope {

	/** Les notifications ne sont pas reçues dans le thread swing */
	public static interface Listener {
		void onCoordinateChanged();
		void onConnectionStateChanged();
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	boolean isConnected();
	
	// Utiliser pour simuler des déplacements en debug.
	double getRaBias();
	void setRaBias(double d);
	double getDecBias();
	void setDecBias(double d);
	
	double getRightAscension();
	double getDeclination();
	
	// Bloque l'appelant
	void slew(double ra, double dec) throws ScopeException;
	void sync(double ra, double dec) throws ScopeException;
	
	// Démarre (tente la connection et émet des evenements)
	void start();
	
	void close();
}
