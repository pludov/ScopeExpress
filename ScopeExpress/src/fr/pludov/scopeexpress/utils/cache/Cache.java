package fr.pludov.scopeexpress.utils.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Cache<Identifier, Target> {
	
	final Map<Identifier, CacheSoftReference<Identifier, Target>> backing;
	final Set<Identifier> producing;
	
	public Cache() {
		this.backing = new HashMap<Identifier, CacheSoftReference<Identifier, Target>>();
		this.producing = new HashSet<Identifier>();
	}
	
	synchronized void removeFromCache(CacheSoftReference<Identifier, Target> cacheEntry)
	{
		if (!cacheEntry.removedFromCache) {
			cacheEntry.removedFromCache = true;
			backing.remove(cacheEntry.identifier);
		}
	}
	
	public Target get(Identifier id)
	{
		synchronized(this)
		{
			while(true){
				CacheSoftReference<Identifier, Target> cacheSoftReference = backing.get(id);
				
				if (cacheSoftReference != null) {
					Target target = cacheSoftReference.get();
					if (target != null) return target;
					removeFromCache(cacheSoftReference);
				}
				
				if (producing.add(id)) {
					// On a réussi à s'enregistrer pour la production
					break;
				}
				// Attendre que la production ait été faite...
				try {
					wait();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Si on arrive ici, il faut produire.
		Target result = null;
		try {
			
			result = produce(id);
			
			return result;
			
		} finally {
			synchronized(this)
			{
				producing.remove(id);
				if (result != null) {
					CacheSoftReference<Identifier, Target> cacheSoftReference;
					cacheSoftReference = new CacheSoftReference<Identifier, Target>(this, id, result);
					this.backing.put(id, cacheSoftReference);
				}
				this.notifyAll();
			}
		}
		
	}
	
	public abstract Target produce(Identifier identifier);
}
