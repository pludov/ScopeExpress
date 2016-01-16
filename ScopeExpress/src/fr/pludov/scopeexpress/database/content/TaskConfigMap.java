package fr.pludov.scopeexpress.database.content;

import java.io.*;
import java.util.*;

import fr.pludov.scopeexpress.database.*;

public class TaskConfigMap extends BaseDatabaseItem<Root> {
	private static final long serialVersionUID = 1670086198768076583L;
	
	Map<String, Object> content;
	
	public TaskConfigMap(Database<Root> container) {
		super(container);
		
		sanitize();
	}

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	in.defaultReadObject();
    	sanitize();
    }
	
	private void sanitize()
	{
		if (content == null) {
			content = new HashMap<>();
		}
	}

	public Map<String, Object> getContent() {
		return content;
	}
}
