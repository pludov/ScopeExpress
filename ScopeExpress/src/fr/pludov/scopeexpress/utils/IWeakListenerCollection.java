package fr.pludov.scopeexpress.utils;

import java.util.Iterator;

public interface IWeakListenerCollection<Interface> {

	public void addListener(WeakListenerOwner owner, Interface i);
	
	public void removeListener(WeakListenerOwner owner);
}
