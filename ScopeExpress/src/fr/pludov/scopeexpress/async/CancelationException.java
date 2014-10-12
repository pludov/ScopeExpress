package fr.pludov.scopeexpress.async;

/**
 * Exception qui n'est pas remontée. Une cancelationException n'est pas une cause d'erreur
 * 
 * @author Ludovic POLLET
 */
public class CancelationException extends Exception {

	public CancelationException() {
		// TODO Auto-generated constructor stub
	}

	public CancelationException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public CancelationException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public CancelationException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
