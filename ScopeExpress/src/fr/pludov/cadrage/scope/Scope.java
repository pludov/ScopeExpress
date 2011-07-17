package fr.pludov.cadrage.scope;

public interface Scope {
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
	
	void close();
}
