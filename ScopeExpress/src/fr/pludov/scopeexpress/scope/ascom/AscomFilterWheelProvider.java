package fr.pludov.scopeexpress.scope.ascom;

import fr.pludov.scopeexpress.camera.Camera;
import fr.pludov.scopeexpress.filterwheel.FilterWheel;
import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;


public class AscomFilterWheelProvider extends AscomDriverProvider<FilterWheel> {
	public AscomFilterWheelProvider() {
		super("FilterWheel");
	}
	
	@Override
	public FilterWheel buildDevice(DeviceIdentifier si) {
		return new AscomFilterWheel(((AscomDeviceIdentifier)si).classId);
	}
}
