package fr.pludov.scopeexpress.http.server.jsonbean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.pludov.scopeexpress.supervision.Event;

public class EventData {
	Double when;
	Map<String, Object> data;
	
	public EventData() {}
	
	public EventData(Event event) {
		this.when = event.getWhen() / 1000.0;
		data = new HashMap<>();
		List<String> fields = event.getSource().getFields();
		for(int i = 0; i < fields.size(); ++i) {
			data.put(fields.get(i), event.getValue(i));
		}
	}

}
