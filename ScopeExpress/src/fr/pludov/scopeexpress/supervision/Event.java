package fr.pludov.scopeexpress.supervision;

public class Event {
	final EventSource source;
	final long when;
	final Object [] values;
	
	public Event(EventSource source, long time, Object ... values) {
		this.source = source;
		this.when = time;
		this.values = values;
	}

	public long getWhen() {
		return when;
	}

	public EventSource getSource() {
		return source;
	}

	public Object getValue(int i) {
		return values[i];
	}
	
	
}
