package fr.pludov.cadrage.scope;

public interface Scope {
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

	void addCoordinateChangedListener(CoordinateChangedListener listener);
	void addConnectionStateChangedListener(ConnectionStateChangedListener listener);
	
	
	public interface CoordinateChangedListener
	{
		public void onCoordinateChanged(Scope scope);
	}
	
	public interface ConnectionStateChangedListener
	{
		public void onConnectionStateChanged(Scope scope);
	}
}
