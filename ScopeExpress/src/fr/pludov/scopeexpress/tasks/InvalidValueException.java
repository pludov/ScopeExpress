package fr.pludov.scopeexpress.tasks;

import fr.pludov.scopeexpress.utils.EndUserException;

public class InvalidValueException extends EndUserException {

	public InvalidValueException() {
	}

	public InvalidValueException(String message) {
		super(message);
	}

	public InvalidValueException(Throwable cause) {
		super(cause);
	}

	public InvalidValueException(String message, Throwable cause) {
		super(message, cause);
	}

}
