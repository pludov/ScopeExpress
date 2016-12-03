package fr.pludov.scopeexpress.openphd;

import fr.pludov.scopeexpress.scope.*;

public class OpenPhdDeviceIdentifier implements DeviceIdentifier {
	final String id;
	
	OpenPhdDeviceIdentifier(String id) {
		this.id = id;
	}

	@Override
	public String getTitle() {
		return "OpenPhd 2+";
	}

	@Override
	public String getStorableId() {
		return Utils.withProviderId(getProviderId(), id);
	}

	@Override
	public boolean matchStorableId(String storedId) {
		return storedId.equals(getStorableId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenPhdDeviceIdentifier other = (OpenPhdDeviceIdentifier) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	static OpenPhdDeviceIdentifier getInstance() {
		return new OpenPhdDeviceIdentifier("openphd2.x");
	}
	
	@Override
	public String getProviderId() {
		return OpenPhdProvider.openPhdProviderId;
	}
}
