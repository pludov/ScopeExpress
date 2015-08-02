package fr.pludov.scopeexpress.scope;

import java.util.List;

public interface DeviceListedCallback {

	/** Appell� quand un scope a �t� choisi ou null si annulation/erreur */
	void onDeviceListed(List<? extends DeviceIdentifier> availables);
}
