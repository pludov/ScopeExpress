package fr.pludov.scopeexpress.openphd;

import java.util.*;

import fr.pludov.scopeexpress.scope.*;
import fr.pludov.scopeexpress.ui.*;

public class OpenPhdProvider implements DriverProvider<OpenPhdDevice>{

	static final String openPhdProviderId = "openphd";
	
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
		storedId = OpenPhdDeviceIdentifier.Utils.withoutProviderId(storedId);
		return new OpenPhdDeviceIdentifier(storedId);
	}

	@Override
	public String getProviderId() {
		return openPhdProviderId;
	}
}
