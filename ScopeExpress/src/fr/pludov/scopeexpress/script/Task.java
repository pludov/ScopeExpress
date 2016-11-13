package fr.pludov.scopeexpress.script;

import java.lang.reflect.*;
import java.util.*;

/** Tout est executé en synchrone dans le thread swing */
public abstract class Task {

	public enum Status {
		/** En attente d'initialisation */
		Pending,
		Blocked,
		/** Réservé aux JSTask... */
		Runnable,
		/** Permet de garder des taches "en cours d'execution" (deboggage) */
		Running,
		Done
	}
	
	final TaskGroup taskGroup;
	private Status status;
	Throwable error;
	Object result;
	
	List<Runnable> onDone;
	
	
	public Task(TaskGroup tg) {
		this.taskGroup = tg;
		setStatus(Status.Pending);
	}

	public Object getResult() {
		assert(status == Status.Done);
		if (error != null) {
			if (error instanceof RuntimeException) throw (RuntimeException)error;
			throw new UndeclaredThrowableException(error);
		}
		
		return result;
	}
	
	public Status getStatus() {
		return status;
	}
	
	protected void setStatus(Status newStatus)
	{
		if (status == newStatus) return;
		
		boolean wasActive = status != Status.Done && status != null;
		boolean isActive = newStatus != Status.Done && newStatus != null;
		
		boolean wasRunnable = status == Status.Runnable || status == Status.Pending;
		boolean isRunnable = newStatus == Status.Runnable || newStatus == Status.Pending;
		
		
		status = newStatus;
		
		if (wasActive != isActive) {
			if (isActive) {
				taskGroup.allTasks.add(this);
			} else {
				taskGroup.allTasks.remove(this);
			}
		}
		
		if (wasRunnable != isRunnable) {
			if (isRunnable) {
				taskGroup.addRunnableTask(this);
			} else {
				taskGroup.removeRunnableTask(this);
			}
		}
		
		
		
		
		if (status == Status.Done && onDone != null) {
			for(Runnable r : onDone) {
				r.run();
			}
			onDone = null;
		}
		
	}

	public void onDone(Runnable callback) {
		if (onDone == null) {
			onDone = new ArrayList<>();
		}
		onDone.add(callback);
	}

	abstract void advance();

}
