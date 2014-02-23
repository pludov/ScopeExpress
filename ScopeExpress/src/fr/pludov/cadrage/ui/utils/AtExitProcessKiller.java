package fr.pludov.cadrage.ui.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.utils.IdentityHashSet;

/**
 * Centralise la terminaison automatique des process externe à la sortie du programme
 * 
 * Usage:
 *     AtExitProcessKiller.add(Process p)
 *     try {
 *     	
 *     		p.destroy();
 *     } finally {
 *     	    AtExitProcessKiller.remove(p)
 *     }
 */
public class AtExitProcessKiller extends Thread {
	final static Logger logger = Logger.getLogger(AtExitProcessKiller.class);
	final IdentityHashSet<Process> runningProcesses = new IdentityHashSet<Process>();
	
	private static AtExitProcessKiller instance = null;  
	
	public static synchronized AtExitProcessKiller getInstance() {
		if (instance == null) {
			AtExitProcessKiller thread = new AtExitProcessKiller();
			Runtime.getRuntime().addShutdownHook(thread);
			instance = thread;
		}
		return instance;
	}
	
	public synchronized void add(Process p)
	{
		runningProcesses.add(p);
	}
	
	public synchronized void remove(Process p)
	{
		runningProcesses.remove(p);
	}
	
	@Override
	public void run() {
		List<Process> tokill;
		synchronized(this) {
			tokill = new ArrayList<Process>(this.runningProcesses);
		}
		
		for(Process p : tokill) {
			try {
				p.destroy();
			} catch(Throwable t) {
				logger.error("kill failed", t);
			}
		}
	}
}