package fr.pludov.scopeexpress.scope;

public interface DeviceIdentifier {
	public String getProviderId();
	public String getTitle();
	public String getStorableId();
	public boolean matchStorableId(String storedId);
	
	
	public static class Utils {
		public static String getProviderId(String from)
		{
			int id = from.indexOf(':');
			if (id == -1) {
				return "";
			}
			return from.substring(0, id);
		}
		
		public static String withoutProviderId(String from)
		{
			int id = from.indexOf(':');
			if (id == -1) {
				return "";
			}
			return from.substring(id + 1);
		}
		
		public static String withProviderId(String providerId, String storableId)
		{
			return providerId + ":" + storableId;
		}

		public static String withoutSpecificProviderId(String prefered, String ascomproviderid) {
			if (ascomproviderid == null) return null;
			if (getProviderId(prefered).equals(ascomproviderid)) {
				return withoutProviderId(prefered);
			}
			return null;
		}
	}
}
