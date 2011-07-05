package fr.pludov.cadrage.utils;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

public class IdentityHashSet<E> implements Set<E> {
	IdentityHashMap<E, Integer> map;
	Set<E> set;
	static final Integer zero = 0;
	
	public IdentityHashSet() {
		map = new IdentityHashMap<E, Integer>();
		set = map.keySet();
	}


	public int size() {
		return set.size();
	}


	public boolean isEmpty() {
		return set.isEmpty();
	}


	public boolean contains(Object o) {
		return set.contains(o);
	}


	public Iterator<E> iterator() {
		return set.iterator();
	}


	public Object[] toArray() {
		return set.toArray();
	}


	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}


	public boolean add(E e) {
		Integer old = map.put(e, zero);
		return old == null;
	}


	public boolean remove(Object o) {
		return set.remove(o);
	}


	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}


	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for(E e : c) {
			result |= add(e);
		}
		return result;
	}


	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}


	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}


	public void clear() {
		set.clear();
	}


	public boolean equals(Object o) {
		return set.equals(o);
	}


	public int hashCode() {
		return set.hashCode();
	}
	
}
