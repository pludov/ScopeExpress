package fr.pludov.scopeexpress.scope.ascom;

import fr.pludov.scopeexpress.scope.*;

public class AscomDeviceIdentifier implements DeviceIdentifier {
	String classId;
	String title;
	
	AscomDeviceIdentifier(String classId, String title)
	{
		this.classId = classId;
		this.title = title;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getStorableId() {
		return Utils.withProviderId(getProviderId(), classId + "#" + title);
	}
	
	@Override
	public String getProviderId() {
		return AscomDriverProvider.ascomProviderId;
	}
	
	@Override
	public boolean matchStorableId(String storedId) {
		int storedIdHash = storedId.indexOf('#');
		if (storedIdHash != -1) {
			storedId = storedId.substring(0, storedIdHash);
		}
		return classId.equals(storedId);
	}

}
