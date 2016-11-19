package fr.pludov.scopeexpress.script;

import java.util.*;

public class TaskGroup {
	final static ThreadLocal<TaskGroup> current = new ThreadLocal<>();
	
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
		


	public void addRunnableTask(Task jsTask) {
		this.runnableTasks.add(jsTask);
	}


	public void removeRunnableTask(Task jsTask) {
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

	public static TaskGroup getCurrent() {
		TaskGroup result = current.get();
		assert(result != null);
		return result;
	}
}
