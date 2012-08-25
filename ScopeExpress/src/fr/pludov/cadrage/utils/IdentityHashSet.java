package fr.pludov.cadrage.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

public class IdentityHashSet<E> implements Set<E>, Serializable{
	private static final long serialVersionUID = -66174827635860294L;
	
	IdentityHashMap<E, Integer> map;
	static final Integer zero = 0;
	
	public IdentityHashSet() {
		map = new IdentityHashMap<E, Integer>();
		
	}


	Set<E> getSet() {
		return map.keySet();
	}


	public int size() {
		return getSet().size();
	}


	public boolean isEmpty() {
		return getSet().isEmpty();
	}


	public boolean contains(Object o) {
		return getSet().contains(o);
	}


	public Iterator<E> iterator() {
		return getSet().iterator();
	}


	public Object[] toArray() {
		return getSet().toArray();
	}


	public <T> T[] toArray(T[] a) {
		return getSet().toArray(a);
	}


	public boolean add(E e) {
		Integer old = map.put(e, zero);
		return old == null;
	}


	public boolean remove(Object o) {
		return getSet().remove(o);
	}


	public boolean containsAll(Collection<?> c) {
		return getSet().containsAll(c);
	}


	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for(E e : c) {
			result |= add(e);
		}
		return result;
	}


	public boolean retainAll(Collection<?> c) {
		return getSet().retainAll(c);
	}


	public boolean removeAll(Collection<?> c) {
		return getSet().removeAll(c);
	}


	public void clear() {
		getSet().clear();
	}


	public boolean equals(Object o) {
		return getSet().equals(o);
	}


	public int hashCode() {
		return getSet().hashCode();
	}
	
}
