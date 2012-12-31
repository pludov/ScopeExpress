package fr.pludov.cadrage.ui.utils;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public abstract class BackgroundTask {
	public static enum Status {
		// En attente
		Pending (true, false, false),
		// En cours
		Running (false, true, false),
		Aborting (false, true, false),
		// Etat finaux
		Canceled (false, false, true),
		Done (false, false, true),
		Aborted (false, false, true);
		
		public final boolean isPending;
		final boolean isRunning;
		final boolean isFinal;
		
		Status(boolean isPending, boolean isRunning, boolean isFinal)
		{
			this.isPending = isPending;
			this.isRunning = isRunning;
			this.isFinal = isFinal;
		}
	};
	
	
	private String title;
	private int percent;
	
	long startTime;
	long endTime;
	BackgroundTaskQueue queue;
	Status status;

	public BackgroundTask(String title)
	{
		this.title = title;
		this.percent = 0;
		this.status = Status.Pending;
		this.queue = null;
		this.startTime = -1;
		this.endTime = -1;
	}
	
	// Indique si la tache est prete à être exécuté
	protected boolean isReady()
	{
		return true;
	}
	
	// Ceci est appellé dès que isReady est vrai et que le nombre de taches en attente le permet
	protected abstract void proceed() throws BackgroundTaskCanceledException, Throwable;
	
	
	protected final void runSync(Runnable runnable) throws BackgroundTaskCanceledException, InvocationTargetException
	{
		checkInterrupted();
		try {
			SwingUtilities.invokeAndWait(runnable);
		} catch(InterruptedException e) {
			throw new BackgroundTaskCanceledException();
		}
		checkInterrupted();
	}

	/**
	 * Peut-être appellé par proceed.
	 */
	protected final void checkInterrupted() throws BackgroundTaskCanceledException
	{
		if (queue == null) {
			return;
		}
		synchronized(queue)
		{
			switch(status)
			{
			case Aborting:
			case Aborted:
				throw new BackgroundTaskCanceledException();
			default:
				return;
			}
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (queue == null) {
			this.title = title;
			return;
		}
		
		if (this.title.equals(title)) return;
		synchronized(queue)
		{
			if (this.title.equals(title)) return;
			this.title = title;
			queue.somethingChanged();
		}
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		if (queue == null) {
			this.percent = percent;
			return;
		}
		
		if (this.percent == percent) return;
		synchronized(queue)
		{
			if (this.percent == percent) return;
			this.percent = percent;
			queue.somethingChanged();
		}
	}

	public Status getStatus() {
		return status;
	}
	
	public static class BackgroundTaskCanceledException extends Exception
	{
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}
}
