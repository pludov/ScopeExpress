package fr.pludov.scopeexpress.scope;

import java.util.List;

import fr.pludov.scopeexpress.ui.DriverProvider;
import fr.pludov.scopeexpress.utils.Couple;

public interface ScopeProvider extends DriverProvider<Scope> {
	/** Liste les scope dispos (attention, c'est asynchrone) */
	@Override
	void listDevices(DeviceListedCallback onListed);
	
	@Override
	boolean canChooseScope();
	@Override
	void chooseDevice(String prefered, DeviceChoosedCallback onChoose);
	
	@Override
	Scope buildDevice(DeviceIdentifier si);

	/** Retourne un identifiant pour la chaine donnée */
	@Override
	DeviceIdentifier buildIdFor(String storedId);
}
