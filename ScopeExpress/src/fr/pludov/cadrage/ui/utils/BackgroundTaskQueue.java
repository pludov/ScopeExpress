package fr.pludov.cadrage.ui.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.ui.utils.BackgroundTask.BackgroundTaskCanceledException;
import fr.pludov.cadrage.ui.utils.BackgroundTask.Status;
import fr.pludov.cadrage.utils.WeakListenerCollection;

/**
 * Les ajouts/suppressions sont fait dans le thread swing
 */
public final class BackgroundTaskQueue {
	private static final Logger logger = Logger.getLogger(BackgroundTaskQueue.class);
	
	public final WeakListenerCollection<BackgroundTaskQueueListener> listeners = new WeakListenerCollection<BackgroundTaskQueueListener>(BackgroundTaskQueueListener.class);
	List<BackgroundTask> tasks;
	int runningCount;
	int maxRunCount;
	
	public BackgroundTaskQueue()
	{
		this.tasks = new ArrayList<BackgroundTask>();
		this.runningCount = 0;
		this.maxRunCount = Runtime.getRuntime().availableProcessors();
		logger.info("Background task queue started with " + maxRunCount + " max active tasks");
	}
	
	public synchronized void addTask(BackgroundTask task)
	{
		logger.info("Adding task: " + task.getTitle());
		task.queue = this;
		tasks.add(task);
		somethingChanged();
	}

	
	public <M extends BackgroundTask> List<M> getTasksWithStatus(Class<M> clazz, Status ... status)
	{
		List<M> result = new ArrayList<M>();
		synchronized(this)
		{
			for(BackgroundTask task : this.tasks)
			{
				if (clazz.isAssignableFrom(task.getClass()))
				{
					M taskOfType = (M)task;
					
					for(int i = 0; i < status.length; ++i)
					{
						if (taskOfType.getStatus() == status[i]) {	
							result.add(taskOfType);
							break;
						}
					}
				}
			}
		}
		return result;
	}
	/**
	 * Doit être appellé uniquement depuis le thread swing
	 */
	public void abortTask(BackgroundTask task)
	{
		logger.info("Abort task: " + task.getTitle());

		synchronized(this)
		{
			switch(task.status) {
			case Running:
				task.status = Status.Aborting;
				break;
			case Pending:
				task.status = Status.Canceled;
				break;
			case Aborting:
			case Aborted:
			case Canceled:
			case Done:
				// Pas de changement pour ces cas là
				return;
			}
			somethingChanged();

		}
		
		try {
			task.onDone();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	// Les taches ne changent pas d'état hors du thread Swing.
	public synchronized List<BackgroundTask> getRunningTasks()
	{
		List<BackgroundTask> result = new ArrayList<BackgroundTask>();
		for(BackgroundTask task : tasks)
		{
			if (task.status.isRunning)
			{
				result.add(task);
			}
		}
		return result;
	}

	/**
	 * Executé dans le thread swing uniquement
	 */
	synchronized void taskDone(final BackgroundTask task)
	{
		logger.info("Task done: " + task.getTitle());

		boolean callOnDone;
		synchronized(this)
		{
			task.endTime = System.currentTimeMillis();
			this.runningCount--;
			callOnDone = !task.status.isFinal;
			switch(task.status)
			{
			case Aborting:
				task.status = Status.Aborted;
				break;
			default:
				task.status = Status.Done;
			}
			
			somethingChanged();
		}
		if (callOnDone) {
			try {
				task.onDone();
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	/**
	 * Executé dans le thread swing uniquement
	 */
	synchronized void startTask(final BackgroundTask task)
	{
		logger.info("Starting task: " + task.getTitle());

		task.status = Status.Running;
		task.startTime = System.currentTimeMillis();
		Thread t = new Thread(task.getTitle())
		{
			@Override
			public void run() {
				try {
					task.checkInterrupted();
					task.proceed();
				} catch(BackgroundTaskCanceledException e) {
					logger.info("Task canceled: " + task.getTitle());
				} catch(Throwable t) {
					logger.error("Task error for " + task.getTitle(), t);
				} finally {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							taskDone(task);
						}
					});
				}
			}
		};
		
		t.start();
		
		somethingChanged();
	}
	
	synchronized void startSomeTask()
	{
		if (runningCount >= maxRunCount) return;
		for(BackgroundTask task : tasks)
		{
			if ((task.status == Status.Pending) && task.isReady())
			{
				startTask(task);
				runningCount++;
				if (runningCount >= maxRunCount) return;
			}
		}
	}
	
	synchronized void removeDoneTasks()
	{
		long now = System.currentTimeMillis();
		boolean somethingChanged = false;
		for(Iterator<BackgroundTask> it = tasks.iterator(); it.hasNext();)
		{
			BackgroundTask task = it.next();
			
			if (!task.getStatus().isFinal) continue;
			if (task.endTime + 10000 > now) {
				it.remove();
				somethingChanged = true;
			}
			
		}
		if (somethingChanged) {
			this.somethingChanged();
		}
	}
	
	
	// Appellé quand quelque chose change.
	// Peut relancer une nouvelle tache, déclencher la mise à jour des titre & co, ...
	void somethingChanged()
	{
		Runnable periodic = new Runnable()
		{
			@Override
			public void run() {
				removeDoneTasks();
				startSomeTask();
				listeners.getTarget().stateChanged();
			}
		};
		
		SwingUtilities.invokeLater(periodic);
	}
}
