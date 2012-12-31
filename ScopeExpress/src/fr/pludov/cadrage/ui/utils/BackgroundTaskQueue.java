package fr.pludov.cadrage.ui.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import fr.pludov.cadrage.ui.utils.BackgroundTask.BackgroundTaskCanceledException;
import fr.pludov.cadrage.ui.utils.BackgroundTask.Status;
import fr.pludov.cadrage.utils.WeakListenerCollection;

/**
 * Les ajouts/suppressions sont fait dans le thread swing
 */
public final class BackgroundTaskQueue {
	public final WeakListenerCollection<BackgroundTaskQueueListener> listeners = new WeakListenerCollection<BackgroundTaskQueueListener>(BackgroundTaskQueueListener.class);
	List<BackgroundTask> tasks;
	int runningCount;
	int maxRunCount;
	
	public BackgroundTaskQueue()
	{
		this.tasks = new ArrayList<BackgroundTask>();
		this.runningCount = 0;
		this.maxRunCount = Runtime.getRuntime().availableProcessors();
	}
	
	public synchronized void addTask(BackgroundTask task)
	{
		task.queue = this;
		tasks.add(task);
		somethingChanged();
	}

	/**
	 * Doit être appellé uniquement depuis le thread swing
	 */
	public void abortTask(BackgroundTask task)
	{
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
		boolean callOnDone;
		synchronized(this)
		{
			task.endTime = System.currentTimeMillis();
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
				} catch(Throwable t) {
					t.printStackTrace();
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
