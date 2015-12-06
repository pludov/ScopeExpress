package fr.pludov.scopeexpress.http.server.jsonbean;

import java.util.List;
import java.util.Map;

public class EventResponse {
	String serialForNextUpdate;
	Map<String, List<EventData>> events;
	
	public EventResponse() {
		// TODO Auto-generated constructor stub
	}

	public String getSerialForNextUpdate() {
		return serialForNextUpdate;
	}

	public void setSerialForNextUpdate(String serialForNextUpdate) {
		this.serialForNextUpdate = serialForNextUpdate;
	}

	public Map<String, List<EventData>> getEvents() {
		return events;
	}

	public void setEvents(Map<String, List<EventData>> events) {
		this.events = events;
	}

}
