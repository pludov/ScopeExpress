package fr.pludov.scopeexpress.ui.utils;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

/**
 * Ce monitor permet d'avoir un accès exclusif au thread de dispatch des evenements:
 * il bloque tous les autres evenements pendant qu'il est acquéri.
 * 
 * Je ne sais pas si il est correcte d'appeler des fonctions swing si on a ce lock
 * (il y a peut être des threadLocal qui permettraient de faire la différence)
 * 
 * En tout cas, ça devrait permettre d'acceder sereinement aux données spécifique au programme.
 */
public class SwingThreadMonitor {
	private static final Logger logger = Logger.getLogger(SwingThreadMonitor.class);
	
	int lockCount;
	Runnable synchronizer;
	
	
	private SwingThreadMonitor()
	{
		
	}
	
	private void doAcquire()
	{
		if (SwingUtilities.isEventDispatchThread()) {
			return;
		}
		synchronized(this)
		{
			if (lockCount > 0) {
				lockCount++;
				return;
			}
			
			this.synchronizer = new Runnable()
			{
				@Override
				public void run() {
					logger.debug("acquired swing global lock");
					synchronized(SwingThreadMonitor.this)
					{
						SwingThreadMonitor.this.lockCount++;
						SwingThreadMonitor.this.notifyAll();
						while(SwingThreadMonitor.this.synchronizer == this)
						{
							try {
								SwingThreadMonitor.this.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					logger.debug("released swing global lock");
				}
			};
			
			SwingUtilities.invokeLater(this.synchronizer);
			
			while(lockCount == 0)
			{
				try {
					this.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private void doRelease()
	{
		if (SwingUtilities.isEventDispatchThread()) {
			return;
		}
		synchronized(this)
		{
			lockCount--;
			if (lockCount <= 0) {
				this.synchronizer = null;
				this.notifyAll();
			}
		}
		
	}
	
	private static final ThreadLocal<SwingThreadMonitor> lockByThread = new ThreadLocal<SwingThreadMonitor>();
	
	public static void acquire()
	{
		SwingThreadMonitor monitor = lockByThread.get();
		if (monitor == null) {
			monitor = new SwingThreadMonitor();
			lockByThread.set(monitor);
		}
		
		monitor.doAcquire();
	}
	
	public static void release()
	{
		SwingThreadMonitor monitor = lockByThread.get();
		if (monitor == null) {
			throw new NullPointerException("probable monitor lock/unlock mismatch");
		}
		monitor.doRelease();
	}
}
