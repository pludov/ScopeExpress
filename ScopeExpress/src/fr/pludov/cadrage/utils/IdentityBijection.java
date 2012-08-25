package fr.pludov.cadrage.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;

public class IdentityBijection<A, B> implements Serializable{
	private static final long serialVersionUID = -3056452260744129872L;
	
	IdentityHashMap<A, B> directMap = new IdentityHashMap<A, B>();
	IdentityHashMap<B, A> reverseMap = new IdentityHashMap<B, A>();
	
	public Collection<A> getSourceSet()
	{
		return Collections.unmodifiableCollection(directMap.keySet());
	}
	
	public B get(A a) {
		return directMap.get(a);
	}

	public A reverseGet(B b) {
		return reverseMap.get(b);
	}
	
	public void put(A a, B b) {
		directMap.put(a, b);
		reverseMap.put(b, a);
	}
	
	// Indique si un mapping est possible
	public boolean exists(A a, B b)
	{
		return directMap.containsKey(a) || reverseMap.containsKey(b);
	}
}
