package fr.pludov.scopeexpress.utils.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class CacheSoftReference<Identifier, Target> extends SoftReference<Target> {
	final Cache<Identifier, Target> cache;
	
	final Identifier identifier;
	
	// Initialisé a false. Mis à true par Cache.removeFromCache
	boolean removedFromCache;
	
	
	public CacheSoftReference(Cache<Identifier, Target> cache, Identifier identifier, Target referent) {
		super(referent, ReferenceCleaner.getInstance().getQueue());
		this.cache = cache;
		this.removedFromCache = true;
		this.identifier = identifier;
		
	}
}
