package fr.pludov.scopeexpress.ui;

public interface IDriverStatusListener {
	default void onConnectionStateChanged() {};
	// Purement informatif
	default void onConnectionError(Throwable message) {};
}
