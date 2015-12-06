package fr.pludov.scopeexpress.scope.ascom;

import fr.pludov.scopeexpress.camera.Camera;
import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;


public class AscomCameraProvider extends AscomDriverProvider<Camera> {
	public AscomCameraProvider() {
		super("Camera");
	}
	
	@Override
	public Camera buildDevice(DeviceIdentifier si) {
		return new AscomCamera(((AscomDeviceIdentifier)si).classId);
	}
}
