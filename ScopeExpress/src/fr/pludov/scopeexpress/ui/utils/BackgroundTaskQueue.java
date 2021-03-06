package fr.pludov.scopeexpress.ui.utils;

import java.util.*;

import javax.swing.*;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.utils.BackgroundTask.*;
import fr.pludov.scopeexpress.utils.*;

/**
 * Les ajouts/suppressions sont fait dans le thread swing
 */
public final class BackgroundTaskQueue {
	static final private Logger logger = Logger.getLogger(BackgroundTaskQueue.class);
	
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
	
	public NativeTask coWork(BackgroundTask task)
	{
		addTask(task);
		return new NativeTask() {
			
			boolean tryToFinish() {
				switch(task.getStatus()) {
				case Aborted:
					failed("aborted");
					return true;
				case Canceled:
					failed("canceled");
					return true;
				case Done:
					done(null);
					return true;
				default:
					return false;
				}
			}
			
			@Override
			protected void cancel() {
				super.cancel();
				try {
					switch(task.getStatus()) {
					case Pending:
					case Running:
						task.abort();
					}
				} catch(Throwable t) {
					logger.warn("Failed to abort task", t);
				}
			}
			
			@Override
			protected void init() throws Throwable {
				
				if (!tryToFinish()) {
					listeners.addListener(this.listenerOwner, new BackgroundTaskQueueListener() {
						
						@Override
						public void stateChanged() {
							tryToFinish();
						}
					});
				}
			}
		};
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
	 * Doit �tre appell� uniquement depuis le thread swing
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
				// Pas de changement pour ces cas l�
				return;
			}
			somethingChanged();
			this.notifyAll();
		}
		
		try {
			task.onDone();
			task.listeners.getTarget().onDone();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	// Les taches ne changent pas d'�tat hors du thread Swing.
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
	 * Execut� dans le thread swing uniquement
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
				task.listeners.getTarget().onDone();
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	/**
	 * Execut� dans le thread swing uniquement
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
		logger.info("Checking for task ready to start");
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
	
	Runnable periodic = new Runnable()
	{
		@Override
		public void run() {
			boolean wantRemoveAddTask;
			synchronized(periodic) {
				pending = false;
				wantRemoveAddTask = removeAddTaskPending;
				removeAddTaskPending = false;
			}
			
			if (wantRemoveAddTask) {
				removeDoneTasks();
				startSomeTask();
			}
			listeners.getTarget().stateChanged();
		}
	};
	boolean pending;
	boolean removeAddTaskPending;
	
	// Appell� quand quelque chose change.
	// Peut relancer une nouvelle tache, d�clencher la mise � jour des titre & co, ...
	void somethingChanged()
	{
		synchronized(periodic)
		{
			if (!pending) {
				SwingUtilities.invokeLater(periodic);
				pending = true;
			}
			removeAddTaskPending = true;
		}
	}
	
	void detailsChanged()
	{
		synchronized(periodic)
		{
			if (!pending) {
				SwingUtilities.invokeLater(periodic);
				pending = true;
			}
		}
	}

}
