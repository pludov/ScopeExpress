package fr.pludov.scopeexpress.script;

import java.util.*;

public class TaskGroup {
	final List<Task> runnableTasks = new ArrayList<>();
	final List<Task> allTasks = new ArrayList<>();

	
	public boolean advance() {
		
		if (runnableTasks.isEmpty()) {
			return false;
		}
		
		Task nextToRun = runnableTasks.remove(0);
		runnableTasks.add(nextToRun);
		nextToRun.advance();
		
		return true;
	}
	

}
