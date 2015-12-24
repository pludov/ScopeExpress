package fr.pludov.scopeexpress.openphd;

import java.util.Collections;

import fr.pludov.scopeexpress.scope.DeviceChoosedCallback;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;
import fr.pludov.scopeexpress.scope.DeviceListedCallback;
import fr.pludov.scopeexpress.ui.DriverProvider;

public class OpenPhdProvider implements DriverProvider<OpenPhdDevice>{

	public OpenPhdProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void listDevices(DeviceListedCallback onListed) {
		onListed.onDeviceListed(Collections.singletonList(OpenPhdDeviceIdentifier.getInstance()));
	}

	@Override
	public boolean canChooseScope() {
		return false;
	}

	@Override
	public void chooseDevice(String prefered, DeviceChoosedCallback onChoose) {
		onChoose.onDeviceChoosed(OpenPhdDeviceIdentifier.getInstance());
	}

	@Override
	public OpenPhdDevice buildDevice(DeviceIdentifier si) {
		return new OpenPhdDevice();
	}

	@Override
	public DeviceIdentifier buildIdFor(String storedId) {
		return new OpenPhdDeviceIdentifier(storedId);
	}

}
