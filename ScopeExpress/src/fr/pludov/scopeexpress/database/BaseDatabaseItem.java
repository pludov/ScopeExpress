package fr.pludov.scopeexpress.database;

import java.io.*;

public abstract class BaseDatabaseItem<ROOT extends BaseDatabaseItem<ROOT>> implements Serializable {
	private static final long serialVersionUID = -9079444406345910259L;
	transient Database<ROOT> container;
	
	public BaseDatabaseItem(Database<ROOT> db) {
		this.container = db;
	}

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        container = ((DatabaseObjectInputStream)in).getContainer();
    }

	public Database<ROOT> getContainer() {
		return container;
	}
	
}
