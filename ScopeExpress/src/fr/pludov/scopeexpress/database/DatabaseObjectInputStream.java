package fr.pludov.scopeexpress.database;

import java.io.*;

class DatabaseObjectInputStream extends ObjectInputStream {
	final Database container;
	
	public DatabaseObjectInputStream(Database container) throws IOException, SecurityException {
		this.container = container;
	}

	public DatabaseObjectInputStream(Database container, InputStream arg0) throws IOException {
		super(arg0);
		this.container = container;
	}

	public Database getContainer() {
		return container;
	}

}
