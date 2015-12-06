package fr.pludov.scopeexpress.supervision;

import java.util.HashMap;
import java.util.Map;

public class Supervisor {

	Map<String, EventSource> sources;
	
	public EventSource getSourceById(String id)
	{
		return sources.get(id);
	}
	
	public Supervisor() {
		sources = new HashMap<>();
	}

}
