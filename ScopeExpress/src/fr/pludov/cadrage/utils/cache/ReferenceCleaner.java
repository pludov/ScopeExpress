package fr.pludov.cadrage.utils.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;

class ReferenceCleaner extends Thread {
	
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
						
						System.out.println("Dead ref: " + cacheEntry.identifier);
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
