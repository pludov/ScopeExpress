package fr.pludov.scopeexpress.database;

import java.io.*;
import java.util.*;

import fr.pludov.scopeexpress.utils.*;

public class DatabaseItemCollection<ROOT extends BaseDatabaseItem<ROOT>, CONTENT extends BaseDatabaseItem<ROOT>> implements Serializable{
	private static final long serialVersionUID = -13375358646452713L;

	public transient WeakListenerCollection<Listener<CONTENT>> listeners;
	
	List<CONTENT> content;
	
	public DatabaseItemCollection()
	{
		this.content = new ArrayList<>();
		sanitize();
	}

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        sanitize();
    }
	
    void sanitize()
    {
    	if (content == null) {
    		content = new ArrayList<>();
    	}
    	listeners = (WeakListenerCollection)new WeakListenerCollection<>(Listener.class);
    }
    
	public interface Listener<CONTENT> {
		void itemAdded(CONTENT content);
		void itemRemoved(CONTENT content);
	}
	
	public void add(CONTENT item)
	{
		this.content.add(item);
		this.listeners.getTarget().itemAdded(item);
	}
	
	public boolean remove(CONTENT item)
	{
		if (this.content.remove(item)) {
			this.listeners.getTarget().itemRemoved(item);
			return true;
		}
		return false;
	}
	
	public List<CONTENT> getContent()
	{
		return new ArrayList<>(this.content);
	}
	
}
