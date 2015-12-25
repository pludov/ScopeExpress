package fr.pludov.scopeexpress.tasks;

import java.util.*;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

/** Pas de synchro, tout est géré dans le thread swing */
public class TaskManager implements ITaskParent {
	public static final Logger logger = Logger.getLogger(TaskManager.class);
	public static interface TaskManagerListener {
		void childAdded(BaseTask bt, BaseTask justAfter);
		void childRemoved(BaseTask bt);
		
		/** movedAfter was just move after "ref". ref null means first */
		void childMoved(BaseTask ref, BaseTask movedAfter);
	}

	public final WeakListenerCollection<TaskManagerListener> statusListeners = new WeakListenerCollection<>(TaskManagerListener.class);
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	final List<BaseTask> runningTasks;
	BaseTask first, last;
	
	public TaskManager() {
		runningTasks = new ArrayList<>();
	}

	
	public BaseTask startTask(FocusUi focusUi, BaseTaskDefinition taskDef, ITaskParameterView parameters) {
		BaseTask task = taskDef.build(focusUi, this, null);
		
		task.statusListeners.addListener(this.listenerOwner, new TaskStatusListener() {
			
			@Override
			public void statusChanged() {
				startSomeTask();
			}
		});
		task.setParameters(parameters);
		BaseTask previous = task.previous;;
		runningTasks.add(task);
		
		checkList();
		statusListeners.getTarget().childAdded(task, previous);
		
		if (startSomeTask() == task) {
			focusUi.selectTask(task);
		}
		
		return task;
	}

	BaseTask startSomeTask() {
		BaseTask result = null;
		BaseTask candidate;
		// Parcourir les taches
		do {
			candidate = null;
			for(BaseTask bt = first; bt != null; bt = bt.next) {
				if (bt.getStatus().isTerminal()) {
					continue;
				}
				if (bt.getStatus() == BaseStatus.Pending) {
					if (candidate == null) {
						candidate = bt;
					}
					continue;
				}
				if (bt.getStatus() == BaseStatus.Pending) {
					continue;
				}
				// Quelque chose bloque
				return null;
			}
			
			if (candidate != null) {
				if (result == null) {
					result = candidate;
				}
				try {
					candidate.start();
				} catch(Throwable t) {
					candidate.logger.error("Error", t);
				}
			}
		} while(candidate != null);
		return result;
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
				task.statusListeners.removeListener(this.listenerOwner);
				startSomeTask();
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


	/** Move next so it is just after previous. Notify the change */
	public void moveTaskAfter(BaseTask previous, BaseTask next) {
		if (next.getParent() == null) {
			return;
		}
		
		if (next.previous == previous) {
			return;
		}
		if (previous == next) {
			return;
		}
		
		next.dettach();
		next.parent = this;
		next.previous = previous;
		if (previous != null) {
			next.next = previous.next;
			
			if (next.next == null) {
				last = next;
			} else {
				next.next.previous = next;
			}
			previous.next = next;
		} else {
			// Insert as first
			next.next = first;
			if (next.next != null) {
				next.next.previous = next;
			} else {
				last = next;
			}
			first = next;
		}
		checkList();
		
		statusListeners.getTarget().childMoved(previous, next);
	}
	
	void checkList()
	{
		if (first != null) {
			checkList(first);
		} else {
			if (last != null) {
				throw new RuntimeException("last/first mismatch");
			}
		}
	}
	
	private void checkList(BaseTask item)
	{
		if (item.previous == null) {
			if (first != item) {
				throw new RuntimeException("not first");
			}
		} else {
			if (item.previous.next != item) {
				throw new RuntimeException("invalid chain");
			}
		}
		if (item.next == null) {
			if (last != item) {
				throw new RuntimeException("not last");
			}
		} else {
			if (item.next.previous != item) {
				throw new RuntimeException("invalid chain");
			}
		}
	}
}
