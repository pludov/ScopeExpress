package fr.pludov.cadrage.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * Exécution de code dans un thread précis (pour ole32)
 * 
 * @author Ludovic POLLET
 *
 */
public abstract class WorkThread extends Thread {

	boolean terminated;
	Task periodicTask;
	long interval;
	
	
	protected interface Task
	{
		public Object run() throws Throwable;
	}
	
	protected abstract class AsyncOrder implements Task
	{
		private boolean done;
		private Throwable error;
		private Object result;
		
		public abstract Object run() throws Throwable;
	}
	
	LinkedList<AsyncOrder> todoList = new LinkedList<AsyncOrder>();
	
	protected Object exec(AsyncOrder order) throws Throwable
	{
		order.done = false;
		order.error = null;
		order.result = null;
		synchronized(this)
		{
			todoList.add(order);
			this.notifyAll();
		}
		
		synchronized(order)
		{
			while(!order.done) {
				order.wait();
			}
			
			if (order.error != null) {
				throw order.error;
			}
			
			return order.result;
		}
	}
	
	protected void execAsync(AsyncOrder order) 
	{
		order.done = false;
		order.error = null;
		order.result = null;
		synchronized(this)
		{
			todoList.add(order);
			this.notifyAll();
		}
	}

	protected void setTerminated()
	{
		synchronized(this)
		{
			this.terminated = true;
			notifyAll();
		}
	}
	
	public void setPeriodicTask(Task t, long interval)
	{
		this.periodicTask = t;
		this.interval = interval;
	}
	
	@Override
	public void run() {
		long l = System.currentTimeMillis();
		long nextL = l + interval;
		
		while(true) {
			Task todo;
			synchronized(this)
			{
				long now = System.currentTimeMillis();
				
				while((periodicTask == null || now < nextL) && todoList.isEmpty() && !terminated) {
										
					try {
						if (periodicTask != null)
							this.wait(nextL - now);
						else 
							this.wait();
						
					} catch(InterruptedException ie)
					{
						
					}
					now = System.currentTimeMillis();
				}
				if (terminated) {
					return;
				}
				
				if (periodicTask != null && now >= nextL) {
					while(now >= nextL) {
						nextL += interval; 
					}
					todo = periodicTask;
				} else {
					todo = todoList.removeFirst();
				}
			}
			Object result = null;
			Throwable error = null;
			try {
				result = todo.run();
			} catch(Throwable t) {
				error = t;
			}
			if (todo instanceof AsyncOrder) {
				AsyncOrder order = (AsyncOrder)todo;
				
				synchronized(order)
				{
					
					order.done = true;
					order.result = result;
					order.error = error;
					order.notifyAll();
				}
			}
		} 
	}
}
