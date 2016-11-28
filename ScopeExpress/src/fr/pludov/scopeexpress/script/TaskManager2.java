package fr.pludov.scopeexpress.script;

import java.util.*;

import fr.pludov.scopeexpress.utils.*;

public class TaskManager2 {
	public final WeakListenerCollection<TaskManagerListener> statusListeners = new WeakListenerCollection<>(TaskManagerListener.class);

	
	public static interface TaskManagerListener {
		void childAdded(TaskOrGroup bt, TaskOrGroup justAfter);
		void childRemoved(TaskOrGroup bt);
		
		/** movedAfter was just move after "ref". ref null means first */
		void childMoved(TaskOrGroup ref, TaskOrGroup movedAfter);
	}
	
	final List<TaskGroup> activeGroups;
	
	public TaskManager2() {
		this.activeGroups = new ArrayList<>();
	}

	public List<TaskGroup> getGroups() {
		return Collections.unmodifiableList(activeGroups);
	}

	public void add(TaskGroup tg) {
		TaskOrGroup previous = activeGroups.isEmpty() ? null : activeGroups.get(activeGroups.size() - 1);
		this.activeGroups.add(tg);
		statusListeners.getTarget().childAdded(tg, previous);
	}

	public void removeTask(TaskGroup tg)
	{
		if (this.activeGroups.remove(tg)) {
			statusListeners.getTarget().childRemoved(tg);
		}
		
	}

	public void moveTaskAfter(TaskOrGroup previous, TaskOrGroup next) {
		if (!(previous instanceof TaskGroup)) return;
		if (!(next instanceof TaskGroup)) return;
		if (previous == next) return;

		int idToRemove = activeGroups.indexOf(next);
		if (idToRemove == -1) {
			return;
		}
		
		int idToAdd = activeGroups.indexOf(previous);
		if (idToAdd == -1) {
			return;
		}
		
		if (idToAdd < idToRemove) {
			idToAdd++;
		}
		activeGroups.remove(idToRemove);
		activeGroups.add(idToAdd, (TaskGroup)next);
		
		statusListeners.getTarget().childMoved(previous, next);
	}
// public void moveTaskAfter(BaseTask previous, BaseTask next) {
//	if (next.getParent() == null) {
//		return;
//	}
//	
//	if (next.previous == previous) {
//		return;
//	}
//	if (previous == next) {
//		return;
//	}
//	
//	next.dettach();
//	next.parent = this;
//	next.previous = previous;
//	if (previous != null) {
//		next.next = previous.next;
//		
//		if (next.next == null) {
//			last = next;
//		} else {
//			next.next.previous = next;
//		}
//		previous.next = next;
//	} else {
//		// Insert as first
//		next.next = first;
//		if (next.next != null) {
//			next.next.previous = next;
//		} else {
//			last = next;
//		}
//		first = next;
//	}
//	checkList();
//	
//	statusListeners.getTarget().childMoved(previous, next);
//}


}
