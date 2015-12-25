package fr.pludov.scopeexpress.ui.log;

public class LogMessage {
	enum Level {
		Debug,
		Info,
		Warn,
		Error
	};
	
	final Level level;
	final long time;
	final String message;
	
	public LogMessage(Level level, String message) {
		this.level = level;
		this.message = message;
		this.time = System.currentTimeMillis();
	}

}
