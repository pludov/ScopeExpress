package fr.pludov.scopeexpress.scope;

public interface ScopeIdentifier {
	public String getTitle();
	public String getStorableId();
	public boolean matchStorableId(String storedId);
}
