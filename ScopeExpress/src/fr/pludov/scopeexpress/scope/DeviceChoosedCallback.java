package fr.pludov.scopeexpress.scope;

public interface DeviceChoosedCallback {

	/** Appell� quand un scope a �t� choisi ou null si annulation/erreur */
	void onDeviceChoosed(DeviceIdentifier si);
}
