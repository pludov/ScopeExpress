package fr.pludov.scopeexpress.ui.utils;

import java.util.*;

import com.google.gson.*;

import fr.pludov.scopeexpress.ui.preferences.*;

public class GlobalPreferences {

	private static class Entry {
		StringConfigItem storage;
		Object currentValue;
	}
	
	final Map<String, Entry> items = new HashMap<>();

	private Entry lookup(String path, boolean create)
	{
		Entry result = items.get(path);
		if (result != null || !create) {
			return result;
		}
		
		result = new Entry();
		result.storage = new StringConfigItem(GlobalPreferences.class, path, null);
		String content = result.storage.get();
		if (content != null) {
			try {
				result.currentValue = new Gson().fromJson(content, Object.class);
			} catch(JsonSyntaxException e) {
				e.printStackTrace();
			}
		}
		items.put(path, result);
		return result;
	}
	
	public Object getCurrentStorageForPath(String path)
	{
		Entry e = lookup(path, false);
		if (e == null) return null;
		return e.currentValue;
	}
	
	public void setCurrentStorageForPath(String path, Object o)
	{
		Entry e = lookup(path, true);
		e.currentValue = o;
		e.storage.set(new Gson().toJson(o));
	}
	
	private GlobalPreferences() {
	}
	
	static GlobalPreferences instance;
	
	public static synchronized GlobalPreferences getInstance()
	{
		if (instance == null) {
			instance = new GlobalPreferences();
		}
		return instance;
	}

}
