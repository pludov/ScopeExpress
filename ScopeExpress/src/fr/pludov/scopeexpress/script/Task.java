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
	
	protected final TaskGroup taskGroup;
	private Status status;
	// La tache parent ne peut pas être done (on le vide/passe au parent eventuellement)
	Task parent;
	Throwable error;
	Object result;
	
	List<Runnable> onDone;
	List<Runnable> onProduced;

	
	public Task(TaskGroup tg) {
		this.taskGroup = tg;
		this.parent = null;
		setStatus(Status.Pending);
	}

	public Task(Task parent) {
		this.parent = parent;
		this.taskGroup = parent.taskGroup;
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
	
	public boolean isTerminated() {
		return status == Status.Done;
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
				taskGroup.reparent(this);
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

	public void onProduced(Runnable callback) {
		if (onProduced == null) {
			onProduced = new ArrayList<>();
		}
		onProduced.add(callback);
	}
	
	public void removeOnProduced(Runnable callback) {
		if (onProduced != null) {
			onProduced.remove(callback);
		}
	}
	
	List<Task> getChilds()
	{
		List<Task> result = new ArrayList<>();
		for(Task child : this.taskGroup.allTasks) {
			if (child.parent == this) {
				result.add(child);
			}
		}
		return result;
	}
	
	public Object readProduced() { return null; }
	
	abstract void advance();
	
	// Pas de garantie que la tache s'arrete tout de suite.
	// Doit propager aux taches filles.
	abstract void cancel();

}
