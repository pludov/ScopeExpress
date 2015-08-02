package fr.pludov.scopeexpress.scope.ascom;

import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;

public class AscomFocuserProvider extends AscomDriverProvider<Focuser> {
	public AscomFocuserProvider() {
		super("Focuser");
	}

	@Override
	public Focuser buildDevice(DeviceIdentifier si) {
		return new AscomFocuser(((AscomDeviceIdentifier)si).classId);
	}
}
