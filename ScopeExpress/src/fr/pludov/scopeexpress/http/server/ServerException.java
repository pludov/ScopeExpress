package fr.pludov.scopeexpress.http.server;

public class ServerException extends Exception {
	final int statusCode;
	
	public ServerException() {
		statusCode = 500;
	}

	public ServerException(String arg0) {
		super(arg0);
		statusCode = 500;
	}

	public ServerException(Throwable arg0) {
		super(arg0);
		statusCode = 500;
	}

	public ServerException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		statusCode = 500;
	}

	public ServerException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		statusCode = 500;
	}

	public ServerException(int statusCode, String string) {
		super(string);
		this.statusCode = statusCode;
	}

}
