package fr.pludov.scopeexpress.ui;

import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.DeviceChoosedCallback;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;
import fr.pludov.scopeexpress.scope.DeviceListedCallback;

public interface DriverProvider<DRIVER extends IDeviceBase> {
	/** Liste les scope dispos (attention, c'est asynchrone) */
	void listDevices(DeviceListedCallback onListed);
	
	boolean canChooseScope();
	void chooseDevice(String prefered, DeviceChoosedCallback onChoose);
	
	DRIVER buildDevice(DeviceIdentifier si);

	/** Retourne un identifiant pour la chaine donnée */
	DeviceIdentifier buildIdFor(String storedId);

}
