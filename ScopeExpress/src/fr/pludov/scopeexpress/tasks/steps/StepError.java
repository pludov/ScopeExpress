package fr.pludov.scopeexpress.tasks.steps;

public class StepError implements EndMessage {
	final String message;

	public StepError(String message) {
		this.message = message;
	}


	@Override
	public String toString() {
		return message;
	}
}
