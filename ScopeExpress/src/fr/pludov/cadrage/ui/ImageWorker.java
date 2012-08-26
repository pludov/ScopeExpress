package fr.pludov.cadrage.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public abstract class ImageWorker {
	private static final Logger logger = Logger.getLogger(ImageWorker.class);
	volatile boolean canceled;
	boolean started;
	
	private static final LinkedList<ImageWorker> queue = new LinkedList<ImageWorker>();// new Queue<ImageWorker>(2 * Runtime.getRuntime().availableProcessors());
	
	private static void runThread() throws InterruptedException
	{
		while(true)
		{
			final ImageWorker worker;
			synchronized(queue)
			{
				while(queue.isEmpty()) {
					queue.wait();
				}
				worker = queue.removeFirst();
				worker.started = true;
			}
			
			worker.run();
			if (!worker.canceled) {
				boolean doRun;
				synchronized(queue) {
					doRun = !worker.canceled;
				}
				if (doRun) {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								synchronized(queue) {
									if (!worker.canceled) {
										worker.done();
									}
								}
							}
						});
					} catch(InvocationTargetException e) {
						logger.warn("Exception", e);
					}
				}
			}
		}
	}
	
	static { 
		for(int i = 0; i < Runtime.getRuntime().availableProcessors() * 2; ++i) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						runThread();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "ImageWorker " + i).start();
		}
			
	}
	
	public final void cancel()
	{
		synchronized(queue) {
			canceled = true;
		}
	}
	
	public final boolean cancelIfNotStarted()
	{
		synchronized(queue) {
			if (!started) {
				canceled = true;
				return true;
			}
			return false;
		}
	}
	
	public final void queue()
	{
		synchronized(queue) {
			canceled = false;
			started = false;
			queue.add(this);
			queue.notify();
		}
	}
	
	public abstract void run();
	
	public abstract void done();
}
