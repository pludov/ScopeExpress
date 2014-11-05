package fr.pludov.scopeexpress.ui.utils;

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
		public final boolean isRunning;
		public final boolean isFinal;
		
		Status(boolean isPending, boolean isRunning, boolean isFinal)
		{
			this.isPending = isPending;
			this.isRunning = isRunning;
			this.isFinal = isFinal;
		}
	};
	
	
	private String title;
	private String runningDetails;
	private int percent;
	
	long startTime;
	long endTime;
	// Positionné par BackgroundTaskQueue.addTask
	BackgroundTaskQueue queue;
	Status status;

	public BackgroundTask(String title)
	{
		this.title = title;
		this.runningDetails = "";
		this.percent = 0;
		this.status = Status.Pending;
		this.queue = null;
		this.startTime = -1;
		this.endTime = -1;
	}

	/**
	 * Doit uniquement être appellé depuis le thread swing
	 */
	public void abort()
	{
		queue.abortTask(this);
	}
	
	// Indique si la tache est prete à être exécuté
	protected boolean isReady()
	{
		return true;
	}

	/**
	 * Indique si du point de vue ressource, cette tache est particulièrement intéressante à lancer maintenant
	 * c.a.d les données dont elle a besoin sont déjà dispo
	 */
	public abstract int getResourceOpportunity();
	
	// Ceci est appellé dès que isReady est vrai et que le nombre de taches en attente le permet
	protected abstract void proceed() throws BackgroundTaskCanceledException, Throwable;
	
	// Appellé dans tous les cas à la fin de l'exécution de la tache, ou après une annulation
	// exécuté dans le thread swing
	protected void onDone() {}
	
	/// C.f. SwingThreadMonitor
	@Deprecated
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
	
	protected void waitForEndOfRunningStatus() throws InterruptedException
	{
		synchronized(queue)
		{
			while (status == Status.Running) {
				queue.wait();
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
			queue.detailsChanged();
		}
	}

	public String getRunningDetails() {
		return runningDetails;
	}

	public void setRunningDetails(String subTitle) {
		if (queue == null) {
			this.runningDetails = subTitle;
			return;
		}
		
		if (this.runningDetails.equals(subTitle)) return;
		synchronized(queue)
		{
			if (this.runningDetails.equals(subTitle)) return;
			this.runningDetails = subTitle;
			queue.detailsChanged();
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
			queue.detailsChanged();
		}
	}

	public Status getStatus() {
		return status;
	}
	
	public static class BackgroundTaskCanceledException extends Exception
	{
	}

	public static class TaskException extends Exception
	{
		private static final long serialVersionUID = -4691730829592873848L;

		public TaskException(String message, Throwable cause) {
			super(message, cause);
		}

		public TaskException(String message) {
			super(message);
		}

		public TaskException(Throwable cause) {
			super(cause);
		}
	}
	
	
	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public BackgroundTaskQueue getQueue() {
		return queue;
	}
}
