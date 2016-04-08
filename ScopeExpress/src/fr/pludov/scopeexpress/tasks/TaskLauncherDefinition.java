package fr.pludov.scopeexpress.tasks;

import java.util.*;

public class TaskLauncherDefinition {
	final String id;
	String title;
	final BaseTaskDefinition fromTask;
	final BaseTaskDefinition startedTask;
	final List<TaskLauncherOverride<?>> overrides;
	
	public TaskLauncherDefinition(BaseTaskDefinition fromTask, String id, BaseTaskDefinition startedTask) {
		overrides = new ArrayList<>();
		this.id = id;
		this.title = startedTask.getTitle();
		this.fromTask = fromTask;
		this.startedTask = startedTask;
		
		fromTask.declareLauncher(this);
	}

	public String getId() {
		return id;
	}

	public BaseTaskDefinition getStartedTask() {
		return startedTask;
	}

	public List<TaskLauncherOverride<?>> getOverrides() {
		return new ArrayList<>(overrides);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "TaskLauncherDefinition:" + getId();
	}
}
