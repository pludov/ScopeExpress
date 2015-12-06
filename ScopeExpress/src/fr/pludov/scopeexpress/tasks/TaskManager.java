package fr.pludov.scopeexpress.tasks;

import java.util.*;

import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public class TaskManager implements ITaskParent {
	public static interface TaskManagerListener {
		void childAdded(BaseTask bt, BaseTask justAfter);
		void childRemoved(BaseTask bt);
	}

	public final WeakListenerCollection<TaskManagerListener> statusListeners = new WeakListenerCollection<>(TaskManagerListener.class);

	final List<BaseTask> runningTasks;
	BaseTask first, last;
	
	public TaskManager() {
		runningTasks = new ArrayList<>();
	}

	
	public BaseTask startTask(FocusUi focusUi, BaseTaskDefinition taskDef, ITaskParameterView parameters) {
		BaseTask task = taskDef.build(focusUi, this, null);
		
		task.setParameters(parameters);
		BaseTask previous = runningTasks.isEmpty() ? null : runningTasks.get(runningTasks.size() - 1);
		runningTasks.add(task);
		statusListeners.getTarget().childAdded(task, previous);
		
		try {
			task.start();
		} catch(TaskInterruptedException e) {}
		
		focusUi.selectTask(task);
		
		return task;
	}


	public List<BaseTask> getRunningTasks() {
		return runningTasks;
	}


	public boolean removeTask(BaseTask task) {
		for(int i = 0; i < runningTasks.size(); ++i)
		{
			if (runningTasks.get(i) == task) {
				runningTasks.remove(i);
				task.dettach();
				statusListeners.getTarget().childRemoved(task);
				return true;
			}
		}
		return false;
	}


	public BaseTask getFirst() {
		return first;
	}


	public void setFirst(BaseTask first) {
		this.first = first;
	}


	public BaseTask getLast() {
		return last;
	}


	public void setLast(BaseTask last) {
		this.last = last;
	}
}
