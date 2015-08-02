package fr.pludov.scopeexpress.scope;

import java.util.List;

public interface DeviceListedCallback {

	/** Appellé quand un scope a été choisi ou null si annulation/erreur */
	void onDeviceListed(List<? extends DeviceIdentifier> availables);
}
