package fr.pludov.scopeexpress.utils;

import java.util.*;

public class WeakListenerOwner {
	final Object owner;
	final IdentityHashMap<Object, Integer> listeners;
	volatile boolean dead;
	
	public WeakListenerOwner(Object realOwner) {
		this.owner = realOwner;
		this.listeners = new IdentityHashMap<Object, Integer>();
		this.dead = false;
	}
	
	public void kill()
	{
		this.dead = true;
		this.listeners.clear();
	}
	
	void addListener(Object listener)
	{
		assert(!dead);
		Integer oldCount = listeners.put(listener, 1);
		if (oldCount != null) {
			listeners.put(listener, 1 + oldCount);
		}
	}
	
	void removeListener(Object listener)
	{
		assert(!dead);
		Integer count = listeners.remove(listener);
		if (count != null && count.intValue() > 1) {
			listeners.put(listener, count.intValue() - 1);
		}
	}

}
