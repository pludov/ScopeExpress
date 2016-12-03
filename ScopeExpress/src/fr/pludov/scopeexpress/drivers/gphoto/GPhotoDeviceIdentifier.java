package fr.pludov.scopeexpress.drivers.gphoto;

import fr.pludov.scopeexpress.scope.*;

public class GPhotoDeviceIdentifier implements DeviceIdentifier {

	public GPhotoDeviceIdentifier() {
	}

	@Override
	public String getTitle() {
		return "GPhoto Camera";
	}

	@Override
	public String getStorableId() {
		return Utils.withProviderId(getProviderId(), "gphoto");
	}

	@Override
	public boolean matchStorableId(String storedId) {
		return Utils.getProviderId(storedId).equals(getProviderId());
	}

	@Override
	public String getProviderId() {
		return GPhotoCameraProvider.gPhotoProviderId;
	}
	
}
