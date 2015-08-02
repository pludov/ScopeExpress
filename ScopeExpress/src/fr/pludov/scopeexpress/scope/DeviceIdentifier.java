package fr.pludov.scopeexpress.scope;

public interface DeviceIdentifier {
	public String getTitle();
	public String getStorableId();
	public boolean matchStorableId(String storedId);
}
