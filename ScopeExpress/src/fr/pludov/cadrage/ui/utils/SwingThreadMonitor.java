package fr.pludov.cadrage.ui.utils;

import javax.swing.SwingUtilities;

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
	
	int lockCount;
	Runnable synchronizer;
	
	
	public SwingThreadMonitor()
	{
		
	}
	
	public void acquire()
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
					System.out.println("acquired swing global lock");
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
					System.out.println("released swing global lock");
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
	
	public void release()
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
}
