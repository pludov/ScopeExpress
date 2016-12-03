package fr.pludov.scopeexpress.drivers.gphoto;

import java.util.*;

import javax.swing.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.scope.*;
import fr.pludov.scopeexpress.ui.*;

public class GPhotoCameraProvider implements DriverProvider<Camera> {

	final static String gPhotoProviderId = "gphoto";
	
	public GPhotoCameraProvider() {
	}

	@Override
	public void listDevices(DeviceListedCallback onListed) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				onListed.onDeviceListed(Collections.singletonList(new GPhotoDeviceIdentifier()));
			}
		});
		
	}

	@Override
	public boolean canChooseScope() {
		return false;
	}

	@Override
	public void chooseDevice(String prefered, DeviceChoosedCallback onChoose) {
		
	}

	@Override
	public Camera buildDevice(DeviceIdentifier si) {
		return new GPhotoCamera();
	}

	@Override
	public DeviceIdentifier buildIdFor(String storedId) {
		return new GPhotoDeviceIdentifier();
	}
	
	@Override
	public String getProviderId() {
		return gPhotoProviderId;
	}

}
