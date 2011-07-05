package fr.pludov.cadrage.scope;

public interface Scope {
	boolean isConnected();
	
	double getRightAscension();
	double getDeclination();
	
	// Bloque l'appelant
	void slew(double ra, double dec) throws ScopeException;
	
	void close();
}
