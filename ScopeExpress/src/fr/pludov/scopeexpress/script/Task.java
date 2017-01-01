package fr.pludov.scopeexpress.script;

import java.lang.reflect.*;
import java.util.*;

import fr.pludov.scopeexpress.utils.*;

/** Tout est executé en synchrone dans le thread swing */
public abstract class Task implements TaskOrGroup {

	final WeakListenerCollection<TaskStatusListener> statusListeners = new WeakListenerCollection<>(TaskStatusListener.class);
	final WeakListenerCollection<TaskChildListener> childListeners = new WeakListenerCollection<>(TaskChildListener.class);
	
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
	
	@Override
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
			while(!onDone.isEmpty()) {
				onDone.remove(0).run();
			}
			onDone = null;
		}
		
		statusListeners.getTarget().statusChanged();
		if (wasActive != isActive) {
			if (isActive) {
				taskGroup.childListeners.getTarget().childAdded(this);
			} else {
				taskGroup.childListeners.getTarget().childRemoved(this);
			}
		}
		if (wasActive && taskGroup.isTerminated()) {
			taskGroup.statusListeners.getTarget().statusChanged();
		}
	}

	public void onDone(Runnable callback) {
		if (onDone == null) {
			onDone = new LinkedList<>();
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

	@Override
	public String getTitle() {
		return "TODO: title";
	}
	
	@Override
	public String getStatusIconId() {
		// FIXME !
		return "status-running";
	}
	
	@Override
	public TaskOrGroup getParent() {
		return parent != null ? parent : taskGroup;
	}
	
	@Override
	public WeakListenerCollection<TaskStatusListener> getStatusListeners()
	{
		return statusListeners;
	}
	
	@Override
	public WeakListenerCollection<TaskChildListener> getChildListeners()
	{
		return childListeners;
	}
	
	@Override
	public List<Task> getChilds()
	{
		return Collections.emptyList();
	}
	
	public Object readProduced() { return null; }
	
	abstract void advance();
	
	// Pas de garantie que la tache s'arrete tout de suite.
	// Doit propager aux taches filles.
	abstract void cancel();

}
