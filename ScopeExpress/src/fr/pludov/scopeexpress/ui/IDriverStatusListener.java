package fr.pludov.scopeexpress.ui;

public interface IDriverStatusListener {
	void onConnectionStateChanged();
	// Purement informatif
	void onConnectionError(Throwable message);
}
