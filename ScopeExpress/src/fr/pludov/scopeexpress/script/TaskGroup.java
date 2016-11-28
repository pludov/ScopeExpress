package fr.pludov.scopeexpress.script;

import java.util.*;

import fr.pludov.scopeexpress.utils.*;

public class TaskGroup implements TaskOrGroup {
	final static ThreadLocal<TaskGroup> current = new ThreadLocal<>();
	
	final WeakListenerCollection<TaskStatusListener> statusListeners = new WeakListenerCollection<>(TaskStatusListener.class);
	final WeakListenerCollection<TaskChildListener> childListeners = new WeakListenerCollection<>(TaskChildListener.class);
	
	private final List<Task> runnableTasks = new ArrayList<>();
	final List<Task> allTasks = new ArrayList<>();
	
	boolean runningFlag = false;
	
	public boolean advance() {
		
		if (runnableTasks.isEmpty()) {
			return false;
		}
		if (runningFlag == true) {
			// Pas de reentrance... Mark for restart
			return false;
		}
		
		TaskGroup prev = current.get();
		current.set(this);
		runningFlag = true;
		try {
			
			Task nextToRun = runnableTasks.remove(0);
			runnableTasks.add(nextToRun);
			nextToRun.advance();
			
			return true;
		} finally {
			current.set(prev);
			runningFlag = false;
		}
	}
		


	protected void addRunnableTask(Task jsTask) {
		this.runnableTasks.add(jsTask);
	}


	protected void removeRunnableTask(Task jsTask) {
		this.runnableTasks.remove(jsTask);
		
	}

	/** Un parent vient d'etre retiré... Reporte la modif chez ses fils */
	void reparent(Task removedTask)
	{
		for(Task t : this.allTasks) {
			if (t.parent == removedTask) {
				t.parent = removedTask.parent;
			}
		}
	}

	@Override
	public TaskOrGroup getParent() {
		return null;
	}
	
	@Override
	public List<Task> getChilds() {
		return new ArrayList<>(this.allTasks);
	}
	
	@Override
	public String getTitle() {
		return "TODO";
	}
	
	@Override
	public String getStatusIconId() {
		return "status-running";
	}
	
	@Override
	public boolean isTerminated() {
		return this.allTasks.isEmpty();
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
	
	public static TaskGroup getCurrent() {
		TaskGroup result = current.get();
		assert(result != null);
		return result;
	}
}
