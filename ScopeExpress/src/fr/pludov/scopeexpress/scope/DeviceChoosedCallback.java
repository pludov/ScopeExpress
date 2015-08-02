package fr.pludov.scopeexpress.scope;

public interface DeviceChoosedCallback {

	/** Appellé quand un scope a été choisi ou null si annulation/erreur */
	void onDeviceChoosed(DeviceIdentifier si);
}
