package fr.pludov.scopeexpress.utils.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.apache.log4j.Logger;

class ReferenceCleaner extends Thread {
	private static final Logger logger = Logger.getLogger(ReferenceCleaner.class);
	
	final ReferenceQueue queue;
	
	ReferenceCleaner()
	{
		this.queue = new ReferenceQueue();
	}
	
	ReferenceQueue getQueue()
	{
		return queue;
	}
	
	@Override
	public void run() {
		while(true)
		{
			try {
				Reference ref;
				while((ref = queue.remove()) != null)
				{
					if (ref instanceof CacheSoftReference)
					{
						CacheSoftReference cacheEntry = (CacheSoftReference)ref;
						
						logger.debug("Dead ref: " + cacheEntry.identifier);
						cacheEntry.cache.removeFromCache(cacheEntry);
					}
				}
				
			} catch(Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
		
	static ReferenceCleaner instance;
	
	public synchronized static ReferenceCleaner getInstance()
	{
		if (instance == null) {
			instance = new ReferenceCleaner();
			instance.start();
		}
		return instance;
	}
	
	
}
