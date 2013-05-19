package fr.pludov.utils;

public class PerfCounter {

	private static class PerfCounterThreadLocal
	{
		boolean active;
		long when;
		
		PerfCounterThreadLocal parent;
	}
	
	private ThreadLocal<PerfCounterThreadLocal> pctl = new ThreadLocal<PerfCounter.PerfCounterThreadLocal>();
	private long total;
	
	public void enter()
	{
		PerfCounterThreadLocal current = pctl.get();
		if (current == null) {
			current = new PerfCounterThreadLocal();
			pctl.set(current);
		}
		
		if (current.active) {
			PerfCounterThreadLocal temporary = new PerfCounterThreadLocal();
			temporary.active = true;
			temporary.when = System.currentTimeMillis();
			temporary.parent = current;
			
			pctl.set(temporary);
		} else {
			current.active = true;
			current.when = System.currentTimeMillis();
		}
	}
	
	
	public void leave()
	{
		PerfCounterThreadLocal current = pctl.get();
		if (current == null) {
			return;
		}
		if (!current.active) {
			return;
		}
		if (current.parent != null) {
			pctl.set(current.parent);
			return;
		}
		long elapsed = System.currentTimeMillis() - current.when;
		current.active = false;
		synchronized(this)
		{
			total += elapsed;
		}
	}
	
	public PerfCounter() {
	}

	public synchronized long getElapsed()
	{
		return total;
	}
}
