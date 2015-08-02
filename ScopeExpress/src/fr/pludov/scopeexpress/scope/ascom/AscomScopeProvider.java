package fr.pludov.scopeexpress.scope.ascom;

import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;

public class AscomScopeProvider extends AscomDriverProvider<Scope> {

	public AscomScopeProvider() {
		super("Telescope");
	}

	@Override
	public Scope buildDevice(DeviceIdentifier si) {
		return new AscomScope(((AscomDeviceIdentifier)si).classId);
	}
	
}
